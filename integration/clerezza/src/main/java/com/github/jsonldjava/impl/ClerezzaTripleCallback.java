package com.github.jsonldjava.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.impl.TypedLiteralImpl;

import com.github.jsonldjava.core.JSONLDTripleCallback;
import com.github.jsonldjava.core.RDFDataset;

public class ClerezzaTripleCallback implements JSONLDTripleCallback {

    private MGraph mGraph = new SimpleMGraph();
    private Map<String, BNode> bNodeMap = new HashMap<String, BNode>();

    public void setMGraph(MGraph mGraph) {
        this.mGraph = mGraph;
        bNodeMap = new HashMap<String, BNode>();
    }

    public MGraph getMGraph() {
        return mGraph;
    }

    private void triple(String s, String p, String o, String graph) {
        if (s == null || p == null || o == null) {
            // TODO: i don't know what to do here!!!!
            return;
        }

        final NonLiteral subject = getNonLiteral(s);
        final UriRef predicate = new UriRef(p);
        final NonLiteral object = getNonLiteral(o);
        mGraph.add(new TripleImpl(subject, predicate, object));
    }

    private void triple(String s, String p, String value, String datatype, String language,
            String graph) {
        final NonLiteral subject = getNonLiteral(s);
        final UriRef predicate = new UriRef(p);
        Resource object;
        if (language != null) {
            object = new PlainLiteralImpl(value, new Language(language));
        } else {
            if (datatype != null) {
                object = new TypedLiteralImpl(value, new UriRef(datatype));
            } else {
                object = new PlainLiteralImpl(value);
            }
        }

        mGraph.add(new TripleImpl(subject, predicate, object));
    }

    private NonLiteral getNonLiteral(String s) {
        if (s.startsWith("_:")) {
            return getBNode(s);
        } else {
            return new UriRef(s);
        }
    }

    private BNode getBNode(String s) {
        if (bNodeMap.containsKey(s)) {
            return bNodeMap.get(s);
        } else {
            final BNode result = new BNode();
            bNodeMap.put(s, result);
            return result;
        }
    }

    @Override
    public Object call(RDFDataset dataset) {
        for (String graphName : dataset.graphNames()) {
            final List<RDFDataset.Quad> quads = dataset.getQuads(graphName);
            if ("@default".equals(graphName)) {
                graphName = null;
            }
            for (final RDFDataset.Quad quad : quads) {
                if (quad.getObject().isLiteral()) {
                    triple(quad.getSubject().getValue(), quad.getPredicate().getValue(), quad
                            .getObject().getValue(), quad.getObject().getDatatype(), quad
                            .getObject().getLanguage(), graphName);
                } else {
                    triple(quad.getSubject().getValue(), quad.getPredicate().getValue(), quad
                            .getObject().getValue(), graphName);
                }
            }
        }

        return getMGraph();
    }

}
