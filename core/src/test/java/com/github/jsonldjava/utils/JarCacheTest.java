package com.github.jsonldjava.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.RequestAcceptEncoding;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.junit.After;
import org.junit.Test;

public class JarCacheTest {

    @Test
    public void cacheHit() throws Exception {
        final CacheConfig cacheConfig = CacheConfig.custom().setMaxCacheEntries(1000)
                .setMaxObjectSize(1024 * 128).build();
        final JarCacheStorage storage = new JarCacheStorage(null, cacheConfig);
        final HttpClient httpClient = createTestHttpClient(cacheConfig, storage);
        final HttpGet get = new HttpGet("http://nonexisting.example.com/context");
        final HttpResponse resp = httpClient.execute(get);

        assertEquals("application/ld+json", resp.getEntity().getContentType().getValue());
        final String str = IOUtils.toString(resp.getEntity().getContent(), "UTF-8");
        assertTrue(str.contains("ex:datatype"));
    }

    @Test(expected = IOException.class)
    public void cacheMiss() throws Exception {
        final CacheConfig cacheConfig = CacheConfig.custom().setMaxCacheEntries(1000)
                .setMaxObjectSize(1024 * 128).build();
        final JarCacheStorage storage = new JarCacheStorage(null, cacheConfig);
        final HttpClient httpClient = createTestHttpClient(cacheConfig, storage);
        final HttpGet get = new HttpGet("http://nonexisting.example.com/notfound");
        // Should throw an IOException as the DNS name
        // nonexisting.example.com does not exist
        httpClient.execute(get);
    }

    @Test
    public void doubleLoad() throws Exception {
        final CacheConfig cacheConfig = CacheConfig.custom().setMaxCacheEntries(1000)
                .setMaxObjectSize(1024 * 128).build();
        final JarCacheStorage storage = new JarCacheStorage(null, cacheConfig);
        final HttpClient httpClient = createTestHttpClient(cacheConfig, storage);
        final HttpGet get = new HttpGet("http://nonexisting.example.com/context");
        HttpResponse resp = httpClient.execute(get);
        resp = httpClient.execute(get);
        // Ensure second load through the cached jarcache list works
        assertEquals("application/ld+json", resp.getEntity().getContentType().getValue());
    }

    @Test
    public void customClassPath() throws Exception {
        final URL nestedJar = getClass().getResource("/nested.jar");
        final ClassLoader cl = new URLClassLoader(new URL[] { nestedJar });
        final CacheConfig cacheConfig = CacheConfig.custom().setMaxCacheEntries(1000)
                .setMaxObjectSize(1024 * 128).build();
        final JarCacheStorage storage = new JarCacheStorage(cl, cacheConfig);
        final HttpClient httpClient = createTestHttpClient(cacheConfig, storage);
        final HttpGet get = new HttpGet("http://nonexisting.example.com/nested/hello");
        final HttpResponse resp = httpClient.execute(get);

        assertEquals("application/json", resp.getEntity().getContentType().getValue());
        final String str = IOUtils.toString(resp.getEntity().getContent(), "UTF-8");
        assertEquals("{ \"Hello\": \"World!\" }", str.trim());
    }

    @Test
    public void contextClassLoader() throws Exception {
        final URL nestedJar = getClass().getResource("/nested.jar");
        assertNotNull(nestedJar);
        final ClassLoader cl = new URLClassLoader(new URL[] { nestedJar });

        final CacheConfig cacheConfig = CacheConfig.custom().setMaxCacheEntries(1000)
                .setMaxObjectSize(1024 * 128).build();
        final JarCacheStorage storage = new JarCacheStorage(cl, cacheConfig);
        Thread.currentThread().setContextClassLoader(cl);

        final HttpClient httpClient = createTestHttpClient(cacheConfig, storage);
        final HttpGet get = new HttpGet("http://nonexisting.example.com/nested/hello");
        final HttpResponse resp = httpClient.execute(get);

        assertEquals("application/json", resp.getEntity().getContentType().getValue());
        final String str = IOUtils.toString(resp.getEntity().getContent(), "UTF-8");
        assertEquals("{ \"Hello\": \"World!\" }", str.trim());
    }

    @After
    public void setContextClassLoader() {
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
    }

    @Test
    public void systemClassLoader() throws Exception {
        final URL nestedJar = getClass().getResource("/nested.jar");
        assertNotNull(nestedJar);
        final CacheConfig cacheConfig = CacheConfig.custom().setMaxCacheEntries(1000)
                .setMaxObjectSize(1024 * 128).build();
        final JarCacheStorage storage = new JarCacheStorage(null, cacheConfig);

        final HttpClient httpClient = createTestHttpClient(cacheConfig, storage);
        final HttpGet get = new HttpGet("http://nonexisting.example.com/context");
        final HttpResponse resp = httpClient.execute(get);
        assertEquals("application/ld+json", resp.getEntity().getContentType().getValue());
    }

    private static CloseableHttpClient createTestHttpClient(CacheConfig cacheConfig,
            JarCacheStorage jarCacheConfig) {
        final CloseableHttpClient result = CachingHttpClientBuilder.create()
                // allow caching
                .setCacheConfig(cacheConfig)
                // Set the JarCacheStorage instance as the HttpCache
                .setHttpCacheStorage(jarCacheConfig)
                // Support compressed data
                // http://hc.apache.org/httpcomponents-client-ga/tutorial/html/httpagent.html#d5e1238
                .addInterceptorFirst(new RequestAcceptEncoding())
                .addInterceptorFirst(new ResponseContentEncoding())
                .setRedirectStrategy(DefaultRedirectStrategy.INSTANCE)
                // use system defaults for proxy etc.
                .useSystemProperties().build();

        return result;
    }
}
