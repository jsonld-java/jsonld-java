package com.github.jsonldjava.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;

import org.junit.jupiter.api.Test;

import com.github.jsonldjava.utils.JsonUtils;

public class LongestPrefixTest {
    @Test
    public void toRdfWithNamespace() throws Exception {

        final URL contextUrl = getClass().getResource("/custom/contexttest-0003.jsonld");
        assertNotNull(contextUrl);
        final Object context = JsonUtils.fromURL(contextUrl, JsonUtils.getDefaultHttpClient());
        assertNotNull(context);

        final JsonLdOptions options = new JsonLdOptions();
        options.useNamespaces = true;
        final RDFDataset rdf = (RDFDataset) JsonLdProcessor.toRDF(context, options);
        // System.out.println(rdf.getNamespaces());
        assertEquals("http://vocab.getty.edu/aat/", rdf.getNamespace("aat"));
        assertEquals("http://vocab.getty.edu/aat/rev/", rdf.getNamespace("aat_rev"));
    }

    @Test
    public void fromRdfWithNamespaceLexicographicallyShortestChosen() throws Exception {

        final RDFDataset inputRdf = new RDFDataset();
        inputRdf.setNamespace("aat", "http://vocab.getty.edu/aat/");
        inputRdf.setNamespace("aat_rev", "http://vocab.getty.edu/aat/rev/");

        inputRdf.addTriple("http://vocab.getty.edu/aat/rev/5001065997", JsonLdConsts.RDF_TYPE,
                "http://vocab.getty.edu/aat/datatype");

        final JsonLdOptions options = new JsonLdOptions();
        options.useNamespaces = true;

        final Object fromRDF = JsonLdProcessor.compact(new JsonLdApi(options).fromRDF(inputRdf),
                inputRdf.getContext(), options);

        final RDFDataset rdf = (RDFDataset) JsonLdProcessor.toRDF(fromRDF, options);
        // System.out.println(rdf.getNamespaces());
        assertEquals("http://vocab.getty.edu/aat/", rdf.getNamespace("aat"));
        assertEquals("http://vocab.getty.edu/aat/rev/", rdf.getNamespace("aat_rev"));

        final String toJSONLD = JsonUtils.toPrettyString(fromRDF);
        // System.out.println(toJSONLD);

        assertTrue(toJSONLD.contains("aat:rev/"),
                "The lexicographically shortest URI was not chosen");
    }

    @Test
    public void fromRdfWithNamespaceLexicographicallyShortestChosen2() throws Exception {

        final RDFDataset inputRdf = new RDFDataset();
        inputRdf.setNamespace("aat", "http://vocab.getty.edu/aat/");
        inputRdf.setNamespace("aatrev", "http://vocab.getty.edu/aat/rev/");

        inputRdf.addTriple("http://vocab.getty.edu/aat/rev/5001065997", JsonLdConsts.RDF_TYPE,
                "http://vocab.getty.edu/aat/datatype");

        final JsonLdOptions options = new JsonLdOptions();
        options.useNamespaces = true;

        final Object fromRDF = JsonLdProcessor.compact(new JsonLdApi(options).fromRDF(inputRdf),
                inputRdf.getContext(), options);

        final RDFDataset rdf = (RDFDataset) JsonLdProcessor.toRDF(fromRDF, options);
        // System.out.println(rdf.getNamespaces());
        assertEquals("http://vocab.getty.edu/aat/", rdf.getNamespace("aat"));
        assertEquals("http://vocab.getty.edu/aat/rev/", rdf.getNamespace("aatrev"));

        final String toJSONLD = JsonUtils.toPrettyString(fromRDF);
        // System.out.println(toJSONLD);

        assertFalse(toJSONLD.contains("aat:rev/"),
                "The lexicographically shortest URI was not chosen");
    }

    @Test
    public void prefixUsedToShortenPredicate() throws Exception {
        final RDFDataset inputRdf = new RDFDataset();
        inputRdf.setNamespace("ex", "http://www.a.com/foo/");
        inputRdf.addTriple("http://www.a.com/foo/s", "http://www.a.com/foo/p",
                "http://www.a.com/foo/o");
        assertEquals("http://www.a.com/foo/", inputRdf.getNamespace("ex"));

        final JsonLdOptions options = new JsonLdOptions();
        options.useNamespaces = true;

        final Object fromRDF = JsonLdProcessor.compact(new JsonLdApi(options).fromRDF(inputRdf),
                inputRdf.getContext(), options);
        final String toJSONLD = JsonUtils.toPrettyString(fromRDF);
        // System.out.println(toJSONLD);

        assertFalse(toJSONLD.contains("http://www.a.com/foo/p"),
                "The lexicographically shortest URI was not chosen");
    }

}
