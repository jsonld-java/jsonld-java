package com.github.jsonldjava.jena;

import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import com.github.jsonldjava.core.JSONLD;
import com.github.jsonldjava.core.Options;
import com.github.jsonldjava.utils.JSONUtils;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class JSONLDToRDFTest {
    
    @BeforeClass
    public static void init() {
        JenaJSONLD.init(); 
    }
    
    @Test
    public void write() throws Exception {
        Model model = ModelFactory.createDefaultModel();
        Resource resource = model.createResource("http://example.com/test");
        Property property = model.createProperty("http://example.com/value");
        model.add(resource, property, "Test");

        Options options = new Options();
        options.format = "application/ld+json";
        Object json = JSONLD.fromRDF(model, options);
        String jsonStr = JSONUtils.toPrettyString(json);
//        System.out.println(jsonStr);
        assertTrue(jsonStr.contains("@id"));
        assertTrue(jsonStr.contains("http://example.com/test"));
        assertTrue(jsonStr.contains("http://example.com/value"));
        assertTrue(jsonStr.contains("Test"));
    }
    
}
