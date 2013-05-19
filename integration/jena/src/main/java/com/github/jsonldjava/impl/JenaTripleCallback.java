package com.github.jsonldjava.impl;

import java.util.List;
import java.util.Map;

import com.github.jsonldjava.core.JSONLDTripleCallback;
import com.github.jsonldjava.utils.Obj;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;


public class JenaTripleCallback implements JSONLDTripleCallback {

    private Model jenaModel = ModelFactory.createDefaultModel();

    public void setJenaModel(Model jenaModel) {
        this.jenaModel = jenaModel;
    }

    public Model getJenaModel() {
        return jenaModel;
    }

    private void triple(String s, String p, String o, String graph) {
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

    private void triple(String s, String p, String value, String datatype, String language, String graph) {
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

	@Override
	public Object call(Map<String, Object> dataset) {
		for (String graphName : dataset.keySet()) {
			List<Map<String,Object>> triples = (List<Map<String, Object>>) dataset.get(graphName);
			if ("@default".equals(graphName)) {
				graphName = null;
			}
			for (Map<String,Object> triple : triples) {
				if ("literal".equals(Obj.get(triple, "object", "type"))) {
					triple((String)Obj.get(triple, "subject", "value"), (String)Obj.get(triple, "predicate", "value"), 
							(String)Obj.get(triple, "object", "value"), (String)Obj.get(triple, "object", "datatype"), (String)Obj.get(triple, "object", "language"), graphName);
				} else {
					triple((String)Obj.get(triple, "subject", "value"), (String)Obj.get(triple, "predicate", "value"), (String)Obj.get(triple, "object", "value"), graphName);
				}
			}
		}

		return getJenaModel();
	}

}