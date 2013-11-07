package com.github.jsonldjava.jena;

import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.jsonldjava.core.JSONLDTripleCallback;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.jena.JenaTripleCallback;
import com.github.jsonldjava.utils.Obj;
import com.hp.hpl.jena.rdf.model.Model;

public class JenaTripleCallbackTest {

    @Test
    public void triplesTest() throws JsonParseException, JsonMappingException, JsonLdError {

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
            }
        };

        final List<String> expected = new ArrayList<String>() {
            {
                add("<http://localhost:8080/foo1> <http://foo.com/code> \"123\"^^<http://www.w3.org/2001/XMLSchema#string> .");
                add("<http://localhost:8080/foo2> <http://foo.com/code> \"ABC\"^^<http://www.w3.org/2001/XMLSchema#string> .");
            }
        };

        final JSONLDTripleCallback callback = new JenaTripleCallback();
        final Model model = (Model) JsonLdProcessor.toRDF(input, callback);

        final StringWriter w = new StringWriter();
        model.write(w, "N-TRIPLE");

        final List<String> result = new ArrayList<String>(Arrays.asList(w.getBuffer().toString()
                .split(System.getProperty("line.separator"))));
        Collections.sort(result);

        assertTrue(Obj.equals(expected, result));
    }

}
