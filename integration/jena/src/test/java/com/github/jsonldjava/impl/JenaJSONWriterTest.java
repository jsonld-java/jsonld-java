package com.github.jsonldjava.impl;

import static org.junit.Assert.*;

import java.io.StringWriter;

import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class JenaJSONWriterTest {
    
    static {
        JenaJSONLDWriter.registerWithJena();
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
    
}
