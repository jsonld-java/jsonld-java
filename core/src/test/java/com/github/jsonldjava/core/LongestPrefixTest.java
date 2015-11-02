package com.github.jsonldjava.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;

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
    public void toRdfWithNamespace2() throws Exception {

        final URL contextUrl = getClass().getResource("/custom/contexttest-0004.jsonld");
        assertNotNull(contextUrl);
        final Object context = JsonUtils.fromURL(contextUrl);
        assertNotNull(context);

        final JsonLdOptions options = new JsonLdOptions();
        options.useNamespaces = true;
        
        final RDFDataset rdf = (RDFDataset) JsonLdProcessor.toRDF(context, options);
        System.out.println(rdf.getNamespaces());
        assertEquals("http://dbpedia.org/resource/", rdf.getNamespace("dbr"));
        assertEquals("http://dbpedia.org/resource/Category:", rdf.getNamespace("dbc"));
    }

    @Test
    public void fromRdfWithNamespaceLexicographicallyShortestChosen() throws Exception {
        
        RDFDataset inputRdf = new RDFDataset();
        inputRdf.setNamespace("aat", "http://vocab.getty.edu/aat/");
        inputRdf.setNamespace("aat_rev", "http://vocab.getty.edu/aat/rev/");
        
        inputRdf.addTriple("http://vocab.getty.edu/aat/rev/5001065997", JsonLdConsts.RDF_TYPE, "http://vocab.getty.edu/aat/datatype");
        
        final JsonLdOptions options = new JsonLdOptions();
        options.useNamespaces = true;
        
        Object fromRDF = JsonLdProcessor.compact(new JsonLdApi(options).fromRDF(inputRdf),inputRdf.getContext(), options);
        
        String toJSONLD = JsonUtils.toPrettyString(fromRDF);
        System.out.println(toJSONLD);
        
        assertTrue("The lexicographically shortest URI was not chosen", toJSONLD.contains("aat:rev/"));

        final RDFDataset rdf = (RDFDataset) JsonLdProcessor.toRDF(fromRDF, options);
        System.out.println(rdf.getNamespaces());
        assertEquals("http://vocab.getty.edu/aat/", rdf.getNamespace("aat"));
        assertEquals("http://vocab.getty.edu/aat/rev/", rdf.getNamespace("aat_rev"));
        
    }

    @Test
    public void fromRdfWithNamespaceLexicographicallyShortestChosen2() throws Exception {
        
        RDFDataset inputRdf = new RDFDataset();
        inputRdf.setNamespace("aat", "http://vocab.getty.edu/aat/");
        inputRdf.setNamespace("aatrev", "http://vocab.getty.edu/aat/rev/");
        
        inputRdf.addTriple("http://vocab.getty.edu/aat/rev/5001065997", JsonLdConsts.RDF_TYPE, "http://vocab.getty.edu/aat/datatype");
        
        final JsonLdOptions options = new JsonLdOptions();
        options.useNamespaces = true;
        
        Object fromRDF = JsonLdProcessor.compact(new JsonLdApi(options).fromRDF(inputRdf),inputRdf.getContext(), options);
        
        String toJSONLD = JsonUtils.toPrettyString(fromRDF);
        System.out.println(toJSONLD);
        
        assertFalse("The lexicographically shortest URI was not chosen", toJSONLD.contains("aat:rev/"));

        final RDFDataset rdf = (RDFDataset) JsonLdProcessor.toRDF(fromRDF, options);
        System.out.println(rdf.getNamespaces());
        assertEquals("http://vocab.getty.edu/aat/", rdf.getNamespace("aat"));
        assertEquals("http://vocab.getty.edu/aat/rev/", rdf.getNamespace("aatrev"));
        
    }

    @Test
    public void fromRdfWithNamespaceLexicographicallyShortestChosen3() throws Exception {
        
        RDFDataset inputRdf = new RDFDataset();
        inputRdf.setNamespace("dbr", "http://dbpedia.org/resource/");
        inputRdf.setNamespace("dbc", "http://dbpedia.org/resource/Category:");
        
        inputRdf.addTriple("http://dbpedia.org/resource/Food", JsonLdConsts.RDF_TYPE, "http://dbpedia.org/resource/Category:Food_and_drink");
        
        final JsonLdOptions options = new JsonLdOptions();
        options.useNamespaces = true;
        
        Object fromRDF = JsonLdProcessor.compact(new JsonLdApi(options).fromRDF(inputRdf),inputRdf.getContext(), options);

        String toJSONLD = JsonUtils.toPrettyString(fromRDF);
        System.out.println(toJSONLD);
        
        assertFalse("The lexicographically shortest URI was not chosen", toJSONLD.contains("dbr:Category:"));
        
        final RDFDataset rdf = (RDFDataset) JsonLdProcessor.toRDF(fromRDF, options);
        System.out.println(rdf.getNamespaces());
        assertEquals("http://dbpedia.org/resource/", rdf.getNamespace("dbr"));
        assertEquals("http://dbpedia.org/resource/Category:", rdf.getNamespace("dbc"));
        
    }
}
