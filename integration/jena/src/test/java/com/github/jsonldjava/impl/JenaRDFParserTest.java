package com.github.jsonldjava.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.Test;

import com.github.jsonldjava.core.JSONLD;
import com.github.jsonldjava.core.JSONLDProcessingError;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

@SuppressWarnings("serial")
public class JenaRDFParserTest {

    final String prefix = "@prefix const: <http://foo.com/> .\n"
            + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n";
    final String foo1 = "<http://localhost:8080/foo1> const:code \"123\" .\n";
    final String foo2 = "<http://localhost:8080/foo2> const:code \"ABC\"^^xsd:string .\n";
    final String homepage = "_:a1 <http://example.com/homepage> <http://www.example.com/> .\n";
    final String self = "_:a2 <http://example.com/self> _:a2 .";
    
    LinkedHashMap<String, Object> foo1Json = new LinkedHashMap<String, Object>() {
        {
            put("@id", "http://localhost:8080/foo1");
            put("http://foo.com/code", new ArrayList<Object>() {
                {
                    add(new LinkedHashMap<String, Object>() {
                        {
                            put("@value", "123");
                        }
                    });
                }
            });
        }
    };
    LinkedHashMap<String, Object> foo2Json = new LinkedHashMap<String, Object>() {
        {
            put("@id", "http://localhost:8080/foo2");
            put("http://foo.com/code", new ArrayList<Object>() {
                {
                    add(new LinkedHashMap<String, Object>() {
                        {
                            put("@value", "ABC");
                        }
                    });
                }
            });
        }
    };
    // Anonymous to URI
    LinkedHashMap<String, Object> homepageJson = new LinkedHashMap<String, Object>() {
        {
            put("@id", "_:t0");
            put("http://example.com/homepage",
                    new ArrayList<Object>() {
                        {
                            add(new LinkedHashMap<String, Object>() {
                                {
                                    put("@id",
                                            "http://www.example.com/");
                                }
                            });
                        }
                    });
        }
    };
    // And the node for the www.example.com
    LinkedHashMap<String, Object> exampleCom = new LinkedHashMap<String, Object>() {
        {
            put("@id", "http://www.example.com/");
        }
    };
    // Anonymous to self!
    LinkedHashMap<String, Object> selfJson = new LinkedHashMap<String, Object>() {
        {
            // Anonymous
            put("@id", "_:t0");
            put("http://example.com/self", new ArrayList<Object>() {
                {
                    add(new LinkedHashMap<String, Object>() {
                        {
                            put("@id", "_:t0");
                        }
                    });
                }
            });
        }
    };
    
    @SuppressWarnings("rawtypes")
    @Test
    public void foo1() throws Exception {
        List expected = Collections.singletonList(foo1Json);
        List json = turtleToJsonLD(prefix  + foo1);
        assertEquals(expected, json);        
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void foo2() throws Exception {
        List expected = Collections.singletonList(foo2Json);
        List json = turtleToJsonLD(prefix  + foo2);
        assertEquals(expected, json);        
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void homepage() throws Exception {
        List json = turtleToJsonLD(prefix  + homepage);
        assertEquals(2, json.size());
        assertTrue(json.contains(exampleCom));
        assertTrue(json.contains(homepageJson));
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void self() throws Exception {
        List expected = Collections.singletonList(selfJson);
        List json = turtleToJsonLD(prefix  + self);
        assertEquals(expected, json);        
    }
    
    
    @SuppressWarnings("rawtypes")
    public static List turtleToJsonLD(String turtle) throws JSONLDProcessingError {        
        Model modelResult = ModelFactory.createDefaultModel().read(
                new ByteArrayInputStream(turtle.getBytes()), "", "TURTLE");
        JenaRDFParser parser = new JenaRDFParser();
        return (List) JSONLD.fromRDF(modelResult, parser);
    }
}
