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

	public Object triple(String subject, String property, String objectType,
			String object, String datatype, String language) {
		
		if (subject == null || property == null || object == null) {
			// TODO: i don't know what to do here!!!!
			return null;
		}
		
		Resource s = jenaModel.getResource(subject);
		if (s == null) {
			s = jenaModel.createResource(subject);
		}
		Property p = jenaModel.getProperty(property);
		if (p == null) {
			p = jenaModel.createProperty(property);
		}
		RDFNode o;
		
		if ("literal".equals(objectType)) {
			if (datatype != null) {
				o = jenaModel.createTypedLiteral(object, datatype);
			} else if (language != null) {
				o = jenaModel.createLiteral(object, language);
			} else {
				o = jenaModel.createLiteral(object);
			}
		} else {
			// resource
			o = jenaModel.getResource(object);
			if (o == null) {
				o = jenaModel.createResource(object);
			}
		}
		
		Statement statement = jenaModel.createStatement(s, p, o);
		jenaModel.add(statement);
		return statement;

	}

	public void setJenaModel(Model jenaModel) {
		this.jenaModel = jenaModel;
	}

	public Model getJenaModel() {
		return jenaModel;
	}

	public Object triple(Object s, Object p, Object o) {
		// TODO: fix this!
		return null;
	}

}
