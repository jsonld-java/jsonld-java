package com.github.jsonldjava.impl;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.github.jsonldjava.core.JSONLD;
import com.github.jsonldjava.core.JSONLDProcessingError;
import com.github.jsonldjava.core.Options;
import com.github.jsonldjava.utils.JSONUtils;

public class TurtleRDFParserTest {

	@Test
	public void test() throws JSONLDProcessingError {
		
		String input = "@prefix ericFoaf: <http://www.w3.org/People/Eric/ericP-foaf.rdf#> .\n"
					 + "@prefix : <http://xmlns.com/foaf/0.1/> .\n"
					 + "ericFoaf:ericP :givenName \"Eric\" ;\n"
					 + "\t:knows <http://norman.walsh.name/knows/who/dan-brickley> ,\n"
					 + "\t\t[ :mbox <mailto:timbl@w3.org> ] ,\n"
				     + "\t\t<http://getopenid.com/amyvdh> .";
		
		List<Map<String,Object>> expected = new ArrayList<Map<String,Object>>() {{
	    	add(new LinkedHashMap<String, Object>() {{
	    		put("@id", "_:b1");
	    		put("http://xmlns.com/foaf/0.1/mbox", new ArrayList<Object>() {{
	    			add(new LinkedHashMap<String, Object>() {{
	    				put("@id", "mailto:timbl@w3.org");
	    			}});
	    		}});
	    	}});
	    	add(new LinkedHashMap<String, Object>() {{
				put("@id", "http://getopenid.com/amyvdh");
			}});
	    	add(new LinkedHashMap<String, Object>() {{
				put("@id", "http://norman.walsh.name/knows/who/dan-brickley");
			}});
	    	add(new LinkedHashMap<String,Object>() {{
	    		put("@id", "http://www.w3.org/People/Eric/ericP-foaf.rdf#ericP");
	    		put("http://xmlns.com/foaf/0.1/givenName", new ArrayList<Object>() {{
	    			add(new LinkedHashMap<String, Object>() {{
	    				put("@value", "Eric");
	    			}});
	    		}});
	    		put("http://xmlns.com/foaf/0.1/knows", new ArrayList<Object>() {{
	    			add(new LinkedHashMap<String, Object>() {{
	    				put("@id", "http://norman.walsh.name/knows/who/dan-brickley");
	    			}});
	    			add(new LinkedHashMap<String, Object>() {{
	    				put("@id", "_:b1");
	    			}});
	    			add(new LinkedHashMap<String, Object>() {{
	    				put("@id", "http://getopenid.com/amyvdh");
	    			}});
	    		}});
	    	}});
	    	add(new LinkedHashMap<String, Object>() {{
				put("@id", "mailto:timbl@w3.org");
			}});
	    }};
		
		Object json = JSONLD.fromRDF(
								input, 
								new Options() {{ 
									format = "text/turtle";
								}}, 
								new TurtleRDFParser()
								);
		
		assertTrue(JSONUtils.equals(expected, json));
	}
}
