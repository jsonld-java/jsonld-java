package de.dfki.km.json.jsonld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;

public class JSONLDUtils {
	
	public static class NameGenerator {
		private String prefix;
		private int count;

		public NameGenerator(String prefix) {
			this.prefix = prefix;
			this.count = -1	;
		}
		
		public String next() {
			count += 1;
			return current();
		}

		public String current() {
			return "_:" + prefix + count;
		}
		
		public boolean inNamespace(String iri) {
			return iri.startsWith("_:" + prefix);
		}
	}
	
	public static Map<String,String> getKeywords(Object ctx) {
		Map<String,String> rval = new HashMap<String,String>();
		rval.put("@datatype", "@datatype");
		rval.put("@iri", "@iri");
		rval.put("@language", "@language");
		rval.put("@literal", "@literal");
		rval.put("@subject", "@subject");
		rval.put("@type", "@type");
		
		if (ctx != null && ctx instanceof Map) {
			Map<String,String> keywords = new HashMap<String, String>();
			for (String key: ((Map<String,Object>)ctx).keySet()) {
				Object value = ((Map<String, String>) ctx).get(key); 
				if (value instanceof String && rval.containsKey(value)) {
					keywords.put((String)value, key);
				}
			}
			
			rval.putAll(keywords);
		}
		return rval;
	}

	public static Map<String,Object> mergeContexts(Object ctxOne, Object ctxTwo) throws Exception {

		Map<String,Object> ctx1;
		Map<String,Object> ctx2;
		
		if (ctxOne instanceof List) {
			ctx1 = mergeContexts(new HashMap<String,Object>(), ctxOne);
		} else {
			ctx1 = (Map<String, Object>) ctxOne;
		}
		
		Map<String,Object> merged = (Map<String, Object>) clone(ctx1);
		
		if (ctxTwo instanceof List) {
			for (Object i: (List<Object>)ctxTwo) {
				merged = mergeContexts(merged, i);
			}
		} else {
			ctx2 = (Map<String, Object>) ctxTwo;
			
			for (String key: ctx2.keySet()) {
				if (!key.startsWith("@")) {
					for (String mkey: merged.keySet()) {
						if (merged.get(mkey).equals(ctx2.get(key))) {
							// FIXME: update related @coerce rules
							merged.remove(mkey);
							break;
						}
					}
				}
			}
			
			
			// merge contexts
			for (String key: ctx2.keySet()) {
				if (!key.equals("@coerce")) {
					merged.put(key, clone(ctx2.get(key)));
				}
			}
			
			// merge @coerce
			if (ctx2.containsKey("@coerce")) {
				if (!merged.containsKey("@coerce")) {
					merged.put("@coerce", clone(ctx2.get("@coerce")));
				} else {
					for (String key: ((Map<String, Object>) ctx2.get("@coerce")).keySet()) {
						Map<String,Object> mcoerce = (Map<String, Object>) merged.get("@coerce");
						mcoerce.put(key, ((Map<String,Object>) ctx2.get("@coerce")).get(key));
					}
				}
			}
		}
		return merged;
	}
	
	
	
	public static String compactIRI(Map<String, Object> ctx, String iri) { //, Context usedCtx) {
		String rval = null;
				
		for (String key: ctx.keySet()) {
			if (!key.startsWith("@")) {
				if (iri.equals(ctx.get(key))) {
					// compact to a term
					rval = key;
					/*if (usedCtx != null) {
						usedCtx.put(key, ctx.get(key));
					}*/
					break;
				}
			}
		}
		
		if (rval == null && iri.equals("@type")) {
			rval = getKeywords(ctx).get("@type"); 
		}
		
		if (rval == null) {
			// rval still not found, check the context for a CURIE prefix
			for (String key: ctx.keySet()) {
				if (!key.startsWith("@")) {
					String ctxIRI = (String) ctx.get(key);
					if (iri.startsWith(ctxIRI) && iri.length() > ctxIRI.length()) {
						rval = key + ":" + iri.substring(ctxIRI.length());
						/*if (usedCtx != null) {
							usedCtx.put(key, ctxIRI);
						}*/
						break;
					}
				}
			}
		}
		
		if (rval == null) {
			// could not compact IRI
			rval = iri;
		}
		
		return rval;
	}
	
