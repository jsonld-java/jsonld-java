package com.github.jsonldjava.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A bunch of functions to make loading JSON easy
 * 
 * @author tristan
 * 
 */
public class JSONUtils {
    /**
     * An HTTP Accept header that prefers JSONLD.
     */
    protected static final String ACCEPT_HEADER = "application/ld+json, application/json;q=0.9, application/javascript;q=0.5, text/javascript;q=0.5, text/plain;q=0.2, */*;q=0.1";
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final JsonFactory JSON_FACTORY = new JsonFactory(JSON_MAPPER);

    static {
        // Disable default Jackson behaviour to close
        // InputStreams/Readers/OutputStreams/Writers
        JSON_FACTORY.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        // Disable string retention features that may work for most JSON where
        // the field names are in limited supply, but does not work for JSON-LD
        // where a wide range of URIs are used for subjects and predicates
        JSON_FACTORY.disable(JsonFactory.Feature.INTERN_FIELD_NAMES);
        JSON_FACTORY.disable(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES);
    }

    private static volatile HttpClient httpClient;

    public static Object fromString(String jsonString) throws JsonParseException,
             IOException {
        return fromReader(new StringReader(jsonString));
    }

    public static Object fromReader(Reader r) throws IOException {
        final JsonParser jp = JSON_FACTORY.createParser(r);
        Object rval = null;
        final JsonToken initialToken = jp.nextToken();

        if (initialToken == JsonToken.START_ARRAY) {
            rval = jp.readValueAs(List.class);
        } else if (initialToken == JsonToken.START_OBJECT) {
            rval = jp.readValueAs(Map.class);
        } else if (initialToken == JsonToken.VALUE_STRING) {
            rval = jp.readValueAs(String.class);
        } else if (initialToken == JsonToken.VALUE_FALSE || initialToken == JsonToken.VALUE_TRUE) {
            rval = jp.readValueAs(Boolean.class);
        } else if (initialToken == JsonToken.VALUE_NUMBER_FLOAT
                || initialToken == JsonToken.VALUE_NUMBER_INT) {
            rval = jp.readValueAs(Number.class);
        } else if (initialToken == JsonToken.VALUE_NULL) {
            rval = null;
        } else {
            throw new JsonParseException("document doesn't start with a valid json element : "
                    + initialToken, jp.getCurrentLocation());
        }
        return rval;
    }

    public static void write(Writer w, Object jsonObject) throws JsonGenerationException,
             IOException {
        final JsonGenerator jw = JSON_FACTORY.createGenerator(w);
        jw.writeObject(jsonObject);
    }

    public static void writePrettyPrint(Writer w, Object jsonObject)
            throws JsonGenerationException,  IOException {
        final JsonGenerator jw = JSON_FACTORY.createGenerator(w);
        jw.useDefaultPrettyPrinter();
        jw.writeObject(jsonObject);
    }

    public static Object fromInputStream(InputStream content) throws IOException {
        return fromInputStream(content, "UTF-8"); // no readers from
                                                  // inputstreams w.o.
                                                  // encoding!!
    }

    public static Object fromInputStream(InputStream content, String enc) throws IOException {
        return fromReader(new BufferedReader(new InputStreamReader(content, enc)));
    }

    public static String toPrettyString(Object obj) {
        final StringWriter sw = new StringWriter();
        try {
            writePrettyPrint(sw, obj);
        } catch (final IOException e) {
            // TODO Is this really possible with stringwriter?
            // I think it's only there because of the interface
            // however, if so... well, we have to do something!
            // it seems weird for toString to throw an IOException
            e.printStackTrace();
        }
        return sw.toString();
    }

    public static String toString(Object obj) { // throws
                                                // JsonGenerationException,
                                                // JsonMappingException {
        final StringWriter sw = new StringWriter();
        try {
            write(sw, obj);
        } catch (final IOException e) {
            // TODO Is this really possible with stringwriter?
            // I think it's only there because of the interface
            // however, if so... well, we have to do something!
            // it seems weird for toString to throw an IOException
            e.printStackTrace();
        }
        return sw.toString();
    }

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

        InputStream in = null;
        try {
            in = openStreamFromURL(url);
            final JsonParser parser = JSON_FACTORY.createParser(in);
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
            if (in != null) {
                in.close();
            }
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

    protected static HttpClient getHttpClient() {
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

    protected static void setHttpClient(HttpClient nextHttpClient) {
        synchronized (JSONUtils.class) {
            httpClient = nextHttpClient;
        }
    }
}
