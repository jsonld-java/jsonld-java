package com.github.jsonldjava.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class JSONUtilsTest {

    @SuppressWarnings("unchecked")
    @Test
    public void fromStringTest() {
        final String testString = "{\"seq\":3,\"id\":\"e48dfa735d9fad88db6b7cd696002df7\",\"changes\":[{\"rev\":\"2-6aebf275bc3f29b67695c727d448df8e\"}]}";
        final String testFailure = "{{{{{{{{{{{";
        Object obj = null;

        try {
            obj = JSONUtils.fromString(testString);
        } catch (final Exception e) {
            assertTrue(false);
        }

        assertTrue(((Map<String, Object>) obj).containsKey("seq"));
        assertTrue(((Map<String, Object>) obj).get("seq") instanceof Number);

        try {
            obj = JSONUtils.fromString(testFailure);
            assertTrue(false);
        } catch (final Exception e) {
            assertTrue(true);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void fromURLTest0001() throws Exception {
        final URL contexttest = getClass().getResource("/custom/contexttest-0001.jsonld");
        assertNotNull(contexttest);
        final Object context = JSONUtils.fromURL(contexttest);
        assertTrue(context instanceof Map);
        final Map<String, Object> contextMap = (Map<String, Object>) context;
        assertEquals(1, contextMap.size());
        final Map<String, Object> cont = (Map<String, Object>) contextMap.get("@context");
        assertEquals(3, cont.size());
        assertEquals("http://example.org/", cont.get("ex"));
        final Map<String, Object> term1 = (Map<String, Object>) cont.get("term1");
        assertEquals("ex:term1", term1.get("@id"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void fromURLTest0002() throws Exception {
        final URL contexttest = getClass().getResource("/custom/contexttest-0002.jsonld");
        assertNotNull(contexttest);
        final Object context = JSONUtils.fromURL(contexttest);
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
        final Object context = JSONUtils.fromURL(url);
        assertEquals(1, requests.get());
        assertTrue(context instanceof Map);
        assertFalse(((Map<?, ?>) context).isEmpty());
    }

    protected HttpClient fakeHttpClient(ArgumentCaptor<HttpUriRequest> httpRequest)
            throws IllegalStateException, IOException {
        final HttpClient httpClient = mock(HttpClient.class);
        final HttpResponse fakeResponse = mock(HttpResponse.class);
        final StatusLine statusCode = mock(StatusLine.class);
        when(statusCode.getStatusCode()).thenReturn(200);
        when(fakeResponse.getStatusLine()).thenReturn(statusCode);
        final HttpEntity entity = mock(HttpEntity.class);
        when(entity.getContent()).thenReturn(
                JSONUtilsTest.class.getResourceAsStream("/custom/contexttest-0001.jsonld"));
        when(fakeResponse.getEntity()).thenReturn(entity);
        when(httpClient.execute(httpRequest.capture())).thenReturn(fakeResponse);
        return httpClient;
    }

    @Test
    public void fromURLAcceptHeaders() throws Exception {

        final URL url = new URL("http://example.com/fake-jsonld-test");
        final ArgumentCaptor<HttpUriRequest> httpRequest = ArgumentCaptor
                .forClass(HttpUriRequest.class);
        JSONUtils.setHttpClient(fakeHttpClient(httpRequest));
        try {
            final Object context = JSONUtils.fromURL(url);
            assertTrue(context instanceof Map);
        } finally {
            JSONUtils.setHttpClient(null);
        }
        assertEquals(1, httpRequest.getAllValues().size());
        final HttpUriRequest req = httpRequest.getValue();
        assertEquals(url.toURI(), req.getURI());

        final Header[] accept = req.getHeaders("Accept");
        assertEquals(1, accept.length);
        assertEquals(JSONUtils.ACCEPT_HEADER, accept[0].getValue());
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

}