	public static String getCoercionType(Map<String,Object> ctx, String property) {
		String rval = null;

		// get expanded property
		String p = expandTerm(ctx, property);//, null);
		
		// built-in type coercion JSON-LD-isms
		if ("@subject".equals(p) || "@type".equals(p)) {
			rval = "@iri";
		} else if (ctx.containsKey("@coerce")) {
			p = compactIRI(ctx, p);//, null);
			String type = ((Map<String, String>) ctx.get("@coerce")).get(p);
			if (type != null) { // type could be null!
				rval = expandTerm(ctx, type);
			}
			
			/* THE FOLLOWING IS THE OLDER VERSION OF COERCE WHICH
			   USED THE TYPE AS THE KEY 
			 
			for (String type: ((Map<String,Object>) ctx.get("@coerce")).keySet()) {
				Object props = ((Map<String,Object>) ctx.get("@coerce")).get(type);
				if (!(props instanceof List)) {
					Object tmp = props;
					props = new ArrayList<Object>();
					((List<Object>) props).add(tmp);
				}
				
				for (Object o: (List<Object>)props) {
					if (o.equals(p)) {
						// property found
						rval = expandTerm(ctx, type);
						break;
					}
				}
			} */
		}
		
		return rval;
	}
	
	public static String expandTerm(Object ctx, String term) {
		Map<String,String> keywords = getKeywords(ctx);
		String rval = "";
		
		// 1. If the property has a colon, then it is a CURIE or an absolute IRI:
		int idx = term.indexOf(":");
		if (idx != -1) {
			String prefix = term.substring(0, idx);
			
			// 1.1 See if the prefix is in the context:
			if (((Map<String, String>) ctx).containsKey(prefix)) {
				rval = ((Map<String, String>) ctx).get(prefix) + term.substring(idx + 1);
				/*if (usedCtx != null) {
					usedCtx.put(prefix, ctx.get(prefix));
				}*/
			} else { // 1.2. Prefix is not in context, property is already an absolute IRI:
				rval = term;
			}
		} else if (((Map<String, String>) ctx).containsKey(term)) {
			// 2. If the property is in the context, then it's a term.
			rval = ((Map<String, String>) ctx).get(term); // TODO: assuming string
			/*if (usedCtx != null) {
				usedCtx.put(term, rval);
			}*/
		} else if (term.equals(keywords.get("@subject"))) {
			// 3. The property is the special-case subject.
			rval = "@subject";
		} else if (term.equals(keywords.get("@type"))) {
			// 4. The property is the special-case rdf type.
			rval = "@type";
		} else {
			// 5. The property is a relative IRI, prepend the default vocab.
			rval = term;
			if (((Map<String, String>) ctx).containsKey("@vocab")) {
				rval = ((Map<String, String>) ctx).get("@vocab") + rval;
				/*if (usedCtx != null) {
					usedCtx.put("@vocab", ctx.get("@vocab"));
				}*/
			}
		}
		
		return rval;
	}
	
	public static void collectSubjects(Object input, Map<String,Object> subjects, List<Map<String, Object>> bnodes) {
		if (input == null) {
			return;
		} else if (input instanceof List) {
			for (Object o: (List<Object>)input) {
				collectSubjects(o, subjects, bnodes);
			}
		} else if (input instanceof Map) {
			if (((Map<String,Object>) input).containsKey("@subject")) {
				Object subj = ((Map<String,Object>) input).get("@subject");
				if (subj instanceof List) {
					// graph literal
					collectSubjects(subj, subjects, bnodes);
				} else {
					// named subject
					subjects.put((String)((Map<String,Object>) subj).get("@iri"), input);
				}
				
			} else if (isBlankNode(input)) {
				bnodes.add((Map<String, Object>) input);
			}
			
			for (String key: ((Map<String,Object>) input).keySet()) {
				collectSubjects(((Map<String,Object>) input).get(key), subjects, bnodes);
			}
		}
	}

