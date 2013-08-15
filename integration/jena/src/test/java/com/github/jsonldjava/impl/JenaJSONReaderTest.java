package com.github.jsonldjava.impl;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Ignore;
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
    public void readInputStream() throws Exception {
        Model model = ModelFactory.createDefaultModel();
        String jsonld = " { '@id': 'test', \n" +
        		"   'http://example.com/value': 'Test' \n }  ";
        jsonld = jsonld.replace('\'', '"');
        InputStream in = new ByteArrayInputStream(jsonld.getBytes("utf8"));
        
        String uri = "http://example.com/";
        model.read(in, uri, "JSON-LD");
//        model.write(System.out, "TURTLE", "");
        assertEquals(1, model.size());
        Resource test = model.getResource("http://example.com/test");
        Property value = model.getProperty("http://example.com/value");
        assertEquals("Test", model.getProperty(test, value).getString());
    }
    
    @Ignore("Integration test")
    @Test
    public void readURL() throws Exception {
        Model model = ModelFactory.createDefaultModel();
        String url = "http://json-ld.org/test-suite/tests/expand-0002-in.jsonld";
        model.read(url, "JSON-LD");
        model.write(System.out, "TURTLE", "");
//        assertEquals(7, model.size());
        Resource test = model.getResource("http://example.com/id1");
        Property value = model.getProperty("http://example.com/term1");
        assertEquals("v1", model.getProperty(test, value).getString());
    }
}
