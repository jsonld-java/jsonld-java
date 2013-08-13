package com.github.jsonldjava.impl;

import java.util.List;

import com.github.jsonldjava.core.JSONLDTripleCallback;
import com.github.jsonldjava.core.RDFDataset;
import com.github.jsonldjava.core.RDFDataset.Node;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.shared.InvalidPropertyURIException;

public class JenaTripleCallback implements JSONLDTripleCallback {

    private Model jenaModel = ModelFactory.createDefaultModel();

    public void setJenaModel(Model jenaModel) {
        this.jenaModel = jenaModel;
    }

    public Model getJenaModel() {
        return jenaModel;
    }

    private void triple(Node subjectNode, Node propertyNode, Node objectNode, String graph) {
        if (subjectNode == null || propertyNode == null || objectNode == null) {
            // TODO: i don't know what to do here!!!!
            return;
        }
        
        Resource subject = createResourceFromNode(subjectNode);
        if (! propertyNode.isIRI()) {
            throw new InvalidPropertyURIException(propertyNode.getValue());
        }
        Property property = jenaModel.createProperty(propertyNode.getValue());
        Resource object = createResourceFromNode(subjectNode);

        final Statement statement = jenaModel.createStatement(subject, property, object);
        jenaModel.add(statement);
    }

    private void triple(Node subjectNode, Node propertyNode, String value, String datatype, String language,
            String graph) {

        Resource subject = createResourceFromNode(subjectNode);
        if (! propertyNode.isIRI()) {
            throw new InvalidPropertyURIException(propertyNode.getValue());
        }
        Property property = jenaModel.createProperty(propertyNode.getValue());

        RDFNode object;
        if (language != null) {
            object = jenaModel.createLiteral(value, language);
        } else {
            object = jenaModel.createTypedLiteral(value, datatype);
        }

        final Statement statement = jenaModel.createStatement(subject, property, object);
        jenaModel.add(statement);
    }

    private Resource createResourceFromNode(Node subjectNode) {
        Resource sR;
        if (subjectNode.isIRI()) {
            sR = jenaModel.createResource(subjectNode.getValue());
        } else {
            sR = jenaModel.createResource(new AnonId(subjectNode.getValue()));
        }
        return sR;
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
                    triple(quad.getSubject(), quad.getPredicate(), quad
                            .getObject().getValue(), quad.getObject().getDatatype(), quad
                            .getObject().getLanguage(), graphName);
                } else {
                    triple(quad.getSubject(), quad.getPredicate(), quad
                            .getObject(), graphName);
                }
            }
        }

        return getJenaModel();
    }

}
