package com.github.jsonldjava.core;

import static com.github.jsonldjava.core.JSONLDConsts.*;
import static com.github.jsonldjava.core.JSONLDUtils.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.jsonldjava.impl.NQuadTripleCallback;

class ToRDFUtils {
	
	private static String LITERAL = "literal";
	private static String BLANK_NODE = "blank node";
	private static String IRI = "IRI";
	
	/**
	 * Creates an array of RDF triples for the given graph.
	 *
	 * @param graph the graph to create RDF triples for.
	 * @param namer a UniqueNamer for assigning blank node names.
	 *
	 * @return the array of RDF triples for the given graph.
	 */
	static List<Object> graphToRDF(Map<String,Object> graph, UniqueNamer namer) {
		List<Object> rval = new ArrayList<Object>();
		
		for (String id : graph.keySet()) {
			Map<String,Object> node = (Map<String, Object>) graph.get(id);
			for (String property : node.keySet()) {
				Object items = node.get(property);
				if ("@type".equals(property)) {
					property = RDF_TYPE;
				} else if (isKeyword(property)) {
					continue;
				}
				
				for (Object item : (List<Object>) items) {
					// RDF subjects
					Map<String,Object> subject = new HashMap<String,Object>();
					if (id.indexOf("_:") == 0) {
						subject.put("type", BLANK_NODE);
						subject.put("value", namer.getName(id));
					} else {
						subject.put("type", IRI);
						subject.put("value", id);
					}
					
					// RDF predicates
					Map<String,Object> predicate = new HashMap<String,Object>();
					predicate.put("type", IRI);
					predicate.put("value", property);
					
					// convert @list to triples
					if (isList(item)) {
						listToRDF((List<Object>) ((Map<String, Object>) item).get("@list"), namer, subject, predicate, rval);
					}
					// convert value or node object to triple
					else {
						Object object = objectToRDF(item, namer);
						Map<String,Object> tmp = new HashMap<String, Object>();
						tmp.put("subject", subject);
						tmp.put("predicate", predicate);
						tmp.put("object", object);
						rval.add(tmp);
					}
				}
			}
		}
		
		return rval;
	}

	/**
	 * Converts a @list value into linked list of blank node RDF triples
	 * (an RDF collection).
	 *
	 * @param list the @list value.
	 * @param namer a UniqueNamer for assigning blank node names.
	 * @param subject the subject for the head of the list.
	 * @param predicate the predicate for the head of the list.
	 * @param triples the array of triples to append to.
	 */
	private static void listToRDF(List<Object> list, UniqueNamer namer,
			Map<String, Object> subject, Map<String, Object> predicate,
			List<Object> triples) {
		Map<String,Object> first = new HashMap<String, Object>();
		first.put("type", IRI);
		first.put("value", RDF_FIRST);
		Map<String,Object> rest = new HashMap<String, Object>();
		rest.put("type", IRI);
		rest.put("value", RDF_REST);
		Map<String,Object> nil = new HashMap<String, Object>();
		nil.put("type", IRI);
		nil.put("value", RDF_NIL);
		
		for (Object item : list) {
			Map<String,Object> blankNode = new HashMap<String, Object>();
			blankNode.put("type", BLANK_NODE);
			blankNode.put("value", namer.getName());
			
			{
				Map<String,Object> tmp = new HashMap<String, Object>();
				tmp.put("subject", subject);
				tmp.put("predicate", predicate);
				tmp.put("object", blankNode);
				triples.add(tmp);
			}
			
			subject = blankNode;
			predicate = first;
			Object object = objectToRDF(item, namer);
			
			{
				Map<String,Object> tmp = new HashMap<String, Object>();
				tmp.put("subject", subject);
				tmp.put("predicate", predicate);
				tmp.put("object", object);
				triples.add(tmp);
			}
			
			predicate = rest;
		}
		Map<String,Object> tmp = new HashMap<String, Object>();
		tmp.put("subject", subject);
		tmp.put("predicate", predicate);
		tmp.put("object", nil);
		triples.add(tmp);
	}
	
