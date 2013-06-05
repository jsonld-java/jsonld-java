package com.github.jsonldjava.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    
    @Test
    public void fromURLAcceptHeaders() throws Exception {
        final Map<String, List<String>> requestProperties = new HashMap<String, List<String>>();
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
                        if (! requestProperties.isEmpty()) {
                            fail("Multiple connections");
                        }
                        requestProperties.putAll(getRequestProperties());   
                        assertTrue(getRequestProperty("Accept").startsWith("application/ld+json,"));
                        return getClass().getResourceAsStream("/custom/contexttest-0001.jsonld");
                    }
                    
                };
            }
        };
        URL url = new URL(null, "jsonldtest:context", handler);
        Object context = JSONUtils.fromURL(url);
        assertTrue(context instanceof Map);
        assertFalse(requestProperties.isEmpty());
        assertEquals(1, requestProperties.get("Accept").size());
        String expected = "application/ld+json, application/json;q=0.9, application/javascript;q=0.5, text/javascript;q=0.5, text/plain;q=0.2, */*;q=0.1";
        assertEquals(expected, requestProperties.get("Accept").get(0));

    }
}
