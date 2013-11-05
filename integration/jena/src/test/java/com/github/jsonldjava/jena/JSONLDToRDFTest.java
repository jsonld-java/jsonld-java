package com.github.jsonldjava.jena;

import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import com.github.jsonldjava.core.JsonLdApi;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.RDFDataset;
import com.github.jsonldjava.impl.JenaRDFParser;
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
        final Model model = ModelFactory.createDefaultModel();
        final Resource resource = model.createResource("http://example.com/test");
        final Property property = model.createProperty("http://example.com/value");
        model.add(resource, property, "Test");

        final JsonLdOptions options = new JsonLdOptions();
        options.format = "application/ld+json";
        final JenaRDFParser parser = new JenaRDFParser();
        final RDFDataset dataset = parser.parse(model);
        final Object json = new JsonLdApi(options).fromRDF(dataset);
        final String jsonStr = JSONUtils.toPrettyString(json);
        // System.out.println(jsonStr);
        assertTrue(jsonStr.contains("@id"));
        assertTrue(jsonStr.contains("http://example.com/test"));
        assertTrue(jsonStr.contains("http://example.com/value"));
        assertTrue(jsonStr.contains("Test"));
    }

}
