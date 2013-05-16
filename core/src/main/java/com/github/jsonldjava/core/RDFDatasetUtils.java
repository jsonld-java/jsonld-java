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

public class RDFDatasetUtils {
	
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
						subject.put("type", BLANK_NODE);
						subject.put("value", namer.getName(id));
					} else {
						subject.put("type", IRI);
						subject.put("value", id);
					}
					
					// RDF predicates
					Map<String,Object> predicate = new LinkedHashMap<String,Object>();
					predicate.put("type", IRI);
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
		first.put("type", IRI);
		first.put("value", RDF_FIRST);
		Map<String,Object> rest = new LinkedHashMap<String, Object>();
		rest.put("type", IRI);
		rest.put("value", RDF_REST);
		Map<String,Object> nil = new LinkedHashMap<String, Object>();
		nil.put("type", IRI);
		nil.put("value", RDF_NIL);
		
		for (Object item : list) {
			Map<String,Object> blankNode = new LinkedHashMap<String, Object>();
			blankNode.put("type", BLANK_NODE);
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
	
	/**
	 * Converts an RDF triple object to a JSON-LD object.
	 *
	 * @param o the RDF triple object to convert.
	 * @param useNativeTypes true to output native types, false not to.
	 *
	 * @return the JSON-LD object.
	 */
	static Map<String,Object> RDFToObject(final Map<String, Object> value, Boolean useNativeTypes) {
		// If value is an an IRI or a blank node identifier, return a new JSON object consisting 
		// of a single member @id whose value is set to value.
		if ("IRI".equals(value.get("type")) || "blank node".equals(value.get("type"))) {
			return new LinkedHashMap<String, Object>() {{
				put("@id", value.get("value"));
			}};
		};
		
		// convert literal object to JSON-LD
		Map<String,Object> rval = new LinkedHashMap<String, Object>() {{
			put("@value", value.get("value"));
		}};
		
		// add language
		if (value.containsKey("language")) {
			rval.put("@language", value.get("language"));
		}
		// add datatype
		else {
			String type;
			if (value.containsKey("datatype")) {
				type = (String) value.get("datatype");
			} else {
				// default datatype to string in the case that it hasn't been set
				type = XSD_STRING;
			}
			if (useNativeTypes) {
				// use native datatypes for certain xsd types
				if (XSD_BOOLEAN.equals(type)) {
					if ("true".equals(rval.get("@value"))) {
						rval.put("@value", Boolean.TRUE);
					} else if ("false".equals(rval.get("@value"))) {
						rval.put("@value", Boolean.FALSE);
					}
				} else if (Pattern.matches("^[+-]?[0-9]+((?:\\.?[0-9]+((?:E?[+-]?[0-9]+)|)|))$", (String)rval.get("@value"))){
					try {
						Double d = Double.parseDouble((String)rval.get("@value"));
						if (!Double.isNaN(d) && !Double.isInfinite(d)) {
							if (XSD_INTEGER.equals(type)) {
								Integer i = d.intValue();
								if (i.toString().equals(rval.get("@value"))) {
									rval.put("@value", i);
								}
							} else if (XSD_DOUBLE.equals(type)) {
								rval.put("@value", d);
							} else {
								// we don't know the type, so we should add it to the JSON-LD
								rval.put("@type", type);
							}
						}
					} catch (NumberFormatException e) {
						// TODO: This should never happen since we match the value with regex!
						throw new RuntimeException(e);
					}
				}
				// do not add xsd:string type
				else if (!XSD_STRING.equals(type)) {
					rval.put("@type", type);
				}
			} else {
				rval.put("@type", type);
			}
		}
		
		return rval;
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
	
	static String toNQuad(Map<String, Object> triple, String graphName) {
		return toNQuad(triple, graphName, null);
	}
	
	// define partial regexes
	final private static Pattern re_iri = Pattern.compile("(?:<([^:]+:[^>]*)>)");
	final private static Pattern re_bnode = Pattern.compile("(_:(?:[A-Za-z][A-Za-z0-9]*))");
	final private static Pattern re_plain = Pattern.compile("\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"");
	final private static Pattern re_datatype = Pattern.compile("(?:\\^\\^" + re_iri + ")");
	final private static Pattern re_language = Pattern.compile("(?:@([a-z]+(?:-[a-z0-9]+)*))");
	final private static Pattern re_literal = Pattern.compile("(?:" + re_plain + "(?:" + re_datatype + "|" + re_language + ")?)");
	final private static Pattern re_ws = Pattern.compile("[ \\t]+");
	final private static Pattern re_wso = Pattern.compile("[ \\t]*");
	final private static Pattern re_eoln = Pattern.compile("(?:\r\n)|(?:\n)|(?:\r)");
	final private static Pattern re_empty = Pattern.compile("^" + re_wso + "$");
	
	// define quad part regexes
	final private static Pattern re_subject = Pattern.compile("(?:" + re_iri + "|" + re_bnode + ")" + re_ws);
	final private static Pattern re_property = Pattern.compile(re_iri.pattern() + re_ws.pattern());
	final private static Pattern re_object = Pattern.compile("(?:" + re_iri + "|" + re_bnode + "|" + re_literal + ")" + re_wso);
	final private static Pattern re_graph = Pattern.compile("(?:\\.|(?:(?:" + re_iri + "|" + re_bnode + ")" + re_wso + "\\.))");
	
	// full quad regex
	final private static Pattern re_quad = Pattern.compile("^" + re_wso + re_subject + re_property + re_object + re_graph + re_wso + "$");
	
	/**
	 * Parses RDF in the form of N-Quads.
	 *
	 * @param input the N-Quads input to parse.
	 *
	 * @return an RDF dataset.
	 */
	public static Map<String,Object> parseNQuads(String input) throws JSONLDProcessingError {
		// build RDF dataset
		Map<String,Object> dataset = new LinkedHashMap<String, Object>();
		
		// split N-Quad input into lines
		String[] lines = re_eoln.split(input);
		int lineNumber = 0;
		for (String line : lines) {
			lineNumber++;
			
			// skip empty lines
			if (re_empty.matcher(line).matches()) {
				continue;
			}
			
			// parse quad
			Matcher match = re_quad.matcher(line);
			if (!match.matches()) {
				throw new JSONLDProcessingError("Error while parsing N-Quads; invalid quad.")
					.setType(JSONLDProcessingError.Error.PARSE_ERROR)
					.setDetail("line", lineNumber);
			}
			
			// create RDF triple
			Map<String,Object> triple = new LinkedHashMap<String, Object>();
			
			// get subject
			if (match.group(1) != null) {
				final String value = match.group(1);
				triple.put("subject", new LinkedHashMap<String, Object>() {{
					put("type", "IRI");
					put("value", value);
				}});
			} else {
				final String value = match.group(2);
				triple.put("subject", new LinkedHashMap<String, Object>() {{
					put("type", "blank node");
					put("value", value);
				}});
			}
			
			// get predicate
			final String predval = match.group(3);
			triple.put("predicate", new LinkedHashMap<String, Object>() {{
				put("type", "IRI");
				put("value", predval);
			}});
			
			// get object
			if (match.group(4) != null) {
				final String value = match.group(4);
				triple.put("object", new LinkedHashMap<String, Object>() {{
					put("type", "IRI");
					put("value", value);
				}});
			} else if (match.group(5) != null) {
				final String value = match.group(5);
				triple.put("object", new LinkedHashMap<String, Object>() {{
					put("type", "blank node");
					put("value", value);
				}});
			} else {
				final String language = match.group(8);
				final String datatype = match.group(7) != null ? match.group(7) : match.group(8) != null ? RDF_LANGSTRING : XSD_STRING;
				final String unescaped = match.group(6)
						.replaceAll("\\\\\\\\", "\\\\")
						.replaceAll("\\\\t", "\\t")
						.replaceAll("\\\\n", "\\n")
						.replaceAll("\\\\r", "\\r")
						.replaceAll("\\\\\"", "\\\"");
				triple.put("object", new LinkedHashMap<String, Object>() {{
					put("type", "literal");
					put("datatype", datatype);
					if (language != null) {
						put("language", language);
					}
					put("value", unescaped);
				}});
			}
			
			// get graph name ('@default' is used for the default graph)
			String name = "@default";
			if (match.group(9) != null) {
				name = match.group(9);
			} else if (match.group(10) != null) {
				name = match.group(10);
			}
			
			// initialise graph in dataset
			if (!dataset.containsKey(name)) {
				List<Object> tmp = new ArrayList<Object>();
				tmp.add(triple);
				dataset.put(name, tmp);
			}
			// add triple if unique to its graph
			else {
				Boolean unique = true;
				List<Map<String,Object>> triples = (List<Map<String, Object>>) dataset.get(name);
				for (int ti = 0; unique && ti < triples.size(); ++ti) {
					if (compareRDFTriples(triples.get(ti), triple)) {
						unique = false;
					}
				}
				if (unique) {
					triples.add(triple);
				}
			}
		}
		
		return dataset;
	}

	/**
	 * Compares two RDF triples for equality.
	 *
	 * @param t1 the first triple.
	 * @param t2 the second triple.
	 *
	 * @return true if the triples are the same, false if not.
	 */
	private static boolean compareRDFTriples(Map<String, Object> t1,
			Map<String, Object> t2) {
		for (String attr : new String[] { "subject", "predicate", "object" }) {
			Map<String,Object> t1a = (Map<String, Object>) t1.get(attr);
			Map<String,Object> t2a = (Map<String, Object>) t2.get(attr);
			if (!_equals(t1a.get("type"), t2a.get("type")) || !_equals(t1a.get("value"), t2a.get("value"))) {
				return false;
			}
		}
		Map<String,Object> t1o = (Map<String, Object>) t1.get("object");
		Map<String,Object> t2o = (Map<String, Object>) t2.get("object");
		if (t1o.containsKey("language")) {
			if (t2o.containsKey("language")) {
				if (!_equals(t1o.get("language"), t2o.get("language"))) {
					return false;
				}
			} else {
				return false;
			}
		} else if (t2o.containsKey("language")) {
			return false;
		}
		if (t1o.containsKey("datatype")) {
			if (t2o.containsKey("datatype")) {
				if (!_equals(t1o.get("datatype"), t2o.get("datatype"))) {
					return false;
				}
			} else {
				return false;
			}
		} else if (t2o.containsKey("datatype")) {
			return false;
		}
		return true;
	}
	
	/**
	 * Returns the triple in the internal RDF dataset format
	 * 
	 * @param s the subject IRI or blank node id
	 * @param p the predicate IRI
	 * @param o the object IRI or blank node id
	 * @return the resulting triple
	 */
	public static Map<String,Object> generateTriple(final String s, final String p, final String o) {
		return new LinkedHashMap<String, Object>() {{
			put("subject", new LinkedHashMap<String, Object>() {{ 
				put("value", s);
				put("type", s.startsWith("_:") ? "blank node" : "IRI");
			}});
			put("predicate", new LinkedHashMap<String, Object>() {{ 
				put("value", p);
				put("type", "IRI");
			}});
			put("object", new LinkedHashMap<String, Object>() {{ 
				put("value", o);
				put("type", o.startsWith("_:") ? "blank node" : "IRI");
			}});
		}};
	}
	
	/**
	 * Returns the triple in the internal RDF dataset format
	 * 
	 * @param s the subject IRI or blank node id
	 * @param p the predicate IRI
	 * @param value the value of the literal object
	 * @param datatype the datatype of the literal object (defaults to XSD_STRING if null)
	 * @param language the language of the literal object (or null if no language specified)
	 * @return the resulting triple
	 */
	public static Map<String,Object> generateTriple(final String s, final String p, final String value, final String datatype, final String language) {
		return new LinkedHashMap<String, Object>() {{
			put("subject", new LinkedHashMap<String, Object>() {{ 
				put("value", s);
				put("type", s.startsWith("_:") ? "blank node" : "IRI");
			}});
			put("predicate", new LinkedHashMap<String, Object>() {{ 
				put("value", p);
				put("type", "IRI");
			}});
			put("object", new LinkedHashMap<String, Object>() {{ 
				put("value", value);
				put("type", "literal");
				if (language != null) {
					put("language", language);
				} else {
					put("datatype", datatype);
				}
			}});
		}};
	}
	
	/**
	 * Returns the basic structure of the internal RDF Dataset format for parsing RDF to JSONLD using fromRDF
	 * 
	 * @return
	 */
	public static Map<String,Object> getInitialRDFDatasetResult() {
		return new LinkedHashMap<String, Object>(){{
			put("@default", new ArrayList<Object>());
		}};
	}
	
	/**
	 * Adds the specified triple to the result dataset
	 * 
	 * @param result the result dataset to add the triple to
	 * @param graphName the graph to add the triple to (uses "@default" if set to null)
	 * @param triple the triple
	 */
	public static void addTripleToRDFDatasetResult(Map<String,Object> result, String graphName, Map<String,Object> triple) {
		List<Object> graph;
		if (graphName == null) {
			graph = (List<Object>) result.get("@default");
		} else {
			if (result.containsKey(graphName)) {
				graph = (List<Object>) result.get(graphName);
			} else {
				graph = new ArrayList<Object>();
				result.put(graphName, graph);
			}
		}
		graph.add(triple);
	}
}
