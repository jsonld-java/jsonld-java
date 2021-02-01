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

}
