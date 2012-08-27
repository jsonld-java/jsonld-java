package de.dfki.km.json.jsonld;


import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
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
}
