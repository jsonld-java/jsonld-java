package com.github.jsonldjava.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.RequestAcceptEncoding;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.github.jsonldjava.utils.JarCacheStorage;

public class DocumentLoader {

    /**
     * Identifies a system property that can be set to "true" in order to
     * disallow remote context loading.
     */
    public static final String DISALLOW_REMOTE_CONTEXT_LOADING = "com.github.jsonldjava.disallowRemoteContextLoading";

    public RemoteDocument loadDocument(String url) throws JsonLdError {
        String disallowRemote = System.getProperty(DocumentLoader.DISALLOW_REMOTE_CONTEXT_LOADING);

        if ("true".equalsIgnoreCase(disallowRemote)) {
            throw new JsonLdError(JsonLdError.Error.LOADING_REMOTE_CONTEXT_FAILED, url);
        }

        final RemoteDocument doc = new RemoteDocument(url, null);
        try {
            doc.setDocument(fromURL(new URL(url)));
        } catch (final Exception e) {
            throw new JsonLdError(JsonLdError.Error.LOADING_REMOTE_CONTEXT_FAILED, url);
        }
        return doc;
    }

    /**
     * An HTTP Accept header that prefers JSONLD.
     */
    public static final String ACCEPT_HEADER = "application/ld+json, application/json;q=0.9, application/javascript;q=0.5, text/javascript;q=0.5, text/plain;q=0.2, */*;q=0.1";

    protected static volatile CloseableHttpClient defaultHttpClient;
    private volatile CloseableHttpClient httpClient;

    /**
     * Returns a Map, List, or String containing the contents of the JSON
     * resource resolved from the JsonLdUrl.
     *
     * @param url
     *            The JsonLdUrl to resolve
     * @return The Map, List, or String that represent the JSON resource
     *         resolved from the JsonLdUrl
     * @throws JsonParseException
     *             If the JSON was not valid.
     * @throws IOException
     *             If there was an error resolving the resource.
     */
    public Object fromURL(java.net.URL url) throws JsonParseException, IOException {

        final MappingJsonFactory jsonFactory = new MappingJsonFactory();
        final InputStream in = openStreamFromURL(url);
        try {
            final JsonParser parser = jsonFactory.createParser(in);
            try {
                final JsonToken token = parser.nextToken();
                Class<?> type;
                if (token == JsonToken.START_OBJECT) {
                    type = Map.class;
                } else if (token == JsonToken.START_ARRAY) {
                    type = List.class;
                } else {
                    type = String.class;
                }
                return parser.readValueAs(type);
            } finally {
                parser.close();
            }
        } finally {
            in.close();
        }
    }

    /**
     * Opens an {@link InputStream} for the given {@link java.net.URL},
     * including support for http and https URLs that are requested using
     * Content Negotiation with application/ld+json as the preferred content
     * type.
     *
     * @param url
     *            The {@link java.net.URL} identifying the source.
     * @return An InputStream containing the contents of the source.
     * @throws IOException
     *             If there was an error resolving the {@link java.net.URL}.
     */
    public InputStream openStreamFromURL(java.net.URL url) throws IOException {
        final String protocol = url.getProtocol();
        if (!protocol.equalsIgnoreCase("http") && !protocol.equalsIgnoreCase("https")) {
            // Can't use the HTTP client for those!
            // Fallback to Java's built-in JsonLdUrl handler. No need for
            // Accept headers as it's likely to be file: or jar:
            return url.openStream();
        }
        final HttpUriRequest request = new HttpGet(url.toExternalForm());
        // We prefer application/ld+json, but fallback to application/json
        // or whatever is available
        request.addHeader("Accept", ACCEPT_HEADER);

        final CloseableHttpResponse response = getHttpClient().execute(request);
        try {
            final int status = response.getStatusLine().getStatusCode();
            if (status != 200 && status != 203) {
                throw new IOException("Can't retrieve " + url + ", status code: " + status);
            }
            return response.getEntity().getContent();
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    protected static CloseableHttpClient getDefaultHttpClient() {
        CloseableHttpClient result = defaultHttpClient;
        if (result == null) {
            synchronized (DocumentLoader.class) {
                result = defaultHttpClient;
                if (result == null) {
                    result = defaultHttpClient = createDefaultHttpClient();
                }
            }
        }
        return result;
    }

    protected static CloseableHttpClient createDefaultHttpClient() {
        return CachingHttpClientBuilder
                .create()
                // allow caching
                .setCacheConfig(
                        CacheConfig.custom().setMaxCacheEntries(1000).setMaxObjectSize(1024 * 128)
                                .build())
                // TODO: enable wrapping with JAR cache:
                .setHttpCacheStorage(new JarCacheStorage())
                // Support compressed data
                // http://hc.apache.org/httpcomponents-client-ga/tutorial/html/httpagent.html#d5e1238
                .addInterceptorFirst(new RequestAcceptEncoding())
                .addInterceptorFirst(new ResponseContentEncoding())
                // use system defaults for proxy etc.
                .useSystemProperties().build();
    }

    public CloseableHttpClient getHttpClient() {
        if (httpClient == null) {
            return getDefaultHttpClient();
        }
        return httpClient;
    }

    public void setHttpClient(CloseableHttpClient nextHttpClient) {
        httpClient = nextHttpClient;
    }
}
