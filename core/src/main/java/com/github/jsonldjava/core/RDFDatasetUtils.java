package com.github.jsonldjava.core;

import static com.github.jsonldjava.core.JSONLDConsts.*;
import static com.github.jsonldjava.core.JSONLDUtils.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.jsonldjava.utils.JSONUtils;

public class RDFDatasetUtils {
	
	/**
	 * Creates an array of RDF triples for the given graph.
	 *
	 * @param graph the graph to create RDF triples for.
	 * @param namer a UniqueNamer for assigning blank node names.
	 *
	 * @return the array of RDF triples for the given graph.
	 */
	@Deprecated // use RDFDataset.graphToRDF
	static List<Object> graphToRDF(Map<String,Object> graph, UniqueNamer namer) {
		List<Object> rval = new ArrayList<Object>();
		for (String id : graph.keySet()) {
			Map<String,Object> node = (Map<String, Object>) graph.get(id);
			List<String> properties = new ArrayList<String>(node.keySet());
			Collections.sort(properties);
			for (String property : properties) {
				Object items = node.get(property);
				if ("@type".equals(property)) {
					property = RDF_TYPE;
				} else if (isKeyword(property)) {
					continue;
				}
				
				for (Object item : (List<Object>) items) {
					// RDF subjects
					Map<String,Object> subject = new LinkedHashMap<String,Object>();
					if (id.indexOf("_:") == 0) {
						subject.put("type", "blank node");
						subject.put("value", namer.getName(id));
					} else {
						subject.put("type", "IRI");
						subject.put("value", id);
					}
					
					// RDF predicates
					Map<String,Object> predicate = new LinkedHashMap<String,Object>();
					predicate.put("type", "IRI");
					predicate.put("value", property);
					
					// convert @list to triples
					if (isList(item)) {
						listToRDF((List<Object>) ((Map<String, Object>) item).get("@list"), namer, subject, predicate, rval);
					}
					// convert value or node object to triple
					else {
						Object object = objectToRDF(item, namer);
						Map<String,Object> tmp = new LinkedHashMap<String, Object>();
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
		Map<String,Object> first = new LinkedHashMap<String, Object>();
		first.put("type", "IRI");
		first.put("value", RDF_FIRST);
		Map<String,Object> rest = new LinkedHashMap<String, Object>();
		rest.put("type", "IRI");
		rest.put("value", RDF_REST);
		Map<String,Object> nil = new LinkedHashMap<String, Object>();
		nil.put("type", "IRI");
		nil.put("value", RDF_NIL);
		
		for (Object item : list) {
			Map<String,Object> blankNode = new LinkedHashMap<String, Object>();
			blankNode.put("type", "blank node");
			blankNode.put("value", namer.getName());
			
			{
				Map<String,Object> tmp = new LinkedHashMap<String, Object>();
				tmp.put("subject", subject);
				tmp.put("predicate", predicate);
				tmp.put("object", blankNode);
				triples.add(tmp);
			}
			
			subject = blankNode;
			predicate = first;
			Object object = objectToRDF(item, namer);
			
			{
				Map<String,Object> tmp = new LinkedHashMap<String, Object>();
				tmp.put("subject", subject);
				tmp.put("predicate", predicate);
				tmp.put("object", object);
				triples.add(tmp);
			}
			
			predicate = rest;
		}
		Map<String,Object> tmp = new LinkedHashMap<String, Object>();
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
		Map<String,Object> object = new LinkedHashMap<String, Object>();
		
		// convert value object to RDF
		if (isValue(item)) {
			object.put("type", "literal");
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
				object.put("type", "blank node");
				object.put("value", namer.getName(id));
			}
			else {
				object.put("type", "IRI");
				object.put("value", id);
			}
		}
		
		return object;
	}

	public static String toNQuads(RDFDataset dataset) {
		List<String> quads = new ArrayList<String>();
		for (String graphName : dataset.graphNames()) {
			List<RDFDataset.Quad> triples = dataset.getQuads(graphName);
			if ("@default".equals(graphName)) {
				graphName = null;
			}
			for (RDFDataset.Quad triple : triples) {
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

	static String toNQuad(RDFDataset.Quad triple, String graphName, String bnode) {
		RDFDataset.Node s = triple.getSubject();
		RDFDataset.Node p = triple.getPredicate();
		RDFDataset.Node o = triple.getObject();
		
		String quad = "";
		
		// subject is an IRI or bnode
		if (s.isIRI()) {
			quad += "<" + s.getValue() + ">";
		}
		// normalization mode
		else if (bnode != null) {
			quad += bnode.equals(s.getValue()) ? "_:a" : "_:z";
		}
		// normal mode
		else {
			quad += s.getValue();
		}
		
		// predicate is always an IRI
		quad += " <" + p.getValue() + "> ";
		
		// object is IRI, bnode or literal
		if (o.isIRI()) {
			quad += "<" + o.getValue() + ">";
		}
		else if (o.isBlankNode()) {
			// normalization mode
			if (bnode != null) {
				quad += bnode.equals(o.getValue()) ? "_:a" : "_:z";
			}
			// normal mode
			else {
				quad += o.getValue();
			}
		}
		else {
			String escaped = o.getValue()
					.replaceAll("\\\\", "\\\\\\\\")
					.replaceAll("\\t", "\\\\t")
					.replaceAll("\\n", "\\\\n")
					.replaceAll("\\r", "\\\\r")
					.replaceAll("\\\"", "\\\\\"");
			quad += "\"" + escaped + "\"";
			if (RDF_LANGSTRING.equals(o.getDatatype())) {
				quad += "@" + o.getLanguage();
			} else if (!XSD_STRING.equals(o.getDatatype())) {
				quad += "^^<" + o.getDatatype() + ">";
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
	
	static String toNQuad(RDFDataset.Quad triple, String graphName) {
		return toNQuad(triple, graphName, null);
	}
	
	public static class Regex {
		// define partial regexes
		final public static Pattern IRI = Pattern.compile("(?:<([^:]+:[^>]*)>)");
		final public static Pattern BNODE = Pattern.compile("(_:(?:[A-Za-z][A-Za-z0-9]*))");
		final public static Pattern PLAIN = Pattern.compile("\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"");
		final public static Pattern DATATYPE = Pattern.compile("(?:\\^\\^" + IRI + ")");
		final public static Pattern LANGUAGE = Pattern.compile("(?:@([a-z]+(?:-[a-z0-9]+)*))");
		final public static Pattern LITERAL = Pattern.compile("(?:" + PLAIN + "(?:" + DATATYPE + "|" + LANGUAGE + ")?)");
		final public static Pattern WS = Pattern.compile("[ \\t]+");
		final public static Pattern WSO = Pattern.compile("[ \\t]*");
		final public static Pattern EOLN = Pattern.compile("(?:\r\n)|(?:\n)|(?:\r)");
		final public static Pattern EMPTY = Pattern.compile("^" + WSO + "$");
		
		// define quad part regexes
		final public static Pattern SUBJECT = Pattern.compile("(?:" + IRI + "|" + BNODE + ")" + WS);
		final public static Pattern PROPERTY = Pattern.compile(IRI.pattern() + WS.pattern());
		final public static Pattern OBJECT = Pattern.compile("(?:" + IRI + "|" + BNODE + "|" + LITERAL + ")" + WSO);
		final public static Pattern GRAPH = Pattern.compile("(?:\\.|(?:(?:" + IRI + "|" + BNODE + ")" + WSO + "\\.))");
		
		// full quad regex
		final public static Pattern QUAD = Pattern.compile("^" + WSO + SUBJECT + PROPERTY + OBJECT + GRAPH + WSO + "$");
		
		// turtle prefix line
		final public static Pattern TTL_PREFIX_NS = Pattern.compile("(?:([a-zA-Z0-9\\.]*):)"); // TODO: chars can be more
		final public static Pattern TTL_PREFIX_ID = Pattern.compile("^@prefix" + WS + TTL_PREFIX_NS + WS + IRI + WSO + "\\." + WSO + "$");
		
		final public static Pattern IWSO = Pattern.compile("^" + WSO);
		final public static Pattern TTL_SUBJECT = Pattern.compile("^(?:" + TTL_PREFIX_NS + "([^ \\t]+)|" + BNODE + "|" + IRI + ")" + WS);
		final public static Pattern TTL_PREDICATE = Pattern.compile("^(?:" + TTL_PREFIX_NS + "([^ \\t]+)|" + IRI + ")"  + WS);
		final public static Pattern TTL_DATATYPE = Pattern.compile("(?:\\^\\^" + TTL_PREFIX_NS + "([^ \\t]+)|" + IRI + ")");
		final public static Pattern TTL_LITERAL = Pattern.compile("(?:" + PLAIN + "(?:" + TTL_DATATYPE + "|" + LANGUAGE + ")?)");
		final public static Pattern TTL_OBJECT = Pattern.compile("^(?:" + TTL_PREFIX_NS + "([^,; \\t]+)([,;\\.]?)|" + IRI + "|" + BNODE + "|" + TTL_LITERAL + ")" + WSO);
	}
	
	/**
	 * Parses RDF in the form of N-Quads.
	 *
	 * @param input the N-Quads input to parse.
	 *
	 * @return an RDF dataset.
	 */
	public static RDFDataset parseNQuads(String input) throws JSONLDProcessingError {
		// build RDF dataset
		RDFDataset dataset = new RDFDataset();
		
		// split N-Quad input into lines
		String[] lines = Regex.EOLN.split(input);
		int lineNumber = 0;
		for (String line : lines) {
			lineNumber++;
			
			// skip empty lines
			if (Regex.EMPTY.matcher(line).matches()) {
				continue;
			}
			
			// parse quad
			Matcher match = Regex.QUAD.matcher(line);
			if (!match.matches()) {
				throw new JSONLDProcessingError("Error while parsing N-Quads; invalid quad.")
					.setType(JSONLDProcessingError.Error.PARSE_ERROR)
					.setDetail("line", lineNumber);
			}
			
			// get subject
			RDFDataset.Node subject;
			if (match.group(1) != null) {
				subject = new RDFDataset.IRI(match.group(1));
			} else {
				subject = new RDFDataset.BlankNode(match.group(2));
			}
			
			// get predicate
			RDFDataset.Node predicate = new RDFDataset.IRI(match.group(3));
			
			// get object
			RDFDataset.Node object;
			if (match.group(4) != null) {
				object = new RDFDataset.IRI(match.group(4));
			} else if (match.group(5) != null) {
				object = new RDFDataset.BlankNode(match.group(5));
			} else {
				final String language = match.group(8);
				final String datatype = match.group(7) != null ? match.group(7) : match.group(8) != null ? RDF_LANGSTRING : XSD_STRING;
				final String unescaped = match.group(6)
						.replaceAll("\\\\\\\\", "\\\\")
						.replaceAll("\\\\t", "\\t")
						.replaceAll("\\\\n", "\\n")
						.replaceAll("\\\\r", "\\r")
						.replaceAll("\\\\\"", "\\\"");
				object = new RDFDataset.Literal(unescaped, datatype, language);
			}
			
			// get graph name ('@default' is used for the default graph)
			String name = "@default";
			if (match.group(9) != null) {
				name = match.group(9);
			} else if (match.group(10) != null) {
				name = match.group(10);
			}
			
			RDFDataset.Quad triple = new RDFDataset.Quad(subject, predicate, object, name);

			// initialise graph in dataset
			if (!dataset.containsKey(name)) {
				List<RDFDataset.Quad> tmp = new ArrayList<RDFDataset.Quad>();
				tmp.add(triple);
				dataset.put(name, tmp);
			}
			// add triple if unique to its graph
			else {
				List<RDFDataset.Quad> triples = (List<RDFDataset.Quad>) dataset.get(name);
				if (!triples.contains(triple)) {
					triples.add(triple);
				}
			}
		}
		
		return dataset;
	}
}
