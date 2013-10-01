package com.github.jsonldjava.jena;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;

import org.junit.Test;

import com.github.jsonldjava.jena.JenaJSONLD;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class JenaJSONWriterTest {
    
    static {
//        JenaJSONLDWriter.registerWithJena();
        JenaJSONLD.init(); 
    }
    
    @Test
    public void write() throws Exception {
        Model model = ModelFactory.createDefaultModel();
        Resource resource = model.createResource("http://example.com/test");
        Property property = model.createProperty("http://example.com/value");
        model.add(resource, property, "Test");
        
        StringWriter writer = new StringWriter();
        model.write(writer, "JSON-LD");
        
        String json = writer.toString();
//        System.out.println(json);
        assertTrue(json.contains("@id"));
        assertTrue(json.contains("http://example.com/test"));
        assertTrue(json.contains("http://example.com/value"));
        assertTrue(json.contains("Test"));
    }
    
    @Test
    public void writeWithBase() throws Exception {
        Model model = ModelFactory.createDefaultModel();
        Resource resource = model.createResource("http://example.com/test");
        Property property = model.createProperty("http://example.com/value");
        model.add(resource, property, "Test");
        
        StringWriter writer = new StringWriter();
        model.write(writer, "JSON-LD", "http://example.com/");
        
        String json = writer.toString();
//        System.out.println(json);
        assertTrue(json.contains("@id"));
        assertFalse(json.contains("http://example.com/test"));
        assertTrue(json.contains("\"test\""));
        // Note that the PROPERTY might not be relativized and still is "http://example.com/value"
        assertTrue(json.contains("Test"));
    }
    
}
