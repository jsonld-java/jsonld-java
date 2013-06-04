package com.github.jsonldjava.utils;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.github.jsonldjava.utils.JSONUtils;


public class JSONUtilsTest {
	
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
}
