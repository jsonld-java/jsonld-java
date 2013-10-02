package com.github.jsonldjava.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.RequestAcceptEncoding;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.SystemDefaultHttpClient;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClient;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.github.jsonldjava.utils.JSONUtils;

public class DocumentLoader {

	public RemoteDocument loadDocument(String url) throws JsonLdError {
		// TODO: use fromURL to load document
		// TODO: get http link context
		return new RemoteDocument("", null);
		/*
		 } catch (Exception e) {
					// If context cannot be dereferenced
					throw new JsonLdError(Error.LOADING_REMOTE_CONTEXT_FAILED, (String)context);
				}
		 */
	}
	
    /**
     * An HTTP Accept header that prefers JSONLD.
     */
    public static final String ACCEPT_HEADER = "application/ld+json, application/json;q=0.9, application/javascript;q=0.5, text/javascript;q=0.5, text/plain;q=0.2, */*;q=0.1";
    private static volatile HttpClient httpClient;
	
    /**
     * Returns a Map, List, or String containing the contents of the JSON
     * resource resolved from the URL.
     * 
     * @param url
     *            The URL to resolve
     * @return The Map, List, or String that represent the JSON resource
     *         resolved from the URL
     * @throws JsonParseException
     *             If the JSON was not valid.
     * @throws IOException
     *             If there was an error resolving the resource.
     */
    public static Object fromURL(java.net.URL url) throws JsonParseException, IOException {

        final MappingJsonFactory jsonFactory = new MappingJsonFactory();
        final InputStream in = openStreamFromURL(url);
        try {
            final JsonParser parser = jsonFactory.createParser(in);
            final JsonToken token = parser.nextToken();
            Class<?> type;
            if (token == JsonToken.START_OBJECT) {
                type = Map.class;
            } else if (token == JsonToken.START_ARRAY) {
                type = List.class;
            } else {
                type = String.class;
            }
            try {
                return parser.readValueAs(type);
            } finally {
                parser.close();
            }
        } finally {
            in.close();
        }
    }

    /**
     * Opens an {@link InputStream} for the given {@link URL}, including support
     * for http and https URLs that are requested using Content Negotiation with
     * application/ld+json as the preferred content type.
     * 
     * @param url
     *            The URL identifying the source.
     * @return An InputStream containing the contents of the source.
     * @throws IOException
     *             If there was an error resolving the URL.
     */
    public static InputStream openStreamFromURL(java.net.URL url) throws IOException {
        final String protocol = url.getProtocol();
        if (!protocol.equalsIgnoreCase("http") && !protocol.equalsIgnoreCase("https")) {
            // Can't use the HTTP client for those!
            // Fallback to Java's built-in URL handler. No need for
            // Accept headers as it's likely to be file: or jar:
            return url.openStream();
        }
        final HttpUriRequest request = new HttpGet(url.toExternalForm());
        // We prefer application/ld+json, but fallback to application/json
        // or whatever is available
        request.addHeader("Accept", ACCEPT_HEADER);

        final HttpResponse response = getHttpClient().execute(request);
        final int status = response.getStatusLine().getStatusCode();
        if (status != 200 && status != 203) {
            throw new IOException("Can't retrieve " + url + ", status code: " + status);
        }
        return response.getEntity().getContent();
    }

    public static HttpClient getHttpClient() {
        if (httpClient == null) {
            synchronized (JSONUtils.class) {
                if (httpClient == null) {
                    // Uses Apache SystemDefaultHttpClient rather than
                    // DefaultHttpClient, thus the normal proxy settings for the
                    // JVM
                    // will be used

                    final DefaultHttpClient client = new SystemDefaultHttpClient();
                    // Support compressed data
                    // http://hc.apache.org/httpcomponents-client-ga/tutorial/html/httpagent.html#d5e1238
                    client.addRequestInterceptor(new RequestAcceptEncoding());
                    client.addResponseInterceptor(new ResponseContentEncoding());
                    final CacheConfig cacheConfig = new CacheConfig();
                    cacheConfig.setMaxObjectSize(1024 * 128); // 128 kB
                    cacheConfig.setMaxCacheEntries(1000);
                    // and allow caching
                    httpClient = new CachingHttpClient(client, cacheConfig);
                }
            }
        }
        return httpClient;
    }

    public static void setHttpClient(HttpClient nextHttpClient) {
        synchronized (JSONUtils.class) {
            httpClient = nextHttpClient;
        }
    }
}
