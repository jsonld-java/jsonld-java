package com.github.jsonldjava.rdf2go;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.ontoware.rdf2go.RDF2Go;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Syntax;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.Obj;

/**
 * Unit tests for {@link RDF2GoRDFParser} containing a single test, including
 * literals with datatype and language.
 *
 * @author Ismael Rivera
 */
public class RDF2GoRDFParserTest {

    @Test
    public void testFromRDF() throws JsonLdError, IOException {

        final String turtle = "@prefix const: <http://foo.com/> .\n"
                + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n"
                + "<http://localhost:8080/foo1> const:code \"123\" .\n"
                + "<http://localhost:8080/foo2> const:code \"23.3364\"^^xsd:decimal .\n"
                + "<http://localhost:8080/foo3> const:code \"ABC\"^^xsd:string .\n"
                + "<http://localhost:8080/foo4> const:code \"English\"@en .\n";

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
                                        put("@value", "23.3364");
                                        put("@type", "http://www.w3.org/2001/XMLSchema#decimal");
                                    }
                                });
                            }
                        });
                    }
                });
                add(new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://localhost:8080/foo3");
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
                        put("@id", "http://localhost:8080/foo4");
                        put("http://foo.com/code", new ArrayList<Object>() {
                            {
                                add(new LinkedHashMap<String, Object>() {
                                    {
                                        put("@value", "English");
                                        put("@language", "en");
                                    }
                                });
                            }
                        });
                    }
                });
            }
        };

        final RDF2GoRDFParser parser = new RDF2GoRDFParser();

        final Model modelResult = RDF2Go.getModelFactory().createModel().open();
        modelResult.readFrom(new ByteArrayInputStream(turtle.getBytes()), Syntax.Turtle);
        final Object json = JsonLdProcessor.fromRDF(modelResult, parser);

        assertTrue(Obj.equals(json, expected));
    }

}
