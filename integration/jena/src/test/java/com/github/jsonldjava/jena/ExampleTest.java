package com.github.jsonldjava.jena;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Ignore;
import org.junit.Test;

import com.github.jsonldjava.core.JsonLdApi;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.RDFDataset;
import com.github.jsonldjava.utils.JSONUtils;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Examples from README.md
 */
public class ExampleTest {

    @Ignore("Integration test")
    @Test
    public void jsonldToTurtleRIOT() throws Exception {
        JenaJSONLD.init(); // Only needed once
        final String url = "http://json-ld.org/test-suite/tests/expand-0002-in.jsonld";
        // Detects language based on extension (ideally content type)
        final Model model = RDFDataMgr.loadModel(url);

        // or explicit with base URI, Lang and any supported source
        final InputStream inStream = new ByteArrayInputStream("{}".getBytes("UTF-8"));
        RDFDataMgr.read(model, inStream, "http://example.com/", JenaJSONLD.JSONLD);

        RDFDataMgr.write(System.out, model, Lang.TURTLE);
        // <http://example.com/id1>
        // a <http://example.com/t1> ;
        // <http://example.com/term1>
        // "v1"^^<http://www.w3.org/2001/XMLSchema#string> ;
        // <http://example.com/term2> "v2"^^<http://example.com/t2> ;
        // <http://example.com/term3> "v3"@en ;
        // <http://example.com/term4> 4 ;
        // <http://example.com/term5> 51 , 50 .
    }

    @Test
    public void modelTojsonldRIOT() throws Exception {
        JenaJSONLD.init(); // Only needed once

        final Model model = ModelFactory.createDefaultModel();
        final Resource resource = model.createResource("http://example.com/test");
        final Property property = model.createProperty("http://example.com/value");
        model.add(resource, property, "Test");

        RDFDataMgr.write(System.out, model, JenaJSONLD.JSONLD);
        // {
        // "@context" : {
        // "value" : {
        // "@id" : "http://example.com/value",
        // "@type" : "@id"
        // }
        // },
        // "@id" : "http://example.com/test",
        // "http://example.com/value" : "Test"
        // }

        // Or more compact:
        RDFDataMgr.write(System.out, model, JenaJSONLD.JSONLD_FORMAT_FLAT);
        // "@context":{"value":{"@id":"http://example.com/value","@type":"@id"}},"@id":"http://example.com/test","http://example.com/value":"Test"}

        // Datasets are also supported
        final Dataset dataset = DatasetFactory.createMem();
        dataset.addNamedModel("http://example.com/graph", model);
        RDFDataMgr.write(System.out, dataset, JenaJSONLD.JSONLD);
        // {
        // "@graph" : [ {
        // "@id" : "http://example.com/test",
        // "http://example.com/value" : "Test"
        // } ],
        // "@id" : "http://example.com/graph"
        // }

    }

    @Test
    public void modelToJsonldClassic() throws Exception {
        JenaJSONLD.init(); // Only needed once

        final Model model = ModelFactory.createDefaultModel();
        final Resource resource = model.createResource("http://example.com/test");
        final Property property = model.createProperty("http://example.com/value");
        model.add(resource, property, "Test");
        model.write(System.out, "JSON-LD");
        // {
        // "@context" : {
        // "value" : {
        // "@id" : "http://example.com/value",
        // "@type" : "@id"
        // }
        // },
        // "@id" : "http://example.com/test",
        // "http://example.com/value" : "Test"
        // }

        // Or made relative from a base URI
        // (notice the relative @id below)
        model.write(System.out, "JSON-LD", "http://example.com/");
        // {
        // "@context" : {
        // "value" : {
        // "@id" : "http://example.com/value",
        // "@type" : "@id"
        // }
        // },
        // "@id" : "test",
        // "http://example.com/value" : "Test"
        // }
    }

    @Ignore("Integration test")
    @Test
    public void jsonldToTurtleClassic() throws Exception {
        JenaJSONLD.init(); // Only needed once

        final String url = "http://json-ld.org/test-suite/tests/expand-0002-in.jsonld";
        final Model model = ModelFactory.createDefaultModel();
        model.read(url, "JSON-LD");
        model.write(System.out, "TURTLE", "http://example.com/");
        // @base <http://example.com/> .
        // <id1> a <t1> ;
        // <term1> "v1"^^<http://www.w3.org/2001/XMLSchema#string> ;
        // <term2> "v2"^^<t2> ;
        // <term3> "v3"@en ;
        // <term4> 4 ;
        // <term5> 51 , 50 .
    }

    @Test
    public void modelToJsonLD() throws Exception {
        JenaJSONLD.init(); // Only needed once
        final Model model = ModelFactory.createDefaultModel();
        final Resource resource = model.createResource("http://example.com/test");
        final Property property = model.createProperty("http://example.com/value");
        model.add(resource, property, "Test");

        final JsonLdOptions options = new JsonLdOptions();
        options.format = "application/ld+json";
        JsonLdApi api = new JsonLdApi(options);
        RDFDataset dataset = new RDFDataset(api);
        final Object json = api.fromRDF(dataset);
        final String jsonStr = JSONUtils.toPrettyString(json);
        System.out.println(jsonStr);
        // [ {
        // "@id" : "http://example.com/test",
        // "http://example.com/value" : [ {
        // "@value" : "Test"
        // } ]
        // } ]
    }
}
