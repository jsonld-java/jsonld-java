package com.github.jsonldjava.impl;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.jsonldjava.core.JSONLD;
import com.github.jsonldjava.core.JSONLDProcessingError;
import com.github.jsonldjava.core.JSONLDTripleCallback;
import com.hp.hpl.jena.rdf.model.Model;

public class JenaTripleCallbackTest {

    @Test
    public void triplesTest() throws JsonParseException, JsonMappingException,
            JSONLDProcessingError {

        final List<Map<String, Object>> input = new ArrayList<Map<String, Object>>() {
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
                add(new LinkedHashMap<String, Object>() {
                    {
                        // Anonymous
                        put("http://foo.com/code", new ArrayList<Object>() {
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
                
            }
        };

        final Set<String> expected = new HashSet<String>() {
            {
                add("<http://localhost:8080/foo1> <http://foo.com/code> \"123\"^^<http://www.w3.org/2001/XMLSchema#string> .");
                add("<http://localhost:8080/foo2> <http://foo.com/code> \"ABC\"^^<http://www.w3.org/2001/XMLSchema#string> .");
                add("_:b0 <http://foo.com/code> <http://www.example.com/> .");
            }
        };

        final JSONLDTripleCallback callback = new JenaTripleCallback();
        final Model model = (Model) JSONLD.toRDF(input, callback);

        final StringWriter w = new StringWriter();
        model.write(w, "N-TRIPLE");

        System.out.println(w);
        
        final Set<String> result = new HashSet<String>(Arrays.asList(w.getBuffer().toString()
                .split(System.getProperty("line.separator"))));

        assertEquals(expected, result);        
    }

}