	public static boolean isBlankNode(Object v) {
		return v instanceof Map &&
			   !(((Map<String,Object>) v).containsKey("@iri") || ((Map<String,Object>) v).containsKey("@literal")) &&
			   (!((Map<String,Object>) v).containsKey("@subject") || isNamedBlankNode(v));
	}

	public static boolean isNamedBlankNode(Object v) {
		return v instanceof Map &&
				((Map<String,Object>) v).containsKey("@subject") &&
			   ((Map<String,Object>) ((Map<String, Object>) v).get("@subject")).containsKey("@iri") &&
			   isBlankNodeIri(((Map<String,Object>) ((Map<String,Object>) v).get("@subject")).get("@iri"));
	}

	public static boolean isBlankNodeIri(Object input) {
		return input instanceof String && ((String) input).startsWith("_:");
	}
	
	public static void flatten(Object parent, String parentProperty, Object value, Map<String,Object> subjects) throws Exception {
		
		Object flattened = null;
		
		if (value == null) {
			// drop null values
		} else if (value instanceof List) {
			for (Object v: (List<Object>)value) {
				flatten(parent, parentProperty, v, subjects);
			}
		} else if (value instanceof Map) {
			Map<String,Object> mapVal = (Map<String, Object>) value;
			if (mapVal.containsKey("@subject") && mapVal.get("@subject") instanceof List) {
				// graph literal/disjoint graph
				if (parent != null) {
					// cannot flatten embedded graph literals
					throw new Exception("Embedded graph literals cannot be flattened");
				}
				
				// top-level graph literal
				for (Object key: (List<Object>)mapVal.get("@subject")) {
					flatten(parent, parentProperty, key, subjects);
				}
			} else if (mapVal.containsKey("@literal") || mapVal.containsKey("@iri")) {
				// already-expanded value
				flattened = clone(value);
			} else {
				// subject
				
				// create of fetch existing subject
				Object subject;
				if (subjects.containsKey(((Map<String,Object>) mapVal.get("@subject")).get("@iri"))) {
					subject = subjects.get(((Map<String,Object>) mapVal.get("@subject")).get("@iri"));
				} else {
					subject = new HashMap<String, Object>();
					if (mapVal.containsKey("@subject")) {
						subjects.put((String)((Map<String,Object>) mapVal.get("@subject")).get("@iri"), subject);
					}
				}
				flattened = subject;
				
				for (String key: mapVal.keySet()) {
					Object v = mapVal.get(key);
					
					if (v != null) {
						if (((Map<String,Object>) subject).containsKey(key)) {
							if (!(((Map<String,Object>) subject).get(key) instanceof List)) {
								Object tmp = ((Map<String,Object>) subject).get(key);
								List<Object> lst = new ArrayList<Object>();
								lst.add(tmp);
								((Map<String,Object>) subject).put(key, lst);
							}
						} else {
							List<Object> lst = new ArrayList<Object>();
							((Map<String,Object>) subject).put(key, lst);
						}					
						
						flatten(((Map<String,Object>) subject).get(key), null, v, subjects);
						if (((List<Object>) ((Map<String,Object>) subject).get(key)).size() == 1) {
							// convert subject[key] to a single object if there is only one object in the list
							((Map<String,Object>) subject).put(key, ((List<Object>) ((Map<String,Object>) subject).get(key)).get(0));
						}
					}
				}
			}
		} else {
			// string value
			flattened = value;
		}
		
		if (flattened != null && parent != null) {
			// remove top-level '@subject' for subjects
			// 'http://mypredicate': {'@subject': {'@iri': 'http://mysubject'}}
		    // becomes
		    // 'http://mypredicate': {'@iri': 'http://mysubject'}
			if (flattened instanceof Map && ((Map<String,Object>) flattened).containsKey("@subject")) {
				flattened = ((Map<String,Object>) flattened).get("@subject");
			}
			
			if (parent instanceof List) {
				boolean duplicate = false;
				if (flattened instanceof Map && ((Map<String,Object>) flattened).containsKey("@iri")) {
					for (Object e: (List<Object>)parent) {
						if (e instanceof Map && ((Map<String,Object>) e).containsKey("@iri") && ((Map<String,Object>) e).get("@iri").equals(((Map<String,Object>) flattened).get("@iri"))) {
							duplicate = true;
							break;
						}
					}
				}
				if (!duplicate) {
					((List<Object>) parent).add(flattened);
				}
			} else {
				((Map<String,Object>) parent).put(parentProperty, flattened);
			}
		}
	}

