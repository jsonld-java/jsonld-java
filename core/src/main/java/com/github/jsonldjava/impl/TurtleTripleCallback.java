package com.github.jsonldjava.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.jsonldjava.core.JSONLDTripleCallback;
import com.github.jsonldjava.core.RDFDataset;
import com.github.jsonldjava.utils.Obj;

import static com.github.jsonldjava.core.JSONLDConsts.*;

public class TurtleTripleCallback implements JSONLDTripleCallback {

	private static final int MAX_LINE_LENGTH = 160;
	private static final int TAB_SPACES = 4;
	final Map<String,String> availableNamespaces = new LinkedHashMap<String, String>() {{
		// TODO: fill with default namespaces
	}};
	Set<String> usedNamespaces;
	
	public TurtleTripleCallback() {}
	public TurtleTripleCallback(Map<String,Object> context) {
		processJSONLDContext(context);
	}
	
	private void processJSONLDContext(Map<String,Object> context) {
		// TODO: this is overly simple and will most likely fail with complicated examples
		if (context == null) {
			return;
		}
		for (String key: context.keySet()) {
			Object value = context.get(key);
			if (value instanceof String) {
				availableNamespaces.put((String)value, key);
			} else if (value instanceof Map && ((Map<String, String>) value).containsKey("@id")) {
				availableNamespaces.put((String)((Map<String, String>) value).get("@id"), key);
			}
		}
	}
	
	@Override
	public Object call(RDFDataset dataset) {
		usedNamespaces = new LinkedHashSet<String>();
		
		int tabs = 0;
		
		Map<String,List<Object>> refs = new LinkedHashMap<String, List<Object>>();
		Map<String,Map<String,List<Object>>> ttl = new LinkedHashMap<String,Map<String,List<Object>>>();
		
		
		for (String graphName : dataset.keySet()) {
			List<Map<String,Object>> triples = (List<Map<String, Object>>) dataset.get(graphName);
			if ("@default".equals(graphName)) {
				graphName = null;
			}
			
			// http://www.w3.org/TR/turtle/#unlabeled-bnodes
			// TODO: implement nesting for unlabled nodes
			
			// map of what the output should look like
			//   subj (or [ if bnode) > pred > obj
			//								 > obj (set ref if IRI)
			//						  > pred > obj (set ref if bnode)
			//   subj > etc etc etc
											 
			// subjid -> [ ref, ref, ref ]
			
			String prevSubject = "";
			String prevPredicate = "";
			
			Map<String,List<Object>> thisSubject = null;
			List<Object> thisPredicate = null;
			
			for (Map<String,Object> triple : triples) {
				String subject = (String)Obj.get(triple, "subject", "value");
				String predicate = (String)Obj.get(triple, "predicate", "value");
				
				if (prevSubject.equals(subject)) {
					if (prevPredicate.equals(predicate)) {
						// nothing to do
					} else {
						// new predicate
						String p;
						if (RDF_TYPE.equals(predicate)) {
							p = "a";
						} else {
							p = getURI(predicate);
						}
						if (thisSubject.containsKey(p)) {
							thisPredicate = thisSubject.get(p);
						} else {
							thisPredicate = new ArrayList<Object>();
							thisSubject.put(p, thisPredicate);
						}
						prevPredicate = predicate;
					}
				} else {
					// new subject
					String s = getURI(subject);
					String p;
					if (RDF_TYPE.equals(predicate)) {
						p = "a";
					} else {
						p = getURI(predicate);
					}
					
					if (ttl.containsKey(s)) {
						thisSubject = ttl.get(s);
					} else {
						thisSubject = new LinkedHashMap<String, List<Object>>();
						ttl.put(s, thisSubject);
					}
					if (thisSubject.containsKey(p)) {
						thisPredicate = thisSubject.get(p);
					} else {
						thisPredicate = new ArrayList<Object>();
						thisSubject.put(p, thisPredicate);
					}
					
					prevSubject = subject;
					prevPredicate = predicate;
				}
				
				if ("literal".equals(Obj.get(triple, "object", "type"))) {
					String literal = "\"" + (String)Obj.get(triple, "object", "value") + "\"";
					String language = (String)Obj.get(triple, "object", "language");
					String datatype = (String)Obj.get(triple, "object", "datatype");
					if (language != null) {
						literal += "@" + language;
					} else {
						// TODO: not sure if turtle is fussy about not writing string datatypes
						if (datatype != null && !XSD_STRING.equals(datatype)) {
							literal += "^^" + getURI(datatype);
						}
					}
					thisPredicate.add(literal);
				} else {
					String o = getURI((String)Obj.get(triple, "object", "value"));
					if (o.startsWith("_:")) {
						// add ref to o
						if (!refs.containsKey(o)) {
							refs.put(o, new ArrayList<Object>());
						}
						refs.get(o).add(thisPredicate);
					}
					thisPredicate.add(o);
				}
			}
		}
		
		// process refs (nesting referenced bnodes if only one reference to them in the whole graph)
		for (String id : refs.keySet()) {
			// skip items if there is more than one reference to them in the graph
			if (refs.get(id).size() > 1) {
				continue;
			}
			
			// otherwise embed them into the referenced location
			Map<String, List<Object>> object = ttl.remove(id);
			List<Object> predicate = (List<Object>) refs.get(id).get(0);
			// replace the one bnode ref with the object
			predicate.set(predicate.lastIndexOf(id), object);
		}
		
		// build turtle output
		String output = generateTurtle(ttl, 0, 0, false);
		
		String prefixes = "";
		for (String prefix : usedNamespaces) {
			String name = availableNamespaces.get(prefix);
			prefixes += "@prefix " + name + ": <" + prefix + "> .\n";
		}
		
		return ("".equals(prefixes) ? "" : prefixes + "\n") + output;
	}
	
