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
import org.apache.http.impl.client.SystemDefaultHttpClient;
import org.apache.http.impl.client.cache.CachingHttpClient;
import org.junit.After;
import org.junit.Test;

public class JarCacheTest {

    @Test
    public void cacheHit() throws Exception {
        final JarCacheStorage storage = new JarCacheStorage();
        final HttpClient httpClient = new CachingHttpClient(new SystemDefaultHttpClient(), storage,
                storage.getCacheConfig());
        final HttpGet get = new HttpGet("http://nonexisting.example.com/context");
        final HttpResponse resp = httpClient.execute(get);

        assertEquals("application/ld+json", resp.getEntity().getContentType().getValue());
        final String str = IOUtils.toString(resp.getEntity().getContent(), "UTF-8");
        assertTrue(str.contains("ex:datatype"));
    }

    @Test(expected = IOException.class)
    public void cacheMiss() throws Exception {
        final JarCacheStorage storage = new JarCacheStorage();
        final HttpClient httpClient = new CachingHttpClient(new SystemDefaultHttpClient(), storage,
                storage.getCacheConfig());
        final HttpGet get = new HttpGet("http://nonexisting.example.com/notfound");
        // Should throw an IOException as the DNS name
        // nonexisting.example.com does not exist
        final HttpResponse resp = httpClient.execute(get);
    }

    @Test
    public void doubleLoad() throws Exception {
        final JarCacheStorage storage = new JarCacheStorage();
        final HttpClient httpClient = new CachingHttpClient(new SystemDefaultHttpClient(), storage,
                storage.getCacheConfig());
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
        final JarCacheStorage storage = new JarCacheStorage(cl);

        final HttpClient httpClient = new CachingHttpClient(new SystemDefaultHttpClient(), storage,
                storage.getCacheConfig());
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

        final JarCacheStorage storage = new JarCacheStorage();
        Thread.currentThread().setContextClassLoader(cl);

        final HttpClient httpClient = new CachingHttpClient(new SystemDefaultHttpClient(), storage,
                storage.getCacheConfig());
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
        final JarCacheStorage storage = new JarCacheStorage(null);

        final HttpClient httpClient = new CachingHttpClient(new SystemDefaultHttpClient(), storage,
                storage.getCacheConfig());
        final HttpGet get = new HttpGet("http://nonexisting.example.com/context");
        final HttpResponse resp = httpClient.execute(get);
        assertEquals("application/ld+json", resp.getEntity().getContentType().getValue());
    }

}
