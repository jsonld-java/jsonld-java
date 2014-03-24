package com.github.jsonldjava.jena;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;

import org.junit.BeforeClass;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class JenaJSONWriterTest {

    @BeforeClass
    public static void init() {
        JenaJSONLD.init();
    }

    @Test
    public void write() throws Exception {
        final Model model = ModelFactory.createDefaultModel();
        final Resource resource = model.createResource("http://example.com/test");
        final Property property = model.createProperty("http://example.com/value");
        model.add(resource, property, "Test");

        final StringWriter writer = new StringWriter();
        model.write(writer, "JSON-LD");

        final String json = writer.toString();
        // System.out.println(json);
        assertTrue(json.contains("@id"));
        assertTrue(json.contains("http://example.com/test"));
        assertTrue(json.contains("http://example.com/value"));
        assertTrue(json.contains("Test"));
    }

    @Test
    public void writeWithBase() throws Exception {
        final Model model = ModelFactory.createDefaultModel();
        final Resource resource = model.createResource("http://example.com/test");
        final Property property = model.createProperty("http://example.com/value");
        model.add(resource, property, "Test");

        final StringWriter writer = new StringWriter();
        model.write(writer, "JSON-LD", "http://example.com/");

        final String json = writer.toString();
        // System.out.println(json);
        assertTrue(json.contains("@id"));
        assertFalse(json.contains("http://example.com/test"));
        assertTrue(json.contains("\"test\""));
        // Note that the PROPERTY might not be relativized and still is
        // "http://example.com/value"
        assertTrue(json.contains("Test"));
    }

}
