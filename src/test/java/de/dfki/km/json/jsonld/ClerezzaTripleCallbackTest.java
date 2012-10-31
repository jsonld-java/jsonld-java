package de.dfki.km.json.jsonld;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.junit.Test;

import de.dfki.km.json.JSONUtils;
import de.dfki.km.json.jsonld.impl.ClerezzaTripleCallback;

public class ClerezzaTripleCallbackTest {

	@Test
	public void triplesTest() throws IOException, JSONLDProcessingError {
		InputStream in = getClass().getClassLoader().getResourceAsStream("testfiles/product.jsonld");
		Object input = JSONUtils.fromInputStream(in);
		
		ClerezzaTripleCallback callback = new ClerezzaTripleCallback();

		JSONLD.toRDF(input, callback);
		MGraph graph = callback.getMGraph();

		for (Triple t : graph) {
			System.out.println(t);
		}
		assertEquals("Graph size",13, graph.size());
		
	}
	
	@Test
	public void curiesInContextTest() throws IOException, JSONLDProcessingError {
		InputStream in = getClass().getClassLoader().getResourceAsStream("testfiles/curies-in-context.jsonld");
		Object input = JSONUtils.fromInputStream(in);
		
		ClerezzaTripleCallback callback = new ClerezzaTripleCallback();

		JSONLD.toRDF(input, callback);
		MGraph graph = callback.getMGraph();

		for (Triple t : graph) {
			System.out.println(t);
			assertTrue("Predicate got fully expanded", t.getPredicate().getUnicodeString().startsWith("http"));
		}
		assertEquals("Graph size",3, graph.size());
		
	}
}
