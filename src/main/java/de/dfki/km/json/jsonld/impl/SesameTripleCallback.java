package de.dfki.km.json.jsonld.impl;

import org.openrdf.model.Graph;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.GraphImpl;

import de.dfki.km.json.jsonld.JSONLDTripleCallback;

public class SesameTripleCallback extends JSONLDTripleCallback {

    private ValueFactory vf;

    private Graph storageGraph;

    public SesameTripleCallback() {
        this(new GraphImpl());
    }

    public SesameTripleCallback(Graph nextGraph) {
        setStorageGraph(nextGraph);
    }

    @Override
    public void triple(String s, String p, String o, String graph) {
        if (s == null || p == null || o == null) {
            // TODO: i don't know what to do here!!!!
            return;
        }

        // This method is always called with three URIs as subject predicate and
        // object
        Statement result = vf.createStatement(vf.createURI(s), vf.createURI(p), vf.createURI(o));
        storageGraph.add(result);
    }

    @Override
    public void triple(String s, String p, String value, String datatype, String language, String graph) {

        if (s == null || p == null || value == null) {
            // TODO: i don't know what to do here!!!!
            return;
        }

        URI subject = vf.createURI(s);

        URI predicate = vf.createURI(p);

        Value object;
        if (language != null) {
            object = vf.createLiteral(value, language);
        } else if (datatype != null) {
            object = vf.createLiteral(value, vf.createURI(datatype));
        } else {
            object = vf.createLiteral(value);
        }

        Statement result = vf.createStatement(subject, predicate, object);
        storageGraph.add(result);
    }

    /**
     * @return the storageGraph
     */
    public Graph getStorageGraph() {
        return storageGraph;
    }

    /**
     * @param storageGraph
     *            the storageGraph to set
     */
    public void setStorageGraph(Graph storageGraph) {
        this.storageGraph = storageGraph;

        if (storageGraph != null) {
            vf = storageGraph.getValueFactory();
        }
    }

}
