package com.github.jsonldjava.core;

import com.github.jsonldjava.utils.JsonUtils;
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
    public void testExpandedList() throws Exception {
        RDFDataset rdf = new RDFDataset();
        rdf.addTriple(
                "_:b0",
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#first",
                "Arnold");
        rdf.addTriple(
                "_:b0",
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#rest",
                "_:b1");
        rdf.addTriple(
                "_:b1",
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#first",
                "Bob");
        rdf.addTriple(
                "_:b1",
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#rest",
                "_:b2");
        rdf.addTriple(
                "_:b2",
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#first",
                "Catherine");
        rdf.addTriple(
                "_:b2",
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#rest",
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#nil");

        JsonLdOptions opts = new JsonLdOptions();
        assertEquals("[{\"@id\":\"_:b0\",\"http://www.w3.org/1999/02/22-rdf-syntax-ns#first\":[{\"@id\":\"Arnold\"}],\"http://www.w3.org/1999/02/22-rdf-syntax-ns#rest\":[{\"@list\":[{\"@id\":\"Bob\"},{\"@id\":\"Catherine\"}]}]}]", JsonUtils.toString(new JsonLdApi(opts).fromRDF(rdf)));

        opts.setExpandBNodeList(true);
        assertEquals("[{\"@id\":\"_:b0\",\"http://www.w3.org/1999/02/22-rdf-syntax-ns#first\":[{\"@id\":\"Arnold\"}],\"http://www.w3.org/1999/02/22-rdf-syntax-ns#rest\":[{\"@id\":\"_:b1\"}]},{\"@id\":\"_:b1\",\"http://www.w3.org/1999/02/22-rdf-syntax-ns#first\":[{\"@id\":\"Bob\"}],\"http://www.w3.org/1999/02/22-rdf-syntax-ns#rest\":[{\"@id\":\"_:b2\"}]},{\"@id\":\"_:b2\",\"http://www.w3.org/1999/02/22-rdf-syntax-ns#first\":[{\"@id\":\"Catherine\"}],\"http://www.w3.org/1999/02/22-rdf-syntax-ns#rest\":[{\"@id\":\"http://www.w3.org/1999/02/22-rdf-syntax-ns#nil\"}]}]", JsonUtils.toString(new JsonLdApi(opts).fromRDF(rdf)));

    }
}
