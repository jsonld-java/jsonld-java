package com.github.jsonldjava.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.util.Map;

import org.junit.Test;

import com.github.jsonldjava.utils.JsonUtils;

public class LongestPrefixTest {
    @Test
    public void toRdfWithNamespace() throws Exception {

        final URL contextUrl = getClass().getResource("/custom/contexttest-0003.jsonld");
        assertNotNull(contextUrl);
        final Object context = JsonUtils.fromURL(contextUrl);
        assertNotNull(context);

        final JsonLdOptions options = new JsonLdOptions();
        options.useNamespaces = true;
        final RDFDataset rdf = (RDFDataset) JsonLdProcessor.toRDF(context, options);
        System.out.println(rdf.getNamespaces());
        assertEquals("http://vocab.getty.edu/aat/", rdf.getNamespace("aat"));
        assertEquals("http://vocab.getty.edu/aat/rev/", rdf.getNamespace("aat_rev"));
    }

    @Test
    public void fromRdfWithNamespace() throws Exception {
        
        RDFDataset inputRdf = new RDFDataset();
        inputRdf.setNamespace("aat", "http://vocab.getty.edu/aat/");
        inputRdf.setNamespace("aat_rev", "http://vocab.getty.edu/aat/rev/");
        
        inputRdf.addTriple("http://vocab.getty.edu/aat/rev/5001065997", JsonLdConsts.RDF_TYPE, "http://vocab.getty.edu/aat/datatype");
        
        final JsonLdOptions options = new JsonLdOptions();
        options.useNamespaces = true;
        
        Object fromRDF = JsonLdProcessor.compact(new JsonLdApi(options).fromRDF(inputRdf),inputRdf.getContext(), options);
        
        final RDFDataset rdf = (RDFDataset) JsonLdProcessor.toRDF(fromRDF, options);
        System.out.println(rdf.getNamespaces());
        assertEquals("http://vocab.getty.edu/aat/", rdf.getNamespace("aat"));
        assertEquals("http://vocab.getty.edu/aat/rev/", rdf.getNamespace("aat_rev"));
        
        String toJSONLD = JsonUtils.toPrettyString(fromRDF);
        System.out.println(toJSONLD);
        
        assertFalse("Longest prefix was not used", toJSONLD.contains("aat:rev/"));
    }
}
