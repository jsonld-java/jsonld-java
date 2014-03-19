package com.github.jsonldjava.core;

import static org.junit.Assert.*;

import java.net.URL;

import org.junit.Test;

import com.github.jsonldjava.utils.JsonUtils;

public class ArrayContextToRDFTest {
	@Test
	public void toRdfWithNamespace() throws Exception {
		
		URL contextUrl = getClass().getResource("/custom/contexttest-0001.jsonld");
		assertNotNull(contextUrl);
		final Object context = JsonUtils.fromURL(contextUrl);
		assertNotNull(context);
		
		URL arrayContextUrl = getClass().getResource("/custom/array-context.jsonld");
		assertNotNull(arrayContextUrl);
		Object arrayContext = JsonUtils.fromURL(arrayContextUrl);
		assertNotNull(arrayContext);
		JsonLdOptions options = new JsonLdOptions();
		options.useNamespaces = true;
		// Fake document loader that always returns the imported context
		// from classpath
		DocumentLoader documentLoader = new DocumentLoader() {
			@Override
			public RemoteDocument loadDocument(String url) throws JsonLdError {
				return new RemoteDocument("http://nonexisting.example.com/context",
						context);
			}
		};
		options.setDocumentLoader(documentLoader);
		RDFDataset rdf = (RDFDataset) JsonLdProcessor.toRDF(arrayContext, options);
		System.out.println(rdf.getNamespaces());
		assertEquals("http://example.org/", rdf.getNamespace("ex"));
		assertEquals("http://example.com/2/", rdf.getNamespace("ex2"));
		// Only 'proper' prefixes returned
		assertFalse(rdf.getNamespaces().containsKey("term1"));
		
		
	}
}