	/**
	 * Converts a JSON-LD value object to an RDF literal or a JSON-LD string or
	 * node object to an RDF resource.
	 *
	 * @param item the JSON-LD value or node object.
	 * @param namer the UniqueNamer to use to assign blank node names.
	 *
	 * @return the RDF literal or RDF resource.
	 */
	private static Object objectToRDF(Object item, UniqueNamer namer) {
		Map<String,Object> object = new HashMap<String, Object>();
		
		// convert value object to RDF
		if (isValue(item)) {
			object.put("type", LITERAL);
			Object value = ((Map<String, Object>) item).get("@value");
			Object datatype = ((Map<String, Object>) item).get("@type");
			
			// convert to XSD datatypes as appropriate
			if (value instanceof Boolean || value instanceof Number) {
				// convert to XSD datatype
				if (value instanceof Boolean) {
					object.put("value", value.toString());
					object.put("datatype", datatype == null ? XSD_BOOLEAN : datatype);
				} else if (value instanceof Double || value instanceof Float) {
					// canonical double representation
					DecimalFormat df = new DecimalFormat("0.0###############E0");
					object.put("value", df.format(value));
					object.put("datatype", datatype == null ? XSD_DOUBLE : datatype);
				} else {
					DecimalFormat df = new DecimalFormat("0");
					object.put("value", df.format(value));
					object.put("datatype", datatype == null ? XSD_INTEGER : datatype);
				}
			} else if (((Map<String, Object>) item).containsKey("@language")) {
				object.put("value", value);
				object.put("datatype", datatype == null ? RDF_LANGSTRING : datatype);
				object.put("language", ((Map<String, Object>) item).get("@language"));
			} else {
				object.put("value", value);
				object.put("datatype", datatype == null ? XSD_STRING : datatype);
			}
		}
		// convert string/node object to RDF
		else {
			String id = isObject(item) ? (String)((Map<String, Object>) item).get("@id") : (String)item;
			if (id.indexOf("_:") == 0) {
				object.put("type", BLANK_NODE);
				object.put("value", namer.getName(id));
			}
			else {
				object.put("type", IRI);
				object.put("value", id);
			}
		}
		
		return object;
	}

	static String toNQuads(Map<String,Object> dataset) {
		//JSONLDTripleCallback callback = new NQuadTripleCallback();
		List<String> quads = new ArrayList<String>();
		for (String graphName : dataset.keySet()) {
			List<Map<String, Object>> triples = (List<Map<String,Object>>) dataset.get(graphName);
			for (Map<String,Object> triple : triples) {
				if ("@default".equals(graphName)) {
					graphName = null;
				}
				quads.add(toNQuad(triple, graphName));
			}
		}
		Collections.sort(quads);
		String rval = "";
		for (String quad : quads) {
			rval += quad;
		}
		return rval;
	}

	static String toNQuad(Map<String, Object> triple, String graphName, String bnode) {
		Map<String,Object> s = (Map<String, Object>) triple.get("subject");
		Map<String,Object> p = (Map<String, Object>) triple.get("predicate");
		Map<String,Object> o = (Map<String, Object>) triple.get("object");
		
		String quad = "";
		
		// subject is an IRI or bnode
		if (IRI.equals(s.get("type"))) {
			quad += "<" + s.get("value") + ">";
		}
		// normalization mode
		else if (bnode != null) {
			quad += bnode.equals(s.get("value")) ? "_:a" : "_:z";
		}
		// normal mode
		else {
			quad += s.get("value");
		}
		
		// predicate is always an IRI
		quad += " <" + p.get("value") + "> ";
		
		// object is IRI, bnode or literal
		if (IRI.equals(o.get("type"))) {
			quad += "<" + o.get("value") + ">";
		}
		else if (BLANK_NODE.equals(o.get("type"))) {
			// normalization mode
			if (bnode != null) {
				quad += bnode.equals(o.get("value")) ? "_:a" : "_:z";
			}
			// normal mode
			else {
				quad += o.get("value");
			}
		}
		else {
			String escaped = ((String)o.get("value"))
					.replaceAll("\\\\", "\\\\\\\\")
					.replaceAll("\\t", "\\\\t")
					.replaceAll("\\n", "\\\\n")
					.replaceAll("\\r", "\\\\r")
					.replaceAll("\\\"", "\\\\\"");
			quad += "\"" + escaped + "\"";
			if (RDF_LANGSTRING.equals(o.get("datatype"))) {
				quad += "@" + o.get("language");
			} else if (!XSD_STRING.equals(o.get("datatype"))) {
				quad += "^^<" + o.get("datatype") + ">";
			}
		}
		
		// graph
		if (graphName != null) {
			if (graphName.indexOf("_:") != 0) {
				quad += " <" + graphName + ">";
			}
			else if (bnode != null) {
				quad += " _:g";
			}
			else {
				quad += " " + graphName;
			}
		}
		
		quad += " .\n";
		return quad;
	}
	
	private static String toNQuad(Map<String, Object> triple, String graphName) {
		return toNQuad(triple, graphName, null);
	}
}
