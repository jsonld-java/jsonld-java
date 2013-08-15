package com.github.jsonldjava.impl;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class JenaJSONReaderTest {
    static {
        JenaJSONLDReader.registerWithJena();
    }

    @Test
    public void read() throws Exception {
        Model model = ModelFactory.createDefaultModel();
        String jsonld = " { '@id': 'test', \n" +
        		"   'http://example.com/value': 'Test' \n }  ";
        jsonld = jsonld.replace('\'', '"');
        InputStream in = new ByteArrayInputStream(jsonld.getBytes("utf8"));
        
        String base = "http://example.com/";
        model.read(in, base, "JSON-LD");

        assertEquals(1, model.size());
        Resource test = model.getResource("http://example.com/test");
        Property value = model.getProperty("http://example.com/value");
        assertEquals("Test", model.getProperty(test, value).getString());
    }
}
