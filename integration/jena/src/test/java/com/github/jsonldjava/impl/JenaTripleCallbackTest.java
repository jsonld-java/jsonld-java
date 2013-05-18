package com.github.jsonldjava.impl;

import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.jsonldjava.core.JSONLD;
import com.github.jsonldjava.core.JSONLDProcessingError;
import com.github.jsonldjava.core.JSONLDProcessor;
import com.github.jsonldjava.core.JSONLDTripleCallback;
import com.github.jsonldjava.impl.JenaTripleCallback;
import com.github.jsonldjava.utils.JSONUtils;

import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;


public class JenaTripleCallbackTest {

	@Test
	public void triplesTest() throws JsonParseException, JsonMappingException, JSONLDProcessingError {
		
		List<Map<String,Object>> input = new ArrayList<Map<String,Object>>() {{
	    	add(new LinkedHashMap<String, Object>() {{
	    		put("@id", "http://localhost:8080/foo1");
	    		put("http://foo.com/code", new ArrayList<Object>() {{
	    			add(new LinkedHashMap<String, Object>() {{
	    				put("@value", "123");
	    			}});
	    		}});
	    	}});
	    	add(new LinkedHashMap<String, Object>() {{
	    		put("@id", "http://localhost:8080/foo2");
	    		put("http://foo.com/code", new ArrayList<Object>() {{
	    			add(new LinkedHashMap<String, Object>() {{
	    				put("@value", "ABC");
	    			}});
	    		}});
	    	}});
	    }};
	    
	    List<String> expected = new ArrayList<String>() {{
	    	add("<http://localhost:8080/foo1> <http://foo.com/code> \"123\"^^<http://www.w3.org/2001/XMLSchema#string> .");
	    	add("<http://localhost:8080/foo2> <http://foo.com/code> \"ABC\"^^<http://www.w3.org/2001/XMLSchema#string> .");
	    }};
	    
	    JSONLDTripleCallback callback = new JenaTripleCallback();
	    Model model = (Model)JSONLD.toRDF(input, callback);
	    
	    StringWriter w = new StringWriter();
	    model.write(w, "N-TRIPLE");
	    
	    List<String> result = new ArrayList<String>(Arrays.asList(w.getBuffer().toString().split(System.getProperty("line.separator"))));
	    Collections.sort(result);
	    
	    assertTrue(JSONUtils.equals(expected, result));
	}
	
}