	public static Object clone(Object value) throws CloneNotSupportedException {
		Object rval = null;
		if (value instanceof Cloneable) {
			try {
				rval = value.getClass().getMethod("clone").invoke(value);
			} catch (Exception e) {
				rval = e;
			}
		}
		if (rval == null || rval instanceof Exception) {
			// the object wasn't cloneable, or an error occured
			if (value instanceof String || value instanceof Number || value instanceof Number) {
				// strings numbers and booleans are immutable
				rval = value;
			} else {
				throw new CloneNotSupportedException((rval instanceof Exception ? ((Exception)rval).getMessage() : ""));
			}
		}
		return rval;
	}

	public static int compare(Object v1, Object v2) {
		int rval = 0;
		
		if (v1 instanceof List && v2 instanceof List) {
			for (int i = 0; i < ((List<Object>) v1).size() && rval == 0; i++) {
				rval = compare(((List<Object>) v1).get(i), ((List<Object>) v2).get(i));
			}
		} else if (v1 instanceof Number && v2 instanceof Number) {
			// TODO: this is sketchy
			double n1 = ((Number)v1).doubleValue();
			double n2 = ((Number)v2).doubleValue();
			
			rval = (n1 < n2 ? -1 : (n1 > n2 ? 1 : 0));
		} else if (v1 instanceof String && v2 instanceof String) {
			rval = ((String)v1).compareTo((String)v2);
		} else {
			// TODO: should we just assume the objects are equal?
		}
		
		return rval;
	}

	public static int compareBlankNodeObjects(Map<String, Object> a, Map<String, Object> b) {
		int rval = 0;
		
		for (String p: a.keySet()) {
			int lenA = (a.get(p) instanceof List ? ((List<Object>) a.get(p)).size() : 1);
			int lenB = (b.get(p) instanceof List ? ((List<Object>) b.get(p)).size() : 1);
			rval = compare(lenA, lenB);
			
			if (rval == 0) {
				List<Object> objsA;
				List<Object> objsB;
				
				if (a.get(p) instanceof List) {
					objsA = (List<Object>) a.get(p);
					objsB = (List<Object>) b.get(p);
				} else {
					objsA = new ArrayList<Object>();
					objsA.add(a.get(p));
					objsB = new ArrayList<Object>();
					objsB.add(b.get(p));
				}
				
				for (int i = 0; i < objsA.size(); i++) {
					Object e = objsA.get(i);
					if (!(e instanceof String || !(((Map<String,Object>) e).containsKey("@iri") && isBlankNodeIri(((Map<String,Object>) e).get("@iri"))))) {
						objsA.remove(i);
						--i;
					}
				}
				for (int i = 0; i < objsB.size(); i++) {
					Object e = objsB.get(i);
					if (!(e instanceof String || !(((Map<String,Object>) e).containsKey("@iri") && isBlankNodeIri(((Map<String,Object>) e).get("@iri"))))) {
						objsB.remove(i);
						--i;
					}
				}
				
				rval = compare(objsA.size(), objsB.size());
				
				if (rval == 0) {
					Collections.sort(objsA, new Comparator<Object>() {
						public int compare(Object o1, Object o2) {
							return compareObjects(o1, o2);
						}
					});
					Collections.sort(objsB, new Comparator<Object>() {
						public int compare(Object o1, Object o2) {
							return compareObjects(o1, o2);
						}
					});
					for (int i = 0; i < objsA.size() && rval == 0; ++i) {
						rval = compareObjects(objsA.get(i), objsB.get(i));
					}
				}
			}
			if (rval != 0) {
				break;
			}
		}
		
		return rval;
	}
	
