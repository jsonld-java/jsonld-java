package com.github.jsonldjava.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.jsonldjava.core.DocumentLoader;
import com.github.jsonldjava.core.JsonLdApi;
import com.github.jsonldjava.core.JsonLdConsts;
import com.github.jsonldjava.core.JsonLdProcessor;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.RequestAcceptEncoding;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.cache.BasicHttpCacheStorage;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;

/**
 * Functions used to make loading, parsing, and serializing JSON easy using
 * Jackson.
 *
 * @author tristan
 *
 */
public class JsonUtils {
    /**
     * An HTTP Accept header that prefers JSONLD.
     */
    public static final String ACCEPT_HEADER = "application/ld+json, application/json;q=0.9, application/javascript;q=0.5, text/javascript;q=0.5, text/plain;q=0.2, */*;q=0.1";

    /**
     * The user agent used by the default {@link CloseableHttpClient}.
     *
     * This will not be used if
     * {@link DocumentLoader#setHttpClient(CloseableHttpClient)} is called with
     * a custom client.
     */
    public static final String JSONLD_JAVA_USER_AGENT = "JSONLD-Java";

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final JsonFactory JSON_FACTORY = new JsonFactory(JSON_MAPPER);
    private static final ObjectMapper JCS_MAPPER = JsonMapper.builder().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true).build();
    private static final SimpleModule module = new SimpleModule();
    private static final JsonFactory jcs_FACTORY = new JsonFactory(JCS_MAPPER);

    private static final JsonSerializer<Double> doubleSerializer = new JsonSerializer<Double>() {

        private DecimalFormat bigDecimalFor(String exponentSeparator) {
            DecimalFormatSymbols symbol = DecimalFormatSymbols.getInstance(Locale.US);
            symbol.setExponentSeparator(exponentSeparator);
            return new DecimalFormat("0E00", symbol);
        }

        private final DecimalFormat DOUBLE_JCS = new DecimalFormat("0.#######",
                DecimalFormatSymbols.getInstance(Locale.US));
        private final DecimalFormat BIG_POSITIVE_DOUBLE_JCS = bigDecimalFor("e+");
        private final DecimalFormat BIG_NEGATIVE_DOUBLE_JCS = bigDecimalFor("e");

        @Override
        public void serialize(Double value, JsonGenerator jgen, SerializerProvider serializerProvider) throws IOException {
            if(value >= 1.0E21)
                jgen.writeNumber(BIG_POSITIVE_DOUBLE_JCS.format(value).toLowerCase());
            else if (value != 0 && value <= 1.0E-21)
                jgen.writeNumber(BIG_NEGATIVE_DOUBLE_JCS.format(value).toLowerCase());
            else
                jgen.writeNumber(DOUBLE_JCS.format(value));

        }
    };

    private static volatile CloseableHttpClient DEFAULT_HTTP_CLIENT;
    // Avoid possible endless loop when following alternate locations
    private static final int MAX_LINKS_FOLLOW = 20;

    static {
        // Disable default Jackson behaviour to close
        // InputStreams/Readers/OutputStreams/Writers
        JSON_FACTORY.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        // Disable string retention features that may work for most JSON where
        // the field names are in limited supply, but does not work for JSON-LD
        // where a wide range of URIs are used for subjects and predicates
        JSON_FACTORY.disable(JsonFactory.Feature.INTERN_FIELD_NAMES);
        JSON_FACTORY.disable(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES);
        //adding our custom serializer
        module.addSerializer(Double.class ,doubleSerializer);
        //registering the module with ObjectMapper
        JCS_MAPPER.registerModule(module);
    }

    /**
     * Parses a JSON-LD document from the given {@link InputStream} to an object
     * that can be used as input for the {@link JsonLdApi} and
     * {@link JsonLdProcessor} methods.<br>
     * Uses UTF-8 as the character encoding when decoding the InputStream.
     *
     * @param input
     *            The JSON-LD document in an InputStream.
     * @return A JSON Object.
     * @throws JsonParseException
     *             If there was a JSON related error during parsing.
     * @throws IOException
     *             If there was an IO error during parsing.
     */
    public static Object fromInputStream(InputStream input) throws IOException {
        // filter BOMs from InputStream
        try (final BOMInputStream bOMInputStream = new BOMInputStream(input, false,
                ByteOrderMark.UTF_8, ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_16LE,
                ByteOrderMark.UTF_32BE, ByteOrderMark.UTF_32LE);) {
            Charset charset = StandardCharsets.UTF_8;
            // Attempt to use the BOM if it exists
            if (bOMInputStream.hasBOM()) {
                try {
                    charset = Charset.forName(bOMInputStream.getBOMCharsetName());
                } catch (final IllegalArgumentException e) {
                    // If there are any issues with the BOM charset, attempt to
                    // parse with UTF_8
                    charset = StandardCharsets.UTF_8;
                }
            }
            return fromInputStream(bOMInputStream, charset);
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }

    /**
     * Parses a JSON-LD document from the given {@link InputStream} to an object
     * that can be used as input for the {@link JsonLdApi} and
     * {@link JsonLdProcessor} methods.
     *
     * @param input
     *            The JSON-LD document in an InputStream.
     * @param enc
     *            The character encoding to use when interpreting the characters
     *            in the InputStream.
     * @return A JSON Object.
     * @throws JsonParseException
     *             If there was a JSON related error during parsing.
     * @throws IOException
     *             If there was an IO error during parsing.
     */
    public static Object fromInputStream(InputStream input, String enc) throws IOException {
        return fromInputStream(input, Charset.forName(enc));
    }

    /**
     * Parses a JSON-LD document from the given {@link InputStream} to an object
     * that can be used as input for the {@link JsonLdApi} and
     * {@link JsonLdProcessor} methods.
     *
     * @param input
     *            The JSON-LD document in an InputStream.
     * @param enc
     *            The character encoding to use when interpreting the characters
     *            in the InputStream.
     * @return A JSON Object.
     * @throws JsonParseException
     *             If there was a JSON related error during parsing.
     * @throws IOException
     *             If there was an IO error during parsing.
     */
    public static Object fromInputStream(InputStream input, Charset enc) throws IOException {
        try (InputStreamReader in = new InputStreamReader(input, enc);
                BufferedReader reader = new BufferedReader(in);) {
            return fromReader(reader);
        }
    }

    /**
     * Parses a JSON-LD document from the given {@link Reader} to an object that
     * can be used as input for the {@link JsonLdApi} and
     * {@link JsonLdProcessor} methods.
     *
     * @param reader
     *            The JSON-LD document in a Reader.
     * @return A JSON Object.
     * @throws JsonParseException
     *             If there was a JSON related error during parsing.
     * @throws IOException
     *             If there was an IO error during parsing.
     */
    public static Object fromReader(Reader reader) throws IOException {
        final JsonParser jp = JSON_FACTORY.createParser(reader);
        return fromJsonParser(jp);
    }

    /**
     * Parses a JSON-LD document from the given {@link JsonParser} to an object
     * that can be used as input for the {@link JsonLdApi} and
     * {@link JsonLdProcessor} methods.
     *
     * @param jp
     *            The JSON-LD document in a {@link JsonParser}.
     * @return A JSON Object.
     * @throws JsonParseException
     *             If there was a JSON related error during parsing.
     * @throws IOException
     *             If there was an IO error during parsing.
     */
    public static Object fromJsonParser(JsonParser jp) throws IOException {
        Object rval;
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
            throw new JsonParseException(jp,
                    "document doesn't start with a valid json element : " + initialToken,
                    jp.getCurrentLocation());
        }

        JsonToken t;
        try {
            t = jp.nextToken();
        } catch (final JsonParseException ex) {
            throw new JsonParseException(jp,
                    "Document contains more content after json-ld element - (possible mismatched {}?)",
                    jp.getCurrentLocation());
        }
        if (t != null) {
            throw new JsonParseException(jp,
                    "Document contains possible json content after the json-ld element - (possible mismatched {}?)",
                    jp.getCurrentLocation());
        }
        return rval;
    }

    /**
     * Parses a JSON-LD document from a string to an object that can be used as
     * input for the {@link JsonLdApi} and {@link JsonLdProcessor} methods.
     *
     * @param jsonString
     *            The JSON-LD document as a string.
     * @return A JSON Object.
     * @throws JsonParseException
     *             If there was a JSON related error during parsing.
     * @throws IOException
     *             If there was an IO error during parsing.
     */
    public static Object fromString(String jsonString) throws JsonParseException, IOException {
        return fromReader(new StringReader(jsonString));
    }

    /**
     * Writes the given JSON-LD Object out to a String, using indentation and
     * new lines to improve readability.
     *
     * @param jsonObject
     *            The JSON-LD Object to serialize.
     * @return A JSON document serialised to a String.
     * @throws IOException
     *             If there is an IO error during serialization.
     */
    public static String toPrettyString(Object jsonObject) throws IOException {
        final StringWriter sw = new StringWriter();
        writePrettyPrint(sw, jsonObject);
        return sw.toString();
    }

    /**
     * Writes the given JSON-LD Object out to a String.
     *
     * @param jsonObject
     *            The JSON-LD Object to serialize.
     * @return A JSON document serialised to a String.
     * @throws IOException
     *             If there is an IO error during serialization.
     */
    public static String toString(Object jsonObject) throws IOException {
        final StringWriter sw = new StringWriter();
        write(sw, jsonObject);
        return sw.toString();
    }

    /**
     * Writes the given JSON-LD Object out to a String as JSON Canonicalization Scheme (JCS).
     *
     * @param jsonObject
     *            The JSON-LD Object to serialize.
     * @return A JSON document serialised to a String.
     * @throws IOException
     *             If there is an IO error during serialization.
     */
    public static String toJcsString(Object jsonObject)  throws IOException {
        if(jsonObject == null) {
            return "null";
        }

        final StringWriter sw = new StringWriter();

        final JsonGenerator jw = jcs_FACTORY.createGenerator(sw);
        jw.writeObject(jsonObject);
        return sw.toString();
    }

    /**
     * Writes the given JSON-LD Object out to the given Writer.
     *
     * @param writer
     *            The writer that is to receive the serialized JSON-LD object.
     * @param jsonObject
     *            The JSON-LD Object to serialize.
     * @throws IOException
     *             If there is an IO error during serialization.
     */
    public static void write(Writer writer, Object jsonObject) throws IOException {
        final JsonGenerator jw = JSON_FACTORY.createGenerator(writer);
        jw.writeObject(jsonObject);
    }

    /**
     * Writes the given JSON-LD Object out to the given Writer, using
     * indentation and new lines to improve readability.
     *
     * @param writer
     *            The writer that is to receive the serialized JSON-LD object.
     * @param jsonObject
     *            The JSON-LD Object to serialize.
     * @throws IOException
     *             If there is an IO error during serialization.
     */
    public static void writePrettyPrint(Writer writer, Object jsonObject)
            throws IOException {
        final JsonGenerator jw = JSON_FACTORY.createGenerator(writer);
        jw.useDefaultPrettyPrinter();
        jw.writeObject(jsonObject);
    }

    /**
     * Parses a JSON-LD document, from the contents of the JSON resource
     * resolved from the JsonLdUrl, to an object that can be used as input for
     * the {@link JsonLdApi} and {@link JsonLdProcessor} methods.
     *
     * @param url
     *            The JsonLdUrl to resolve
     * @param httpClient
     *            The {@link CloseableHttpClient} to use to resolve the URL.
     * @return A JSON Object.
     * @throws JsonParseException
     *             If there was a JSON related error during parsing.
     * @throws IOException
     *             If there was an IO error during parsing.
     */
    public static Object fromURL(java.net.URL url, CloseableHttpClient httpClient)
            throws JsonParseException, IOException {
        final String protocol = url.getProtocol();
        // We can only use the Apache HTTPClient for HTTP/HTTPS, so use the
        // native java client for the others
        if (!protocol.equalsIgnoreCase("http") && !protocol.equalsIgnoreCase("https")) {
            // Can't use the HTTP client for those!
            // Fallback to Java's built-in JsonLdUrl handler. No need for
            // Accept headers as it's likely to be file: or jar:
            return fromInputStream(url.openStream());
        } else {
            return fromJsonLdViaHttpUri(url, httpClient, 0);
        }
    }

    private static Object fromJsonLdViaHttpUri(final URL url, final CloseableHttpClient httpClient, int linksFollowed)
            throws IOException {
        final HttpUriRequest request = new HttpGet(url.toExternalForm());
        // We prefer application/ld+json, but fallback to application/json
        // or whatever is available
        request.addHeader("Accept", ACCEPT_HEADER);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            final int status = response.getStatusLine().getStatusCode();
            if (status != 200 && status != 203) {
                throw new IOException("Can't retrieve " + url + ", status code: " + status);
            }
            // follow alternate document location
            // https://www.w3.org/TR/json-ld11/#alternate-document-location
            URL alternateLink = alternateLink(url, response);
            if (alternateLink != null) {
                linksFollowed++;
                if (linksFollowed > MAX_LINKS_FOLLOW) {
                    throw new IOException("Too many alternate links followed. This may indicate a cycle. Aborting.");
                }
                return fromJsonLdViaHttpUri(alternateLink, httpClient, linksFollowed);
            }
            return fromInputStream(response.getEntity().getContent());
        }
    }

    private static URL alternateLink(URL url, CloseableHttpResponse response)
            throws MalformedURLException {
        if (response.getEntity().getContentType() != null
                && !response.getEntity().getContentType().getValue().equals("application/ld+json")) {
            for (Header header : response.getAllHeaders()) {
                if (header.getName().equalsIgnoreCase("link")) {
                    String alternateLink = "";
                    boolean relAlternate = false;
                    boolean jsonld = false;
                    for (String value : header.getValue().split(";")) {
                        value=value.trim();
                        if (value.startsWith("<") && value.endsWith(">")) {
                            alternateLink = value.substring(1, value.length() - 1);
                        }
                        if (value.startsWith("type=\"application/ld+json\"")) {
                            jsonld = true;
                        }
                        if (value.startsWith("rel=\"alternate\"")) {
                            relAlternate = true;
                        }
                    }
                    if (jsonld && relAlternate && !alternateLink.isEmpty()) {
                        return new URL(url.getProtocol() + "://" + url.getAuthority() + alternateLink);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Fallback method directly using the {@link java.net.HttpURLConnection}
     * class for cases where servers do not interoperate correctly with Apache
     * HTTPClient.
     *
     * @param url
     *            The URL to access.
     * @return The result, after conversion from JSON to a Java Object.
     * @throws JsonParseException
     *             If there was a JSON related error during parsing.
     * @throws IOException
     *             If there was an IO error during parsing.
     */
    public static Object fromURLJavaNet(URL url) throws JsonParseException, IOException {
        final HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
        urlConn.addRequestProperty("Accept", ACCEPT_HEADER);

        final StringWriter output = new StringWriter();
        try (final InputStream directStream = urlConn.getInputStream();) {
            IOUtils.copy(directStream, output, StandardCharsets.UTF_8);
        } finally {
            output.flush();
        }
        final Object context = JsonUtils.fromReader(new StringReader(output.toString()));
        return context;
    }

    public static CloseableHttpClient getDefaultHttpClient() {
        CloseableHttpClient result = DEFAULT_HTTP_CLIENT;
        if (result == null) {
            synchronized (JsonUtils.class) {
                result = DEFAULT_HTTP_CLIENT;
                if (result == null) {
                    result = DEFAULT_HTTP_CLIENT = JsonUtils.createDefaultHttpClient();
                }
            }
        }
        return result;
    }

    public static CloseableHttpClient createDefaultHttpClient() {
        final CacheConfig cacheConfig = createDefaultCacheConfig();

        final CloseableHttpClient result = createDefaultHttpClient(cacheConfig);

        return result;
    }

    public static CacheConfig createDefaultCacheConfig() {
        return CacheConfig.custom().setMaxCacheEntries(500).setMaxObjectSize(1024 * 256)
                .setSharedCache(false).setHeuristicCachingEnabled(true)
                .setHeuristicDefaultLifetime(86400).build();
    }

    public static CloseableHttpClient createDefaultHttpClient(final CacheConfig cacheConfig) {
        return createDefaultHttpClientBuilder(cacheConfig).build();
    }

    public static HttpClientBuilder createDefaultHttpClientBuilder(final CacheConfig cacheConfig) {
        // Common CacheConfig for both the JarCacheStorage and the underlying
        // BasicHttpCacheStorage
        return CachingHttpClientBuilder.create()
                // allow caching
                .setCacheConfig(cacheConfig)
                // Wrap the local JarCacheStorage around a BasicHttpCacheStorage
                .setHttpCacheStorage(new JarCacheStorage(null, cacheConfig,
                        new BasicHttpCacheStorage(cacheConfig)))
                // Support compressed data
                // https://wayback.archive.org/web/20130901115452/http://hc.apache.org:80/httpcomponents-client-ga/tutorial/html/httpagent.html#d5e1238
                .addInterceptorFirst(new RequestAcceptEncoding())
                .addInterceptorFirst(new ResponseContentEncoding())
                .setRedirectStrategy(DefaultRedirectStrategy.INSTANCE)
                // User agent customisation
                .setUserAgent(JSONLD_JAVA_USER_AGENT)
                // use system defaults for proxy etc.
                .useSystemProperties();
    }

    private JsonUtils() {
        // Static class, no access to constructor
    }
}
