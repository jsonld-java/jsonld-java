package de.dfki.km.json.jsonld.impl;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

import de.dfki.km.json.jsonld.JSONLDTripleCallback;

public class JenaTripleCallback implements JSONLDTripleCallback {

    private Model jenaModel = ModelFactory.createDefaultModel();

    public void setJenaModel(Model jenaModel) {
        this.jenaModel = jenaModel;
    }

    public Model getJenaModel() {
        return jenaModel;
    }

    public void triple(String s, String p, String o) {
        if (s == null || p == null || o == null) {
            // TODO: i don't know what to do here!!!!
            return;
        }

        Resource sR = jenaModel.getResource(s);
        if (sR == null) {
            sR = jenaModel.createResource(s);
        }
        Property pR = jenaModel.getProperty(p);
        if (pR == null) {
            pR = jenaModel.createProperty(p);
        }

        RDFNode oR = jenaModel.getResource(o);
        if (oR == null) {
            oR = jenaModel.createResource(o);
        }

        Statement statement = jenaModel.createStatement(sR, pR, oR);
        jenaModel.add(statement);
    }

    @Override
    public void triple(String s, String p, String value, String datatype, String language) {
        // TODO Auto-generated method stub

        Resource sR = jenaModel.getResource(s);
        if (sR == null) {
            sR = jenaModel.createResource(s);
        }
        Property pR = jenaModel.getProperty(p);
        if (pR == null) {
            pR = jenaModel.createProperty(p);
        }

        RDFNode oR;
        if (language != null) {
            oR = jenaModel.createLiteral(value, language);
        } else {
            oR = jenaModel.createTypedLiteral(value, datatype);
        }

        Statement statement = jenaModel.createStatement(sR, pR, oR);
        jenaModel.add(statement);
    }

}
