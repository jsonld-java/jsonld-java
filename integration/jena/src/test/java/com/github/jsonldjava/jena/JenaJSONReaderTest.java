package com.github.jsonldjava.jena;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.BeforeClass;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;

public class JenaJSONReaderTest {

    @BeforeClass
    public static void init() {
        JenaJSONLD.init();
    }

    @Test
    public void readInputStream() throws Exception {
        final Model model = ModelFactory.createDefaultModel();
        String jsonld = " { '@id': 'test', \n" + "   'http://example.com/value': 'Test' \n }  ";
        jsonld = jsonld.replace('\'', '"');
        final InputStream in = new ByteArrayInputStream(jsonld.getBytes("utf8"));

        final String baseUri = "http://example.com/";
        model.read(in, baseUri, "JSON-LD");
        // model.write(System.out, "TURTLE", "");
        checkRelative(model);
    }

    private void checkRelative(Model model) {
        assertEquals(1, model.size());
        final Statement statement = model.listStatements().next();
        assertEquals("http://example.com/value", statement.getPredicate().toString());
        assertEquals("Test", statement.getString());
        assertEquals("http://example.com/test", statement.getSubject().toString());
    }

    // @Ignore("Integration test")
    @Test
    public void readURL() throws Exception {
        final Model model = ModelFactory.createDefaultModel();
        final String url = getClass().getResource("../jena/relative.jsonld").toExternalForm();
        final String baseUri = "http://example.com/";
        model.read(url, baseUri, "JSON-LD");
        model.write(System.out, "TURTLE", "");
        checkRelative(model);
    }
}
