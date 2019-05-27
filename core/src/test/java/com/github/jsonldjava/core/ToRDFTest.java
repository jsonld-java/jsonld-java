package com.github.jsonldjava.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;

import com.github.jsonldjava.utils.JsonUtils;
import com.github.jsonldjava.utils.TestUtils;

public class ToRDFTest {
    @Test
    public void toRdfWithNamespace() throws Exception {

        final URL contextUrl = getClass().getResource("/custom/contexttest-0001.jsonld");
        assertNotNull(contextUrl);
        final Object context = JsonUtils.fromURL(contextUrl, JsonUtils.getDefaultHttpClient());
        assertNotNull(context);

        final URL arrayContextUrl = getClass().getResource("/custom/array-context.jsonld");
        assertNotNull(arrayContextUrl);
        final Object arrayContext = JsonUtils.fromURL(arrayContextUrl,
                JsonUtils.getDefaultHttpClient());
        assertNotNull(arrayContext);
        final JsonLdOptions options = new JsonLdOptions();
        options.useNamespaces = true;
        // Fake document loader that always returns the imported context
        // from classpath
        final DocumentLoader documentLoader = new DocumentLoader() {
            @Override
            public RemoteDocument loadDocument(String url) throws JsonLdError {
                return new RemoteDocument("http://nonexisting.example.com/context", context);
            }
        };
        options.setDocumentLoader(documentLoader);
        final RDFDataset rdf = (RDFDataset) JsonLdProcessor.toRDF(arrayContext, options);
        // System.out.println(rdf.getNamespaces());
        assertEquals("http://example.org/", rdf.getNamespace("ex"));
        assertEquals("http://example.com/2/", rdf.getNamespace("ex2"));
        // Only 'proper' prefixes returned
        assertFalse(rdf.getNamespaces().containsKey("term1"));

    }

    @Test
    // See https://github.com/jsonld-java/jsonld-java/issues/232
    public void toRdfWithHttpBaseIri() throws IOException, JsonLdError {
        testToRdf("/custom/toRdf-0001-in.jsonld", "/custom/toRdf-0001-out.nq", "http://example.org/");
    }

    @Test
    // See https://github.com/jsonld-java/jsonld-java/issues/232
    public void toRdfWithHierarchicalBaseIri() throws IOException, JsonLdError {
        testToRdf("/custom/toRdf-0001-in.jsonld", "/custom/toRdf-0002-out.nq", "tag:/example/");
    }

    @Test
    // See https://github.com/jsonld-java/jsonld-java/issues/232#issuecomment-493454096
    public void toRdfWithOpaqueBaseIri() throws IOException, JsonLdError {
        testToRdf("/custom/toRdf-0001-in.jsonld", "/custom/toRdf-0003-out.nq", "tag:example/");
    }

    private void testToRdf(String inFile, String outFile, String baseIri) throws IOException {
        final Object input = JsonUtils
                .fromInputStream(getClass().getResourceAsStream(inFile));
        List<String> resultLines = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream(outFile), StandardCharsets.UTF_8)).lines()
                        .collect(Collectors.toList());
        JsonLdOptions options = new JsonLdOptions(baseIri);
        options.format = JsonLdConsts.APPLICATION_NQUADS;
        Object result = JsonLdProcessor.toRDF(input, options);
        assertEquals(TestUtils.join(resultLines, "\n").trim(), ((String) result).trim());
    }
}
