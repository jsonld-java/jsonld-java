package com.github.jsonldjava.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.cache.CacheResponseStatus;
import org.apache.http.client.cache.HttpCacheContext;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.SystemDefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

@SuppressWarnings("unchecked")
public class DocumentLoaderTest {

    DocumentLoader documentLoader = new DocumentLoader();

    @Test
    public void fromURLTest0001() throws Exception {
        final URL contexttest = getClass().getResource("/custom/contexttest-0001.jsonld");
        assertNotNull(contexttest);
        final Object context = documentLoader.fromURL(contexttest);
        assertTrue(context instanceof Map);
        final Map<String, Object> contextMap = (Map<String, Object>) context;
        assertEquals(1, contextMap.size());
        final Map<String, Object> cont = (Map<String, Object>) contextMap.get("@context");
        assertEquals(3, cont.size());
        assertEquals("http://example.org/", cont.get("ex"));
        final Map<String, Object> term1 = (Map<String, Object>) cont.get("term1");
        assertEquals("ex:term1", term1.get("@id"));
    }

    @Test
    public void fromURLTest0002() throws Exception {
        final URL contexttest = getClass().getResource("/custom/contexttest-0002.jsonld");
        assertNotNull(contexttest);
        final Object context = documentLoader.fromURL(contexttest);
        assertTrue(context instanceof List);
        final List<Map<String, Object>> contextList = (List<Map<String, Object>>) context;

        final Map<String, Object> contextMap1 = contextList.get(0);
        assertEquals(1, contextMap1.size());
        final Map<String, Object> cont1 = (Map<String, Object>) contextMap1.get("@context");
        assertEquals(2, cont1.size());
        assertEquals("http://example.org/", cont1.get("ex"));
        final Map<String, Object> term1 = (Map<String, Object>) cont1.get("term1");
        assertEquals("ex:term1", term1.get("@id"));

        final Map<String, Object> contextMap2 = contextList.get(1);
        assertEquals(1, contextMap2.size());
        final Map<String, Object> cont2 = (Map<String, Object>) contextMap2.get("@context");
        assertEquals(1, cont2.size());
        final Map<String, Object> term2 = (Map<String, Object>) cont2.get("term2");
        assertEquals("ex:term2", term2.get("@id"));
    }

    // @Ignore("Integration test")
    @Test
    public void fromURLredirectHTTPSToHTTP() throws Exception {
        final URL url = new URL("https://w3id.org/bundle/context");
        final Object context = documentLoader.fromURL(url);
        // Should not fail because of
        // http://stackoverflow.com/questions/1884230/java-doesnt-follow-redirect-in-urlconnection
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4620571
        assertTrue(context instanceof Map);
        assertFalse(((Map<?, ?>) context).isEmpty());
    }

    // @Ignore("Integration test")
    @Test
    public void fromURLredirect() throws Exception {
        final URL url = new URL("http://purl.org/wf4ever/ro-bundle/context.json");
        final Object context = documentLoader.fromURL(url);
        assertTrue(context instanceof Map);
        assertFalse(((Map<?, ?>) context).isEmpty());
    }

    @Test
    public void fromURLCache() throws Exception {
        final URL url = new URL("http://json-ld.org/contexts/person.jsonld");
        documentLoader.fromURL(url);

        // Now try to get it again and ensure it is
        // cached
        final HttpClient client = documentLoader.getHttpClient();
        final HttpUriRequest get = new HttpGet(url.toURI());
        get.setHeader("Accept", DocumentLoader.ACCEPT_HEADER);
        final HttpCacheContext localContext = HttpCacheContext.create();
        final HttpResponse respo = client.execute(get, localContext);
        EntityUtils.consume(respo.getEntity());

        // Check cache status
        // http://hc.apache.org/httpcomponents-client-ga/tutorial/html/caching.html
        final CacheResponseStatus responseStatus = localContext.getCacheResponseStatus();
        assertFalse(CacheResponseStatus.CACHE_MISS.equals(responseStatus));
    }

