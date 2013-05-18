package com.github.jsonldjava.impl;


import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.jsonldjava.core.JSONLD;
import com.github.jsonldjava.core.JSONLDProcessingError;
import com.github.jsonldjava.impl.JenaRDFParser;
import com.github.jsonldjava.utils.JSONUtils;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class JenaRDFParserTest {
	private static Logger logger = LoggerFactory
	        .getLogger(JenaRDFParserTest.class);

	@Test
	public void test() throws JSONLDProcessingError {

	    String turtle = "@prefix const: <http://foo.com/> .\n"
	            + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n"
	            + "<http://localhost:8080/foo1> const:code \"123\" .\n"
	            + "<http://localhost:8080/foo2> const:code \"ABC\"^^xsd:string .\n";
	    
	    List<Map<String,Object>> expected = new ArrayList<Map<String,Object>>() {{
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

	    Model modelResult = ModelFactory.createDefaultModel().read(new ByteArrayInputStream(turtle.getBytes()), "",
	            "TURTLE");
	    JenaRDFParser parser = new JenaRDFParser();
	    Object json = JSONLD.fromRDF(modelResult, parser);
	    
	    assertTrue(JSONUtils.equals(json, expected));
	}
}
