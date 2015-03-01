package com.github.jsonldjava.rdf2go;

import java.util.List;

import org.ontoware.rdf2go.RDF2Go;
import org.ontoware.rdf2go.model.ModelSet;
import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.Resource;
import org.ontoware.rdf2go.model.node.URI;

import com.github.jsonldjava.core.JsonLdTripleCallback;
import com.github.jsonldjava.core.RDFDataset;

/**
 * Implementation of {@link JsonLdTripleCallback} which serializes JSONLD
 * datasets into a {@link ModelSet} object.
 *
 * @author Ismael Rivera
 */
public class RDF2GoTripleCallback implements JsonLdTripleCallback {

    private final ModelSet sinkModel;

    public RDF2GoTripleCallback() {
        this.sinkModel = RDF2Go.getModelFactory().createModelSet();
        this.sinkModel.open();
    }

    private void triple(String s, String p, String o, String graph) {
        triple(sinkModel.createURI(s), sinkModel.createURI(p), sinkModel.createURI(o), graph);
    }

    private void triple(String s, String p, String value, String datatype, String language,
            String graph) {
        Node object = null;
        if (language != null) {
            object = sinkModel.createLanguageTagLiteral(value, language);
        } else if (datatype != null) {
            object = sinkModel.createDatatypeLiteral(value, sinkModel.createURI(datatype));
        } else {
            object = sinkModel.createPlainLiteral(value);
        }

        triple(sinkModel.createURI(s), sinkModel.createURI(p), object, graph);
    }

    private void triple(Resource subject, URI predicate, Node object, String graph) {
        final URI context = graph == null ? null : sinkModel.createURI(graph);
        sinkModel.addStatement(context, subject, predicate, object);
    }

    @Override
    public Object call(RDFDataset dataset) {
        for (String graphName : dataset.keySet()) {
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

        return sinkModel;
    }

}