    @Test
    public void fromURLCustomHandler() throws Exception {
        final AtomicInteger requests = new AtomicInteger();
        final URLStreamHandler handler = new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u) throws IOException {
                return new URLConnection(u) {
                    @Override
                    public void connect() throws IOException {
                        return;
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        requests.incrementAndGet();
                        return getClass().getResourceAsStream("/custom/contexttest-0001.jsonld");
                    }
                };
            }
        };
        final URL url = new URL(null, "jsonldtest:context", handler);
        assertEquals(0, requests.get());
        final Object context = documentLoader.fromURL(url);
        assertEquals(1, requests.get());
        assertTrue(context instanceof Map);
        assertFalse(((Map<?, ?>) context).isEmpty());
    }

    protected CloseableHttpClient fakeHttpClient(ArgumentCaptor<HttpUriRequest> httpRequest)
            throws IllegalStateException, IOException {
        final CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        final CloseableHttpResponse fakeResponse = mock(CloseableHttpResponse.class);
        final StatusLine statusCode = mock(StatusLine.class);
        when(statusCode.getStatusCode()).thenReturn(200);
        when(fakeResponse.getStatusLine()).thenReturn(statusCode);
        final HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContent()).thenReturn(
                DocumentLoaderTest.class.getResourceAsStream("/custom/contexttest-0001.jsonld"));
        when(fakeResponse.getEntity()).thenReturn(entity);
        when(httpClient.execute(httpRequest.capture())).thenReturn(fakeResponse);
        return httpClient;
    }

    @Test
    public void fromURLAcceptHeaders() throws Exception {

        final URL url = new URL("http://example.com/fake-jsonld-test");
        final ArgumentCaptor<HttpUriRequest> httpRequest = ArgumentCaptor
                .forClass(HttpUriRequest.class);
        documentLoader.setHttpClient(fakeHttpClient(httpRequest));
        try {
            final Object context = documentLoader.fromURL(url);
            assertTrue(context instanceof Map);
        } finally {
            documentLoader.setHttpClient(null);
        }
        assertEquals(1, httpRequest.getAllValues().size());
        final HttpUriRequest req = httpRequest.getValue();
        assertEquals(url.toURI(), req.getURI());

        final Header[] accept = req.getHeaders("Accept");
        assertEquals(1, accept.length);
        assertEquals(DocumentLoader.ACCEPT_HEADER, accept[0].getValue());
        // Test that this header parses correctly
        final HeaderElement[] elems = accept[0].getElements();
        assertEquals("application/ld+json", elems[0].getName());
        assertEquals(0, elems[0].getParameterCount());

        assertEquals("application/json", elems[1].getName());
        assertEquals(1, elems[1].getParameterCount());
        assertEquals("0.9", elems[1].getParameterByName("q").getValue());

        assertEquals("application/javascript", elems[2].getName());
        assertEquals(1, elems[2].getParameterCount());
        assertEquals("0.5", elems[2].getParameterByName("q").getValue());

        assertEquals("text/javascript", elems[3].getName());
        assertEquals(1, elems[3].getParameterCount());
        assertEquals("0.5", elems[3].getParameterByName("q").getValue());

        assertEquals("text/plain", elems[4].getName());
        assertEquals(1, elems[4].getParameterCount());
        assertEquals("0.2", elems[4].getParameterByName("q").getValue());

        assertEquals("*/*", elems[5].getName());
        assertEquals(1, elems[5].getParameterCount());
        assertEquals("0.1", elems[5].getParameterByName("q").getValue());

        assertEquals(6, elems.length);
    }

    @Test
    public void jarCacheHit() throws Exception {
        // If no cache, should fail-fast as nonexisting.example.com is not in
        // DNS
        final Object context = documentLoader.fromURL(new URL(
                "http://nonexisting.example.com/context"));
        assertTrue(context instanceof Map);
        assertTrue(((Map) context).containsKey("@context"));
    }

    @Test(expected = IOException.class)
    public void jarCacheMiss404() throws Exception {
        // Should fail-fast as nonexisting.example.com is not in DNS
        final Object context = documentLoader
                .fromURL(new URL("http://nonexisting.example.com/miss"));
    }

    @After
    public void setContextClassLoader() {
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
    }

    @Test(expected = IOException.class)
    public void jarCacheMissThreadCtx() throws Exception {
        final URLClassLoader findNothingCL = new URLClassLoader(new URL[] {}, null);
        Thread.currentThread().setContextClassLoader(findNothingCL);
        final Object context = documentLoader.fromURL(new URL(
                "http://nonexisting.example.com/context"));
    }

    @Test
    public void jarCacheHitThreadCtx() throws Exception {
        final URL url = new URL("http://nonexisting.example.com/nested/hello");
        final URL nestedJar = getClass().getResource("/nested.jar");
        try {
            final Object hello = documentLoader.fromURL(url);
            fail("Should not be able to find nested/hello yet");
        } catch (final IOException ex) {
            // expected
        }

        final ClassLoader cl = new URLClassLoader(new URL[] { nestedJar });
        Thread.currentThread().setContextClassLoader(cl);
        final Object hello = documentLoader.fromURL(url);
        assertTrue(hello instanceof Map);
        assertEquals("World!", ((Map) hello).get("Hello"));
    }

    @Test
    public void sharedHttpClient() throws Exception {
        // Should be the same instance unless explicitly set
        assertSame(documentLoader.getHttpClient(), new DocumentLoader().getHttpClient());
    }

    @Test
    public void differentHttpClient() throws Exception {
        // Custom http client
        documentLoader.setHttpClient(new SystemDefaultHttpClient());
        assertNotSame(documentLoader.getHttpClient(), new DocumentLoader().getHttpClient());

        // Use default again
        documentLoader.setHttpClient(null);
        assertSame(documentLoader.getHttpClient(), new DocumentLoader().getHttpClient());
    }

    @Test
    public void testDisallowRemoteContexts() throws Exception {
        String testUrl = "http://json-ld.org/contexts/person.jsonld";
        Object test = documentLoader.loadDocument(testUrl);

        assertNotNull(
                "Was not able to fetch from URL before testing disallow remote contexts loading",
                test);

        String disallowProperty = System
                .getProperty(DocumentLoader.DISALLOW_REMOTE_CONTEXT_LOADING);
        try {
            System.setProperty(DocumentLoader.DISALLOW_REMOTE_CONTEXT_LOADING, "true");
            documentLoader.loadDocument(testUrl);
            fail("Expected exception to occur");
        } catch (JsonLdError e) {
            assertEquals(JsonLdError.Error.LOADING_REMOTE_CONTEXT_FAILED, e.getType());
        } finally {
            if (disallowProperty == null) {
                System.clearProperty(DocumentLoader.DISALLOW_REMOTE_CONTEXT_LOADING);
            } else {
                System.setProperty(DocumentLoader.DISALLOW_REMOTE_CONTEXT_LOADING, disallowProperty);
            }
        }
    }
}
