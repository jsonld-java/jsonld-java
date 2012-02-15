package de.dfki.km.json.jsonld.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.datatypes.DatatypeFormatException;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import de.dfki.km.json.jsonld.JSONLDConsts;

public class JSONLDSerializer implements
		de.dfki.km.json.jsonld.JSONLDSerializer {

	private static final Logger LOG = LoggerFactory.getLogger( JSONLDSerializer.class ); 
	
	public Object fromJenaModel(Model model, Resource[] subjects) {
		// TODO: this function is really broken (out of date), FIXME !!!
		
		List<Object> rval = new ArrayList<Object>();
		
		for (Resource subject: subjects) {
			Map<String,Object> jsonObject = new HashMap<String, Object>();
			
			// map to assign to @context in the end
			Map<String,Object> context = new HashMap<String, Object>();
			
			String subjectURI = subject.getURI();
			// add @subject to the json object
			jsonObject.put("@id", subjectURI);
			
			StmtIterator statements = model.listStatements(subject, (Property)null, (RDFNode)null);
			while (statements.hasNext()) {
				Statement statement = statements.next();
				Property predicate = statement.getPredicate();
				RDFNode object = statement.getObject();
				
				String localName = predicate.getLocalName();
	
				if ("type".equals(localName) && JSONLDConsts.RDF_SYNTAX_NS.equals(predicate.getNameSpace())) {
					jsonObject.put("@type", object.toString());
				} else {
					List<Object> values = (List<Object>) jsonObject.get(localName);
					if (values == null) {
						values = new ArrayList<Object>();
						// add to @context
						context.put(localName, predicate.getURI()); // TODO: bnode
					}
					if (object.isLiteral()) {
						Literal literal = object.asLiteral();
						Object o = null;
						try {
							o = literal.getValue();
							// trying to work around DatatypeFormatException on some dates
						} catch (DatatypeFormatException e) {
							
						}
						Object value;
						if (o != null) {
							value = o;
						} else {
							value = literal.getLexicalForm();
						}
						if (value instanceof String) {
							values.add(value);
						} else {
							LOG.debug("got a value with class:" + value.getClass() + ". toString gives: " + value.toString());
							values.add(value.toString());
						}
						 
						String datatypeURI = object.asLiteral().getDatatypeURI();
						if (datatypeURI == null) {
							datatypeURI = JSONLDConsts.XSD_NS + "string";
						}
						
						if (datatypeURI.startsWith(JSONLDConsts.XSD_NS)) {
							datatypeURI = datatypeURI.replace(JSONLDConsts.XSD_NS, "xsd:");
							if (!context.containsKey("xsd")) {
								context.put("xsd", JSONLDConsts.XSD_NS);
							}
						}
						Map<String,Object> coerce = new HashMap<String, Object>();
						coerce.put("@type", datatypeURI);
						context.put(localName, coerce);
					
					} else {
						values.add(object.toString());
					}
					jsonObject.put(localName, values);
				}
			}
			
			// go through the keys and change any lists with one value into a single value
			for (String key: jsonObject.keySet()) {
				if (!key.startsWith("@")) {
					Object vals = jsonObject.get(key);
					if (vals instanceof List && ((List<Object>) vals).size() == 1) {
						jsonObject.put(key, ((List<Object>) vals).get(0));
					}
				}
			}
			
			jsonObject.put("@context", context);
			rval.add(jsonObject);
		}
		
		if (rval.size() == 1) {
			return rval.get(0);
		} else {
			return rval;
		}
	}

	public Object fromResource(Resource r) {
		
		Model model = r.getModel();
		if (model == null) {
			return null;
		}
		return fromJenaModel(model, new Resource[]{ r });
	}
}
