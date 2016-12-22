package com.github.jsonldjava.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.net.URL;

import org.junit.Test;

import com.github.jsonldjava.utils.JsonUtils;

public class ArrayContextToRDFTest {
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
}
