package de.dfki.km.json.jsonld;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.junit.Test;

import de.dfki.km.json.JSONUtils;
import de.dfki.km.json.jsonld.impl.ClerezzaTripleCallback;

public class ClerezzaTripleCallbackTest {

	@Test
	public void triplesTest() throws IOException {
		InputStream in = getClass().getClassLoader().getResourceAsStream("testfiles/product.jsonld");
		Object input = JSONUtils.fromInputStream(in);
		
		JSONLDProcessor processor = new JSONLDProcessor();
		ClerezzaTripleCallback callback = new ClerezzaTripleCallback();

		processor.triples(input, callback);
		MGraph graph = callback.getMGraph();

		for (Triple t : graph) {
			System.out.println(t);
		}
		assertEquals("Graph size",13, graph.size());
		
	}
	
	@Test
	public void curiesInContextTest() throws IOException {
		InputStream in = getClass().getClassLoader().getResourceAsStream("testfiles/curies-in-context.jsonld");
		Object input = JSONUtils.fromInputStream(in);
		
		JSONLDProcessor processor = new JSONLDProcessor();
		ClerezzaTripleCallback callback = new ClerezzaTripleCallback();

		processor.triples(input, callback);
		MGraph graph = callback.getMGraph();

		for (Triple t : graph) {
			System.out.println(t);
			assertTrue("Predicate got fully expanded", t.getPredicate().getUnicodeString().startsWith("http"));
			if (t.getPredicate().equals(new UriRef("http://xmlns.com/foaf/0.1/knows"))) {
				assertTrue("Object of foaf:knows is a UriRes", t.getObject() instanceof UriRef);
			}
		}
		assertEquals("Graph size",4, graph.size());
		
	}
}
