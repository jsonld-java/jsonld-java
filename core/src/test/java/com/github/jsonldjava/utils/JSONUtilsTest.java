package com.github.jsonldjava.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Ignore;
import org.junit.Test;


public class JSONUtilsTest {
	
	@SuppressWarnings("unchecked")
    @Test
	public void fromStringTest() {
		String testString = "{\"seq\":3,\"id\":\"e48dfa735d9fad88db6b7cd696002df7\",\"changes\":[{\"rev\":\"2-6aebf275bc3f29b67695c727d448df8e\"}]}";
		String testFailure = "{{{{{{{{{{{";
		Object obj = null;
		
		try {
			obj = JSONUtils.fromString(testString);
		} catch (Exception e) {
			assertTrue(false);
		}
		
		assertTrue(((Map<String,Object>) obj).containsKey("seq"));
		assertTrue(((Map<String,Object>) obj).get("seq") instanceof Number);
	
		try {
			obj = JSONUtils.fromString(testFailure);
			assertTrue(false);
		} catch (Exception e) {
			assertTrue(true);
		}		
	}
	
	@SuppressWarnings("unchecked")
    @Test
    public void fromURLTest0001() throws Exception {
	    URL contexttest = getClass().getResource("/custom/contexttest-0001.jsonld");
	    assertNotNull(contexttest);
	    Object context = JSONUtils.fromURL(contexttest);
	    assertTrue(context instanceof Map);
	    Map<String, Object> contextMap = (Map<String, Object>) context;
	    assertEquals(1, contextMap.size());
	    Map<String,Object> cont = (Map<String, Object>) contextMap.get("@context");
	    assertEquals(3, cont.size());
	    assertEquals("http://example.org/", cont.get("ex"));
	    Map<String,Object> term1 = (Map<String, Object>) cont.get("term1");
	    assertEquals("ex:term1", term1.get("@id"));
	}

    @SuppressWarnings("unchecked")
    @Test
    public void fromURLTest0002() throws Exception {
        URL contexttest = getClass().getResource(
                "/custom/contexttest-0002.jsonld");
        assertNotNull(contexttest);
        Object context = JSONUtils.fromURL(contexttest);
        assertTrue(context instanceof List);
        List<Map<String, Object>> contextList = (List<Map<String, Object>>) context;
        
        Map<String, Object> contextMap1 = contextList.get(0);
        assertEquals(1, contextMap1.size());
        Map<String, Object> cont1 = (Map<String, Object>) contextMap1
                .get("@context");
        assertEquals(2, cont1.size());
        assertEquals("http://example.org/", cont1.get("ex"));
        Map<String, Object> term1 = (Map<String, Object>) cont1.get("term1");
        assertEquals("ex:term1", term1.get("@id"));
        
        Map<String, Object> contextMap2 = contextList.get(1);
        assertEquals(1, contextMap2.size());
        Map<String, Object> cont2 = (Map<String, Object>) contextMap2
                .get("@context");
        assertEquals(1, cont2.size());
        Map<String, Object> term2 = (Map<String, Object>) cont2.get("term2");
        assertEquals("ex:term2", term2.get("@id"));
    }
    
    @Ignore("Integration test")
    @Test
    public void fromURLredirectHTTPSToHTTP() throws Exception {
        URL url = new URL("https://w3id.org/bundle/context");
        Object context = JSONUtils.fromURL(url);
        // Should not fail because of http://stackoverflow.com/questions/1884230/java-doesnt-follow-redirect-in-urlconnection
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4620571
        assertTrue(context instanceof Map);
    }
    

    @Ignore("Integration test")
    @Test
    public void fromURLredirect() throws Exception {
        URL url = new URL("http://purl.org/wf4ever/ro-bundle/context.json");
        Object context = JSONUtils.fromURL(url);
        assertTrue(context instanceof Map);
    }
    
    
    @Test
    public void fromURLCustomHandler() throws Exception {
        final AtomicInteger requests = new AtomicInteger();
        URLStreamHandler handler = new URLStreamHandler() {            
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
        URL url = new URL(null, "jsonldtest:context", handler);
        assertEquals(0, requests.get());
        Object context = JSONUtils.fromURL(url);
        assertEquals(1, requests.get());
        assertTrue(context instanceof Map);
        
//        assertEquals(1, requestProperties.get("Accept").size());
//        String expected = "application/ld+json, application/json;q=0.9, application/javascript;q=0.5, text/javascript;q=0.5, text/plain;q=0.2, */*;q=0.1";
//        assertEquals(expected, requestProperties.get("Accept").get(0));

    }
}