	public static int compareObjects(Object o1, Object o2) {
		int rval = 0;
		if (o1 instanceof String) {
			if (o2 instanceof String) {
				rval = compare(o1, o2);
			} else {
				rval = -1;
			}
		} else if (o2 instanceof String) {
			rval = 1;
		} else if (o1 instanceof Map){
			rval = compareObjectKeys(o1,o2,"@literal");
			if (rval == 0) {
				if (((Map) o1).containsKey("@literal")) {
					rval = compareObjectKeys(o1,o2,"@datatype");
					if (rval == 0) {
						rval = compareObjectKeys(o1,o2,"@language");
					}
				} else {
					rval = compare(((Map<String,Object>) o1).get("@iri"), ((Map<String,Object>) o2).get("@iri"));
				}
			}
			
		}
		return rval;
	}

	private static int compareObjectKeys(Object o1, Object o2, String key) {
		int rval = 0;
		if (((Map<String,Object>) o1).containsKey(key)) {
			if (((Map<String,Object>) o2).containsKey(key)) {
				rval = compare(((Map<String,Object>) o1).get(key), ((Map<String,Object>) o2).get(key));
			} else {
				rval = -1;
			}
		} else if (((Map<String,Object>) o2).containsKey(key)) {
			rval = 1;
		}
		return rval;
	}

	public static void rotate(List<Object> a) {
		if (a.size() > 0) {
			Object tmp = a.remove(0);
			a.add(tmp);
		}
	}

	public static int compareSerializations(String s1, String s2) {
		int rval = 0;
		if (s1.length() == s2.length()) {
			rval = compare(s1,s2);
		} else if (s1.length() > s2.length()) {
			rval = compare(s1.substring(0, s2.length()), s2);
		} else {
			rval = compare(s1, s2.substring(0, s1.length()));
		}
		
		return rval;
	}

	public static String serializeProperties(Map<String, Object> b) {
		String rval = "";
		
		Boolean first = true;
		for (String p: b.keySet()) {
			if (first) {
				first = false;
			} else {
				rval += "|";
			}
			
			rval += "<" + p + ">";
			
			List<Object> objs = null;
			if (b.get(p) instanceof List) {
				objs = (List<Object>) b.get(p);
			} else {
				objs = new ArrayList<Object>();
				objs.add(b.get(p));
			}
			
			for (Object o: objs) {
				if (o instanceof Map) {
					if (((Map) o).containsKey("@iri")) { // iri
						if (isBlankNodeIri(((Map<String,Object>) o).get("@iri"))) {
							rval += "_:";
						} else {
							rval += "<" + ((Map<String,Object>) o).get("@iri") + ">";
						}
					} else { // literal
						rval += "\"" + ((Map<String,Object>) o).get("@literal") + "\"";
						if (((Map<String,Object>) o).containsKey("@datatype")) {
							rval += "^^<" + ((Map<String,Object>) o).get("@datatype") + ">";
						} else if (((Map<String,Object>) o).containsKey("@language")) {
							rval += "@" + ((Map<String,Object>) o).get("@language");
						}
					}
				} else {
					rval += "\"" + o + "\"";
				}
			}
		}
		
		return rval;
	}

	public static void setProperty(Map<String,Object> s, String p, Object o) {
		if (s.containsKey(p)) {
			if (s.get(p) instanceof List) {
				((List<Object>) s.get(p)).add(o);
			} else {
				List<Object> tmp = new ArrayList<Object>();
				tmp.add(s.get(p));
				tmp.add(o);
				s.put(p, tmp);
			}
		} else {
			s.put(p, o);
		}
	}
	
}