	private String generateTurtle(Map<String, Map<String, List<Object>>> ttl, int indentation, int lineLength, boolean isObject) {
		String rval = "";
		for (String subject : ttl.keySet()) {
			//rval += tabs(indentation);
			boolean isBlankNode = subject.startsWith("_:");
			if (isBlankNode) {
				rval += "[ ";
				lineLength += 2;
			} else {
				rval += subject + " ";
				lineLength += subject.length() + 1;
			}
			Iterator<String> predIter = ttl.get(subject).keySet().iterator();
			while (predIter.hasNext()) {
				String predicate = predIter.next();
				rval += predicate + " ";
				lineLength += predicate.length() + 1;
				Iterator<Object> objIter = ttl.get(subject).get(predicate).iterator();
				while (objIter.hasNext()) {
					Object object = objIter.next();
					String obj;
					if (object instanceof String) {
						obj = (String)object;
					} else {
						Map<String, Map<String, List<Object>>> tmp = new LinkedHashMap<String, Map<String,List<Object>>>();
						tmp.put("_:x",  (Map<String, List<Object>>) object);
						obj = generateTurtle(tmp, indentation+1, lineLength, true);
					}
					int idxofcr = obj.indexOf("\n");
					// check if output will fix in the max line length (factor in comma if not the last item, current line length and length to the next CR) 
					if ((objIter.hasNext() ? 1 : 0) + lineLength + (idxofcr != -1 ? idxofcr : obj.length()) > MAX_LINE_LENGTH) {
						rval += "\n" + tabs(indentation+1);
						lineLength = (indentation+1) * TAB_SPACES;
					}
					rval += obj;
					if (idxofcr != -1) {
						lineLength += (obj.length() - obj.lastIndexOf("\n"));
					} else {
						lineLength += obj.length();
					}
					if (objIter.hasNext()) {
						rval += ",";
						lineLength++;
						if (lineLength < MAX_LINE_LENGTH) {
							rval += " ";
							lineLength++;
						}
					}
				}
				if (predIter.hasNext()) {
					rval += " ;\n" + tabs(indentation+1);
					lineLength = (indentation+1) * TAB_SPACES;
				}
			}
			if (isBlankNode) {
				rval += " ]"; 
			}
			if (!isObject) {
				rval += " .\n\n";
			}
		}
		return rval;
	}
	
	// TODO: Assert (TAB_SPACES == 4) otherwise this needs to be edited, and should fail to compile
	private String tabs(int tabs) {
		String rval = "";
		for (int i = 0 ; i < tabs ; i++) {
			rval += "    "; // using spaces for tabs
		}
		return rval;
	}
	/**
	 * checks the URI for a prefix, and if one is found, set used prefixes to true
	 * @param predicate
	 * @return
	 */
	private String getURI(String uri) {
		// check for bnode
		if (uri.startsWith("_:")) {
			// return the bnode id
			return uri;
		}
		for (String prefix : availableNamespaces.keySet()) {
			if (uri.startsWith(prefix)) {
				usedNamespaces.add(prefix);
				// return the prefixed URI
				return availableNamespaces.get(prefix) + ":" + uri.substring(prefix.length());
			}
		}
		// return the full URI
		return "<" + uri + ">";
	}

}
