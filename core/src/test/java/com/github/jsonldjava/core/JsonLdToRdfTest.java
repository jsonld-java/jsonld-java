package com.github.jsonldjava.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JsonLdToRdfTest {

    @Test
    public void testIssue301() throws JsonLdError {
        final RDFDataset rdf = new RDFDataset();
        rdf.addTriple(
                "http://www.w3.org/2002/07/owl#Class",
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
                "http://www.w3.org/2002/07/owl#Class");
        final JsonLdOptions opts = new JsonLdOptions();
        opts.setUseRdfType(Boolean.FALSE);
        opts.setProcessingMode(JsonLdOptions.JSON_LD_1_0);

        final Object out = new JsonLdApi(opts).fromRDF(rdf, true);
        assertEquals("[{@id=http://www.w3.org/2002/07/owl#Class, @type=[http://www.w3.org/2002/07/owl#Class]}]",
                out.toString());

        opts.setUseRdfType(Boolean.TRUE);

        final Object out2 = new JsonLdApi(opts).fromRDF(rdf, true);
        assertEquals("[{@id=http://www.w3.org/2002/07/owl#Class, http://www.w3.org/1999/02/22-rdf-syntax-ns#type=[{@id=http://www.w3.org/2002/07/owl#Class}]}]",
                out2.toString());
    }

    @Test
    public void testIssue329() throws Exception {
        final RDFDataset rdf = new RDFDataset();
        rdf.addTriple(
                "http://test.com/ontology#Class1",
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
                "http://www.w3.org/2002/07/owl#Class");
        rdf.addTriple(
                "http://test.com/ontology#Individual1",
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
                "http://test.com/ontology#Class1");
        final JsonLdOptions opts = new JsonLdOptions();
        opts.setUseRdfType(Boolean.FALSE);
        opts.setProcessingMode(JsonLdOptions.JSON_LD_1_0);

        final Object out = new JsonLdApi(opts).fromRDF(rdf, true);
        assertEquals("[{@id=http://test.com/ontology#Class1, @type=[http://www.w3.org/2002/07/owl#Class]}, "
                        + "{@id=http://test.com/ontology#Individual1, @type=[http://test.com/ontology#Class1]}]",
                out.toString());

        opts.setUseRdfType(Boolean.TRUE);

        final Object out2 = new JsonLdApi(opts).fromRDF(rdf, true);
        assertEquals("[{@id=http://test.com/ontology#Class1, "
                        + "http://www.w3.org/1999/02/22-rdf-syntax-ns#type=[{@id=http://www.w3.org/2002/07/owl#Class}]},"
                        + " {@id=http://test.com/ontology#Individual1, http://www.w3.org/1999/02/22-rdf-syntax-ns#type=[{@id=http://test.com/ontology#Class1}]}]",
                out2.toString());
    }

    @Test
    public void testIssue329_blankNode() throws Exception {
        final RDFDataset rdf = new RDFDataset();
        rdf.addTriple(
                "http://test.com/ontology#Individual1",
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
                "_:someThing");
        rdf.addTriple(
                "_:someThing",
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
                "http://test.com/ontology#Class1");
        final JsonLdOptions opts = new JsonLdOptions();
        opts.setUseRdfType(Boolean.FALSE);
        opts.setProcessingMode(JsonLdOptions.JSON_LD_1_0);

        final Object out = new JsonLdApi(opts).fromRDF(rdf, true);
        assertEquals("[{@id=_:someThing, @type=[http://test.com/ontology#Class1]}, "
                        + "{@id=http://test.com/ontology#Individual1, @type=[_:someThing]}]", out.toString());

        opts.setUseRdfType(Boolean.TRUE);

        final Object out2 = new JsonLdApi(opts).fromRDF(rdf, true);
        assertEquals("[{@id=_:someThing, http://www.w3.org/1999/02/22-rdf-syntax-ns#type=[{@id=http://test.com/ontology#Class1}]},"
                        + " {@id=http://test.com/ontology#Individual1, http://www.w3.org/1999/02/22-rdf-syntax-ns#type=[{@id=_:someThing}]}]",
                out2.toString());
    }
}
