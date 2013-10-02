package com.github.jsonldjava.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.junit.Test;

import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.utils.JSONUtils;

public class ClerezzaTripleCallbackTest {

    @Test
    public void triplesTest() throws IOException, JsonLdError {
        final InputStream in = getClass().getClassLoader().getResourceAsStream(
                "testfiles/product.jsonld");
        final Object input = JSONUtils.fromInputStream(in);

        final ClerezzaTripleCallback callback = new ClerezzaTripleCallback();

        final MGraph graph = (MGraph) JsonLdProcessor.toRDF(input, callback);

        for (final Triple t : graph) {
            System.out.println(t);
        }
        assertEquals("Graph size", 13, graph.size());

    }

    @Test
    public void curiesInContextTest() throws IOException, JsonLdError {
        final InputStream in = getClass().getClassLoader().getResourceAsStream(
                "testfiles/curies-in-context.jsonld");
        final Object input = JSONUtils.fromInputStream(in);

        final ClerezzaTripleCallback callback = new ClerezzaTripleCallback();

        final MGraph graph = (MGraph) JsonLdProcessor.toRDF(input, callback);

        for (final Triple t : graph) {
            System.out.println(t);
            assertTrue("Predicate got fully expanded", t.getPredicate().getUnicodeString()
                    .startsWith("http"));
        }
        assertEquals("Graph size", 3, graph.size());

    }
}
