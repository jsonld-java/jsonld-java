package de.dfki.km.json.jsonld.impl;

import java.util.Map;

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

	public Object triple(Object s, Object p, Object o) {
		if (s == null || p == null || o == null) {
			// TODO: i don't know what to do here!!!!
			return null;
		}
		
		if (!(s instanceof String && p instanceof String)) {
			// TODO: I assumed the subject and predicate should always be a string IRI is this correct?
			return null;
		}
		
		Resource sR = jenaModel.getResource((String) s);
		if (sR == null) {
			sR = jenaModel.createResource((String)s);
		}
		Property pR = jenaModel.getProperty((String) p);
		if (pR == null) {
			pR = jenaModel.createProperty((String)p);
		}
		
		RDFNode oR;
		if (o instanceof String) {
			// basic literal
			oR = jenaModel.createLiteral((String)o);
		} else if (o instanceof Map) { // should be the only other option
			if (((Map) o).containsKey("@literal")) {
				if (((Map) o).containsKey("@datatype")) {
					oR = jenaModel.createTypedLiteral(
							((Map) o).get("@literal"),
							(String)((Map) o).get("@datatype"));
				} else if (((Map) o).containsKey("@language")){
					oR = jenaModel.createLiteral(
							(String) ((Map) o).get("@literal"),
							(String) ((Map) o).get("@language")
							);
				} else {
					oR = jenaModel.createLiteral((String) ((Map) o).get("@literal"));
				}
			} else if (((Map) o).containsKey("@iri")) {
				oR = jenaModel.getResource((String)((Map) o).get("@iri"));
				if (oR == null) {
					oR = jenaModel.createResource((String)((Map) o).get("@iri"));
				}
			} else {
				// TODO: have i missed anything?
				return null;
			}
		} else {
			// TODO: I thought i'd covered all the bases!
			return null;
		}
		Statement statement = jenaModel.createStatement(sR, pR, oR);
		jenaModel.add(statement);
		return statement;
	}

}
