package com.github.jsonldjava.impl;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jsonldjava.core.JSONLD;
import com.github.jsonldjava.core.JSONLDProcessingError;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class JenaRDFParserTest {
    private static Logger logger = LoggerFactory.getLogger(JenaRDFParserTest.class);

    @Test
    public void test() throws JSONLDProcessingError {

        final String turtle = "@prefix const: <http://foo.com/> .\n"
                + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n"
                + "<http://localhost:8080/foo1> const:code \"123\" .\n"                
                + "<http://localhost:8080/foo2> const:code \"ABC\"^^xsd:string .\n"
                + "_:a1 <http://example.com/homepage> <http://www.example.com/> .\n"
                + "_:a1 <http://example.com/self> _:a2 .";

        final List<Map<String, Object>> expected = new ArrayList<Map<String, Object>>() {
            {
                add(new LinkedHashMap<String, Object>() {
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
                });
                add(new LinkedHashMap<String, Object>() {
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
                });
                // Anonymous to URI
                add(new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "_:t0");
                        put("http://example.com/homepage", new ArrayList<Object>() {
                            {
                                add(new LinkedHashMap<String, Object>() {
                                    {
                                        put("@id", "http://www.example.com/");
                                    }
                                });
                            }
                        });
                    }
                });
                // And the node for the www.example.com 
                add(new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.example.com/");
                    }
                });
                // Anonymous to self!
                add(new LinkedHashMap<String, Object>() {
                    {
                        // Anonymous
                        put("@id", "_:t1");
                        put("http://example.com/self", new ArrayList<Object>() {
                            {
                                add(new LinkedHashMap<String, Object>() {
                                    {
                                        put("@id", "_:t1");
                                    }
                                });
                            }
                        });
                    }
                });
            }
        };

        final Model modelResult = ModelFactory.createDefaultModel().read(
                new ByteArrayInputStream(turtle.getBytes()), "", "TURTLE");
        final JenaRDFParser parser = new JenaRDFParser();
        @SuppressWarnings("rawtypes")
        final List json = (List) JSONLD.fromRDF(modelResult, parser);
//        System.out.println(json);
        assertEquals(new HashSet(expected), new HashSet(json));
    }
}
