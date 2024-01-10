package com.github.jsonldjava.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;

import com.github.jsonldjava.utils.JarCacheStorage;
import com.github.jsonldjava.utils.JsonUtils;

import org.apache.http.client.protocol.RequestAcceptEncoding;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.cache.BasicHttpCacheStorage;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.junit.jupiter.api.Test;

public class MinimalSchemaOrgRegressionTest {

    /**
     * Tests getting JSON from schema.org with the HTTP Accept header set to
     * {@value com.github.jsonldjava.utils.JsonUtils#ACCEPT_HEADER}? .
     */
    @Test
    public void testApacheHttpClient() throws Exception {
        final URL url = new URL("http://schema.org/");
        // Common CacheConfig for both the JarCacheStorage and the underlying
        // BasicHttpCacheStorage
        final CacheConfig cacheConfig = CacheConfig.custom().setMaxCacheEntries(1000)
        .setMaxObjectSize(1024 * 128).build();

        final CloseableHttpClient httpClient = CachingHttpClientBuilder.create()
        // allow caching
        .setCacheConfig(cacheConfig)
        // Wrap the local JarCacheStorage around a BasicHttpCacheStorage
        .setHttpCacheStorage(new JarCacheStorage(null, cacheConfig,
        new BasicHttpCacheStorage(cacheConfig)))
        // Support compressed data
        // http://hc.apache.org/httpcomponents-client-ga/tutorial/html/httpagent.html#d5e1238
        .addInterceptorFirst(new RequestAcceptEncoding())
        .addInterceptorFirst(new ResponseContentEncoding())
        .setRedirectStrategy(DefaultRedirectStrategy.INSTANCE)
        // use system defaults for proxy etc.
        .useSystemProperties().build();

        Object content = JsonUtils.fromURL(url, httpClient);
        checkBasicConditions(content.toString());
    }

    private void checkBasicConditions(final String outputString) {
        // Test for some basic conditions without including the JSON/JSON-LD
        // parsing code here
        assertTrue(outputString.endsWith("}"), outputString);
        assertFalse(outputString.isEmpty(),
                "Output string should not be empty: " + outputString.length());
        assertTrue(outputString.length() > 100000, "Unexpected length: " + outputString.length());
    }

}
