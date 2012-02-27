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

import de.dfki.km.json.JSONUtils;

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
		rval.put("@id", "@id");
		rval.put("@language", "@language");
		rval.put("@value", "@value");
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

	public static String getTermIri(Object ctx, String term) {
		String rval = null;
		
		if (((Map<String,Object>) ctx).containsKey(term)) {
			Object t = ((Map<String,Object>) ctx).get(term);
			if (t instanceof String) {
				rval = (String)t;
			} else if (t instanceof Map && ((Map<String,Object>) t).containsKey("@id")) {
				rval = (String) ((Map<String,Object>) t).get("@id");
			}
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
				merged.put(key, ctx2.get(key));
			}
			
			/* OLD COERCION CODE BELOW
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
			} */
		}
		return merged;
	}
	
	
	
	public static String compactIRI(Map<String, Object> ctx, String iri) { //, Context usedCtx) {
		String rval = null;
				
		for (String key: ctx.keySet()) {
			if (!key.startsWith("@")) {
				if (iri.equals(getTermIri(ctx, key))) {
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
					String ctxIRI = getTermIri(ctx, key);
					
					if (ctxIRI != null) {
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
		if ("@id".equals(p) || "@type".equals(p)) {
			rval = "@id";
		} else {
			p = compactIRI(ctx, p);//, null);
			
			if (ctx.containsKey(p) && ctx.get(p) instanceof Map && ((Map<String,String>) ctx.get(p)).containsKey("@type")) {
				String type = ((Map<String, String>) ctx.get(p)).get("@type");
				//if (type != null) { // type could be null!
				rval = expandTerm(ctx, type);
				//}
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
		String rval = term;
		
		// 1. If the property has a colon, it has a prefix or an absolute IRI:
		int idx = term.indexOf(":");
		if (idx != -1) {
			String prefix = term.substring(0, idx);
			
			// 1.1 See if the prefix is in the context:
			if (((Map<String, String>) ctx).containsKey(prefix)) {
				String iri = getTermIri(ctx, prefix);
				rval = iri + term.substring(idx + 1);
				/*if (usedCtx != null) {
					usedCtx.put(prefix, ctx.get(prefix));
				}*/
			}
		} else if (((Map<String, String>) ctx).containsKey(term)) {
			// 2. If the property is in the context, then it's a term.
			rval = getTermIri(ctx, term); // TODO: assuming string
			/*if (usedCtx != null) {
				usedCtx.put(term, rval);
			}*/
		} else {
			// 3. The property is a keyword.
			for (String k: keywords.keySet()) {
				String v = keywords.get(k);
				if (v.equals(term)) {
					rval = k;
					break;
				}
			}
			
		}
		
		return rval;
	}
	
	public static boolean isReference(Object value) {
		return (value != null &&
				value instanceof Map &&
				((Map<String, Object>) value).containsKey("@id") &&
				((Map<String,Object>) value).size() == 1);
	}
	
	public static boolean isSubject(Object value) {
		boolean rval = false;
		if (value != null &&
			value instanceof Map &&
			!((Map<String,Object>) value).containsKey("@value")) {
			rval = ((Map<String,Object>) value).size() > 1 || 
					!((Map<String,Object>) value).containsKey("@id");
		}
		return rval;
	}

	public static boolean isBlankNode(Object v) {
		return isSubject(v) &&
				(!((Map<String,Object>) v).containsKey("@id") ||
						isNamedBlankNode(v));
	}

	public static boolean isNamedBlankNode(Object v) {
		return v instanceof Map &&
				((Map<String,Object>) v).containsKey("@id") &&
			   isBlankNodeIri(((Map<String,Object>) v).get("@id"));
	}

	public static boolean isBlankNodeIri(Object input) {
		return input instanceof String && ((String) input).startsWith("_:");
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
			
			if (!p.equals("@id")) {
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
							if (rval != 0) {
								break;
							}
						}
					}
				}
				if (rval != 0) {
					break;
				}
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
			rval = compareObjectKeys(o1,o2,"@value");
			if (rval == 0) {
				if (((Map) o1).containsKey("@value")) {
					rval = compareObjectKeys(o1,o2,"@type");
					if (rval == 0) {
						rval = compareObjectKeys(o1,o2,"@language");
					}
				} else {
					rval = compare(((Map<String,Object>) o1).get("@id"), ((Map<String,Object>) o2).get("@id"));
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
			if (!"@id".equals(p)) {
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
						if (((Map) o).containsKey("@id")) { // iri
							if (isBlankNodeIri(((Map<String,Object>) o).get("@id"))) {
								rval += "_:";
							} else {
								rval += "<" + ((Map<String,Object>) o).get("@id") + ">";
							}
						} else { // literal
							rval += "\"" + ((Map<String,Object>) o).get("@value") + "\"";
							if (((Map<String,Object>) o).containsKey("@type")) {
								rval += "^^<" + ((Map<String,Object>) o).get("@type") + ">";
							} else if (((Map<String,Object>) o).containsKey("@language")) {
								rval += "@" + ((Map<String,Object>) o).get("@language");
							}
						}
					} else {
						rval += "\"" + o + "\"";
					}
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
