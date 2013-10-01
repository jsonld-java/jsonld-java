package com.github.jsonldjava.jena;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.jsonldjava.core.JSONLD;
import com.github.jsonldjava.core.JSONLDProcessingError;
import com.github.jsonldjava.core.JSONLDTripleCallback;
import com.github.jsonldjava.impl.JenaTripleCallback;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

public class JenaTripleCallbackTest {

    @SuppressWarnings("serial")
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
                // Anonymous to URI
                add(new LinkedHashMap<String, Object>() {
                    {
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
                // Anonymous to self!
                add(new LinkedHashMap<String, Object>() {
                    {
                        // Anonymous
                        put("@id", "_:anon1");
                        put("http://example.com/self", new ArrayList<Object>() {
                            {
                                add(new LinkedHashMap<String, Object>() {
                                    {
                                        put("@id", "_:anon1");
                                    }
                                });
                            }
                        });
                    }
                });

                
            }
        };


        final JSONLDTripleCallback callback = new JenaTripleCallback();
        final Model model = (Model) JSONLD.toRDF(input, callback);
        
        Property homepage = model.getProperty("http://example.com/homepage");
        Property self = model.getProperty("http://example.com/self");
        Property code = model.getProperty("http://foo.com/code");
        
        Resource foo1 = model.getResource("http://localhost:8080/foo1");
        Resource foo2 = model.getResource("http://localhost:8080/foo2");

        assertEquals("123", model.getProperty(foo1,code).getString());
        assertEquals("ABC", model.getProperty(foo2,code).getString());

        Statement homepageSt = model.getProperty((Resource)null, homepage);
        assertTrue(homepageSt.getSubject().isAnon());
        assertEquals("http://www.example.com/", homepageSt.getResource().getURI());
        
        Statement selfSt = model.getProperty((Resource)null, self);
        assertEquals(selfSt.getSubject(), selfSt.getObject());
        assertTrue(selfSt.getObject().isAnon());
        
    }

}

