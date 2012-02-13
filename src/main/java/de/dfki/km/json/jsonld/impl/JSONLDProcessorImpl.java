package de.dfki.km.json.jsonld.impl;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dfki.km.json.jsonld.JSONLDConsts;
import de.dfki.km.json.jsonld.JSONLDTripleCallback;
import de.dfki.km.json.jsonld.JSONLDUtils;
import de.dfki.km.json.jsonld.JSONLDUtils.NameGenerator;


public class JSONLDProcessorImpl implements
		de.dfki.km.json.jsonld.JSONLDProcessor {

	private static final Logger LOG = LoggerFactory.getLogger( JSONLDProcessorImpl.class );
	private NameGenerator ngtmp;
	
	/*private class _Edges {
		
		private class _Edge {
			public _Edge() {}
			
			public List<Object> all = new ArrayList<Object>();
			public List<Object> bnodes = new ArrayList<Object>();
		}
		
		public _Edges() {}
		
		public Map<String,_Edge> refs = new HashMap<String, _Edge>();
		public Map<String,_Edge> props = new HashMap<String, _Edge>();
	}*/
	
	private Map<String,Object> edges;
	private Map<String,Object> subjects;
	private Map<String, Object> renamed;
	private Map<String, Object> mappings;
	private Map<String, Object> serializations;
	private NameGenerator ngc14n; 
	
	public Object expand(Object input) {
		return expand(new HashMap<String,Object>(), null, input);
	}

	private Object expand(Object ctx, Object property,
			Object value) {
		Object rval = null;
		if (value == null) {
			return null;
		} else if (property == null && value instanceof String) {
			rval = JSONLDUtils.expandTerm(ctx, (String)value);//, null));
		} else if (value instanceof List) {
			rval = new ArrayList<Object>();
			for (Object i: (List)value) {
				((List)rval).add(expand(ctx, property, i));
			}
		} else if (value instanceof Map) {
			rval = new HashMap<String,Object>();
			if (((Map<String,Object>) value).containsKey("@context")) {
				try {
					ctx = JSONLDUtils.mergeContexts((Map<String,Object>)ctx, (Map<String,Object>)((Map<String,Object>) value).get("@context"));
				} catch (Exception e) {
					// unable to merge contexts
					// TODO: this should probably just throw back to the calling function
					e.printStackTrace();
					return null;
				}
			}
			
			rval = new HashMap<String, Object>();
			for (String key: ((Map<String,Object>)value).keySet()) {
				if ("@embed".equals(key) || "@explicit".equals(key) ||
					"@default".equals(key) || "@omitDefault".equals(key)) {
					try {
						JSONLDUtils.setProperty((Map<String, Object>) rval, key, 
								JSONLDUtils.clone(((Map<String,Object>) value).get(key)));
					} catch (CloneNotSupportedException e) {
						// TODO: this should never happen, can we clean this up somehow?
						return null;
					}
				} else if (!"@context".equals(key)) {
					JSONLDUtils.setProperty((Map<String, Object>) rval,
							JSONLDUtils.expandTerm(ctx, key),
							expand(ctx, key, ((Map<String,Object>) value).get(key)));			
				}
			}
			
			/*
			Map<String, String> keywords = JSONLDUtils.getKeywords(ctx);
			
			if (!(((Map<String,Object>) value).containsKey(keywords.get("@literal")) || ((Map<String,Object>) value).containsKey(keywords.get("@iri")))) {
				// recursively handle sub-properties that aren't a sub-context
				for (String key: ((Map<String,Object>) value).keySet()) {
					// preserve frame keywords
					if ("@embed".equals(key) || "@explicit".equals(key) || 
						"@default".equals(key) || "@omitDefault".equals(key)) {
						Object tmp = null;
						try {
							tmp = JSONLDUtils.clone(((Map<String,Object>) value).get(key));
						} catch (CloneNotSupportedException e) {
							LOG.error("This is bad. We should never get clone not supported with maps!");
						}
						JSONLDUtils.setProperty((Map<String, Object>) rval, key, tmp);
					} else if (!("@context".equals(key))) {
						// set object to expanded property
						JSONLDUtils.setProperty((Map<String, Object>) rval, JSONLDUtils.expandTerm(ctx, key), expand(ctx, key, ((Map<String,Object>) value).get(key)));
					}
				}
			} else {
				// only need to expand keywords
				if (((Map<String,Object>) value).containsKey(keywords.get("@iri"))) {
					((Map<String,Object>) rval).put("@iri", ((Map<String,Object>) value).get(keywords.get("@iri")));
				} else {
					((Map<String,Object>) rval).put("@literal", ((Map<String,Object>) value).get(keywords.get("@literal")));
					if (((Map<String,Object>) value).containsKey(keywords.get("@language"))) {
						((Map<String,Object>) rval).put("@language", ((Map<String,Object>) value).get(keywords.get("@language")));
					} else if (((Map<String,Object>) value).containsKey(keywords.get("@type"))) {
						((Map<String,Object>) rval).put("@type", ((Map<String,Object>) value).get(keywords.get("@type")));
					}
				}
			} */
		} else {
			// do type coercion
			String coerce = JSONLDUtils.getCoercionType((Map<String, Object>) ctx, (String)property);//, null);
			Map<String,String> keywords = JSONLDUtils.getKeywords(ctx);
			
			// automatic coercion for basic JSON types
			if (coerce == null) {
				if (value instanceof Boolean) {
					coerce = JSONLDConsts.XSD_BOOLEAN;
				} else if (value instanceof Integer) {
					coerce = JSONLDConsts.XSD_INTEGER;
				} else if (value instanceof Double) {
					coerce = JSONLDConsts.XSD_DOUBLE;
				}
			}
			
			// special-case expand @id and @type (skips '@id' expansion)
			if (property.equals("@id") || property.equals(keywords.get("@id")) || 
				property.equals("@type") || property.equals(keywords.get("@type"))) {
				rval = JSONLDUtils.expandTerm(ctx, (String) value);
			}
			// coerce to appropriate type
			else if (coerce != null) {
				rval = new HashMap<String,Object>();
				
				if (coerce.equals("@id")) {
					// expand IRI
					((Map<String,Object>) rval).put("@id", JSONLDUtils.expandTerm(ctx, (String)value)); //, null));
				} else {
					((Map<String,Object>) rval).put("@type", coerce);
					if (coerce.equals(JSONLDConsts.XSD_DOUBLE)) {
						DecimalFormat decimalFormat = new DecimalFormat("0.000000E0");
						Double v = null;
						if (value instanceof String) {
							v = Double.parseDouble((String)value);
						} else if (value instanceof Integer) {
							// TODO: what is this? really?
							v = new Double(1.0*(Integer)value);
						} else {
							v = (Double)value;
						}
						String dec = decimalFormat.format(v);
						Pattern p = Pattern.compile("(-?[0-9\\.]+)E((?:-*))([0-9]+)");
						Matcher matcher = p.matcher(dec);
						matcher.find();
						String sign = matcher.group(2);
						if ("".equals(sign)) {
							sign = "+";
						}
						value = matcher.group(1) + "e" + sign + (matcher.group(3).length() > 1 ? matcher.group(3) : "0" + matcher.group(3));
					}
					((Map<String,Object>) rval).put("@value", value);
				}
			} else {
				// nothing to coerce
				rval = value.toString();
			}
		}
		
		return rval;
	}

	public Object compact(Object input, Object context) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object frame(Object input, Object frame) {
		// TODO Auto-generated method stub
		return frame(input, frame, null);
	}

	public Object frame(Object input, Object frame,	Object options) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object normalize(Object input) {
		// because the expanded output of items from the onlinebox are the same as the normalized version
		// (just inside a list) i'm going to skip implementing the normalize function for now.
		// TODO: implement this properly as if data is really to be imported into the OB with this method
		// this will be needed (mainly for identifying embedded items)
		List<Map<String,Object>> rval = new ArrayList<Map<String,Object>>();
		
		if (input != null) {
			
			Object expanded = expand(new HashMap<String,Object>(), null, input);
			
			nameBlankNodes(expanded);
						
			Map<String,Object> subjects = new HashMap<String,Object>();
			try {
				JSONLDUtils.flatten(null, null, expanded, subjects);				
			} catch (Exception e) {
				// TODO: This should probably be thrown back to the caller
				e.printStackTrace();
				LOG.error("flatten failed!");
				return null;
			}
			for (String key: subjects.keySet()) {
				Map<String,Object> s = (Map<String, Object>) subjects.get(key);
				// TODO: in javascript the keys are sorted and added back into the array
				// in alphabetical order. however in HashMaps, this order isn't kept
				rval.add(s);
			}
			
			canonicalizeBlankNodes(rval);

			// sort the output
			Collections.sort(rval, new Comparator<Map<String,Object>>() {
				public int compare(Map<String, Object> a, Map<String, Object> b) {
					return JSONLDUtils.compare(a.get("@id"), b.get("@id"));
				}
			});
		}
		
		return rval;
	}
	
	/**
	 * 
	 * 
	 * @return a list of objects returned by tripleCallback
	 */
	public Object triples(Object input,
			JSONLDTripleCallback tripleCallback) {
		Object normalized = normalize(input);
		
		if (tripleCallback == null) {
			// TODO: make default triple callback
		}
		
		List<Object> rval = new ArrayList<Object>();
		for (Map<String,Object> e: (List<Map<String,Object>>)normalized) {
			String s = (String)e.get("@id");
			
			for (String p: e.keySet()) {
				Object obj = e.get(p);
				
				if (p.equals("@id")) {
					continue;
				} else if (p.equals("@type")) {
					p = JSONLDConsts.RDF_SYNTAX_NS + "type";
				}
				
				if (!(obj instanceof List)) {
					List<Object> tmp = new ArrayList<Object>();
					tmp.add(obj);
					obj = tmp;
				}
				for (Object o: (List<Object>)obj) {
					Object triple = null;
					if (o instanceof String) {
						triple = tripleCallback.triple(s, p, (String)o, JSONLDConsts.XSD_STRING, null);
					} else if (o instanceof Map) {
						if (((Map) o).containsKey("@value")) {
							if (((Map) o).containsKey("@type")) {
								String datatypeURI = (String)((Map) o).get("@type");
								String value = (String) ((Map) o).get("@value");
								triple = tripleCallback.triple(s, p, value, datatypeURI, null);
							} else if (((Map) o).containsKey("@language")){
								triple = tripleCallback.triple(s, p,
										(String) ((Map) o).get("@value"),
										JSONLDConsts.XSD_STRING,
										(String) ((Map) o).get("@language")
										);
							} else {
								triple = tripleCallback.triple(s, p, 
										(String) ((Map) o).get("@value"),
										JSONLDConsts.XSD_STRING,
										null
										);
							}
						} else if (((Map) o).containsKey("@id")) {
							triple = tripleCallback.triple(s, p, (String)((Map) o).get("@id"));
						} else {
							// TODO: have i missed anything?
							return null;
						}
					}
					boolean quit = (triple == null);
					if (quit) {
						return rval;
					} else {
						rval.add(triple);
					}
				}
			}
		}
		return rval;
	}
	
	public void nameBlankNodes(Object input) {
		JSONLDUtils.NameGenerator ng = new JSONLDUtils.NameGenerator("tmp");
		this.ngtmp = ng;
		
		//Map<String,Object> subjects = new HashMap<String, Object>();
		subjects = new HashMap<String, Object>();
		List<Map<String,Object>> bnodes = new ArrayList<Map<String,Object>>();
		
		JSONLDUtils.collectSubjects(input, subjects, bnodes);
		
		for (Map<String,Object> bnode: bnodes) {
			if (!bnode.containsKey("@id")) {
				while (subjects.containsKey(ng.next()));
				((Map<String,Object>) bnode).put("@id", ng.current());
				subjects.put(ng.current(), bnode);
			}
		}
	}
	
	private void collectEdges() {
		Map<String,Object> refs = (Map<String, Object>) this.edges.get("refs");
		Map<String,Object> props = (Map<String, Object>) this.edges.get("props");
		
		for (String iri: this.subjects.keySet()) {
			Map<String,Object> subject = (Map<String, Object>) this.subjects.get(iri);
			for (String key: subject.keySet()) {
				if (!key.equals("@id")) {
					Object object = subject.get(key);
					List<Object> tmp = null;
					if (object instanceof List) {
						tmp = (List<Object>) object;
					} else {
						tmp = new ArrayList<Object>();
						tmp.add(object);
					}
					for (Object o: tmp) {
						if (o instanceof Map && ((Map) o).containsKey("@id") && this.subjects.containsKey(((Map) o).get("@id"))) {
							Object objIri = ((Map<String, Object>) o).get("@id");
							Map<String, Object> tmp1 = new HashMap<String, Object>();
							tmp1.put("s", iri);
							tmp1.put("p", key);
							((Map<String,List>) refs.get(objIri)).get("all").add(tmp1);
							tmp1 = new HashMap<String, Object>();
							tmp1.put("s", objIri);
							tmp1.put("p", key);
							((Map<String,List>) props.get(iri)).get("all").add(tmp1);
						}
					}
				}
			}
		}
		
		for (String iri: refs.keySet()) {
			// TODO: sort all
			List<Object> all = (List<Object>) ((Map<String,Object>) refs.get(iri)).get("all");
			List<Object> bnodes = new ArrayList<Object>();
			for (Object edge: all) {
				if (JSONLDUtils.isBlankNodeIri(((Map<String,Object>) edge).get("s"))) {
					bnodes.add(edge);
				}
			}
			((Map<String,Object>) refs.get(iri)).put("bnodes", bnodes);
		}
		for (String iri: props.keySet()) {
			// TODO: sort all
			List<Object> all = (List<Object>) ((Map<String,Object>) props.get(iri)).get("all");
			List<Object> bnodes = new ArrayList<Object>();
			for (Object edge: all) {
				if (JSONLDUtils.isBlankNodeIri(((Map<String,Object>) edge).get("s"))) {
					bnodes.add(edge);
				}
			}
			((Map<String,Object>) props.get(iri)).put("bnodes", bnodes);
		}
		
	}
	
	public void canonicalizeBlankNodes(List<Map<String,Object>> input) {
		
		this.renamed = new HashMap<String, Object>();
		this.mappings = new HashMap<String, Object>();
		this.serializations = new HashMap<String, Object>();
		
		this.edges = new HashMap<String, Object>();
		edges.put("refs", new HashMap<String,Object>());
		edges.put("props", new HashMap<String,Object>());
		
		this.subjects = new HashMap<String, Object>();
		List<Map<String,Object>> bnodes = new ArrayList<Map<String,Object>>();
		
		for (Map<String,Object> s: input) {
			String iri = (String)s.get("@id");
			subjects.put(iri, s);
			Map<String,Object> refs = (Map<String, Object>) edges.get("refs");
			Map<String,List> tmp = new HashMap<String,List>();
			tmp.put("all", new ArrayList<Object>());
			tmp.put("bnodes", new ArrayList<Object>());
			refs.put(iri, tmp);
			Map<String,Object> props = (Map<String, Object>) edges.get("props");
			tmp = new HashMap<String,List>();
			tmp.put("all", new ArrayList<Object>());
			tmp.put("bnodes", new ArrayList<Object>());
			props.put(iri, tmp);
			
			if (JSONLDUtils.isBlankNodeIri(iri)) {
				bnodes.add(s);
			}
		}
		
		collectEdges();
		
		this.ngc14n = new NameGenerator("c14n");
		NameGenerator c14n = this.ngc14n;
		NameGenerator ngTmp = this.ngtmp;
		
		for (Map<String,Object> bnode: bnodes) {
			String iri = (String)bnode.get("@id");
			if (c14n.inNamespace(iri)) {
				while (subjects.containsKey(ngTmp.next()));
				renameBlankNode(bnode, ngTmp.current());
				iri = (String)bnode.get("@id");
			}
			Map<String,Object> tmp = new HashMap<String, Object>();
			tmp.put("props", null);
			tmp.put("refs", null);
			serializations.put(iri, tmp);
		}
		
		Comparator<Map<String, Object>> bnodeSort = new Comparator<Map<String,Object>>() {
			
			private JSONLDProcessorImpl processor;
			
			public Comparator<Map<String,Object>> setProcessor(JSONLDProcessorImpl p) {
				processor = p;
				return this;
			}
			
			public int compare(Map<String,Object> a, Map<String,Object> b) {
				return processor.deepCompareBlankNodes(a, b);
			}
		}.setProcessor(this);
		
		// keep sorting and naming blank nodes until they are all named
		boolean resort = true;
		while (bnodes.size() > 0) {

			if (resort) {
				resort = false;
				Collections.sort(bnodes, bnodeSort);
			}
			
			Map<String,Object> bnode = bnodes.get(0);
			bnodes.remove(0);
			String iri = (String)bnode.get("@id");
			resort = serializations.containsKey(iri) && 
					((Map<String, Object>) serializations.get(iri)).get("props") != null;
			Map<String,Object> mapping = null;
			for (String dir: new String[] { "props", "refs" }) {
				// if no serialization has been computed, name only the first node
				if (serializations.containsKey(iri) && 
						((Map<String,Object>) serializations.get(iri)).containsKey(dir) && 
						((Map<String,Object>) serializations.get(iri)).get(dir) != null) {
					mapping = (Map<String, Object>) ((Map<String,Object>) ((Map<String,Object>) serializations.get(iri)).get(dir)).get("m");
				} else {
					mapping = new HashMap<String, Object>();
					mapping.put(iri, "s1");
				}
				
				// TODO: sort keys by value to name them in order
				List<String> keys = new ArrayList<String>(mapping.keySet());
				Collections.sort(keys, new Comparator<String>() {
					private Map<String,Object> mapping;
					public Comparator<String> setMapping(Map<String,Object> m) {
						this.mapping = m;
						return this;
					}
					public int compare(String a, String b) {
						return JSONLDUtils.compare(this.mapping.get(a), this.mapping.get(b));
					}
				}.setMapping(mapping));
				
				// name bnodes in mapping
				List<String> renamed = new ArrayList<String>();
				for (String iriK: keys) {
					if (!c14n.inNamespace(iri) && subjects.containsKey(iriK)) {
						renameBlankNode((Map<String, Object>) subjects.get(iriK), c14n.next());
						renamed.add(iriK);
					}
				}
								
				// only keep non-canonically named bnodes
				List<Map<String,Object>> tmp = bnodes;
				bnodes = new ArrayList<Map<String,Object>>();
				for (Map<String,Object> b: tmp) {
					String iriB = (String)b.get("@id");
					if (!c14n.inNamespace(iriB)) {
						for (Object i2: renamed) {
							if (markSerializationDirty(iriB, i2, dir)) {
								resort = true;
							}
						}
						bnodes.add(b);
					}
				}
			}
		}
		
		for (String key: ((Map<String,Object>) edges.get("props")).keySet()) {
			if (((List<Object>) ((Map<String,Object>) ((Map<String,Object>) edges.get("props")).get(key)).get("bnodes")).size() > 0) {
				Map<String,Object> bnode = (Map<String, Object>) subjects.get(key);
				for (String p: bnode.keySet()) {
					if (!p.startsWith("@") && bnode.get(p) instanceof List) {
						// TODO: bnode[p].sort(_compareObjects);
						Collections.sort((List<Object>)bnode.get(p), new Comparator<Object>() {
							public int compare(Object o1, Object o2) {
								return JSONLDUtils.compareObjects(o1, o2);
							}
							
						});
					}
				}
				
			}
		}
		
	}
	
	private boolean markSerializationDirty(String iri, Object changed, String dir) {
		boolean rval = false;
		Object s = serializations.get(iri);
		if (((Map<String,Object>) s).containsKey(dir) && ((Map<String,Object>) s).get(dir) != null && ((Map<String,Object>) ((Map<String,Object>) ((Map<String,Object>) s).get(dir)).get("m")).containsKey(changed)) {
			((Map<String,Object>) s).put(dir, null);
			rval = true;
		}
		return rval;
	}

	private void renameBlankNode(Map<String,Object> b, String id) {
		
		String old = (String)b.get("@id");
		b.put("@id", id);
		
		subjects.put(id, subjects.get(old));
		subjects.remove(old);
		
		// update reference and property lists
		((Map<String,Object>) edges.get("refs")).put(id, ((Map<String,Object>) edges.get("refs")).get(old));
		((Map<String,Object>) edges.get("props")).put(id, ((Map<String,Object>) edges.get("props")).get(old));
		((Map<String,Object>) edges.get("refs")).remove(old);
		((Map<String,Object>) edges.get("props")).remove(old);
		
		// update references to this bnode
		List<Map<String,Object>> refs = (List<Map<String,Object>>) ((Map<String,Object>) ((Map<String,Object>) edges.get("refs")).get(id)).get("all");
		for (Map<String,Object> i: refs) {		
			String iri = (String) i.get("s");
			if (iri.equals(old)) {
				iri = id;
			}
			Map<String,Object> ref = (Map<String, Object>) subjects.get(iri);
			List<Map<String,Object>> props = (List<Map<String,Object>>) ((Map<String,Object>) ((Map<String,Object>) edges.get("props")).get(iri)).get("all");
			for (Map<String,Object> i2: props) {
				if (old.equals(i2.get("s"))) {
					i2.put("s", id);
					String p = (String) i2.get("p");
					List<Object> tmp = null;
					if (ref.get(p) instanceof Map) {
						tmp = new ArrayList<Object>();
						tmp.add(ref.get(p));
					} else if (ref.get(p) instanceof List) {
						tmp = (List<Object>) ref.get(p);
					} else {
						tmp = new ArrayList<Object>();
					}
					
					for (Object n: tmp) {
						if (n instanceof Map && ((Map) n).containsKey("@id") && old.equals(((Map<String,Object>) n).get("@id"))) {
							((Map<String,Object>) n).put("@id", id);
						}
					}
				}
			}
		}
		
		// update references from this bnode
		List<Map<String,Object>> props = (List<Map<String,Object>>) ((Map<String,Object>) ((Map<String,Object>) edges.get("props")).get(id)).get("all");
		for (Map<String,Object> i: props) {
			String iri = (String) i.get("s");
			refs = (List<Map<String,Object>>) ((Map<String,Object>) ((Map<String,Object>) edges.get("refs")).get(iri)).get("all");
			for (Map<String,Object> r: refs) {
				if (old.equals(r.get("s"))) {
					r.put("s", id);
				}
			}
		}
	}
	
	private int deepCompareBlankNodes(Map<String,Object> a, Map<String,Object> b) {
		int rval = 0;
		
		String iriA = (String)a.get("@id");
		String iriB = (String)b.get("@id");
		
		if (iriA.equals(iriB)) {
			rval = 0;
		} else {
			
			// try a shallow compare first
			rval = shallowCompareBlankNodes(a, b);
			
			if (rval == 0) {
				// deep compare is needed
				String[] dirs = new String[] { "props", "refs" };
				for (int i = 0; rval == 0 && i < dirs.length; i++) {
					String dir = dirs[i];
					Map<String,Object> sA = (Map<String, Object>) serializations.get(iriA);
					Map<String,Object> sB = (Map<String, Object>) serializations.get(iriB);
					if (sA.get(dir) == null) {
						MappingBuilder mb = new MappingBuilder();
						if (dir.equals("refs")) {
							try {
								mb.mapping = (Map<String, String>) JSONLDUtils.clone(((Map<String,Object>) sA.get("props")).get("m"));
							} catch (CloneNotSupportedException e) {
								LOG.error("unexpected value, probably a coding error");
								e.printStackTrace();
							}
							mb.count = mb.mapping.size() + 1;
						}
						serializeBlankNode(sA, iriA, mb, dir);
					}
					if (sB.get(dir) == null) {
						MappingBuilder mb = new MappingBuilder();
						if (dir.equals("refs")) {
							try {
								mb.mapping = (Map<String, String>) JSONLDUtils.clone(((Map<String,Object>) sB.get("props")).get("m"));
							} catch (CloneNotSupportedException e) {
								LOG.error("unexpected value, probably a coding error");
								e.printStackTrace();
							}
							mb.count = mb.mapping.size() + 1;
						}
						serializeBlankNode(sB, iriB, mb, dir);
					}
					
					rval = JSONLDUtils.compare(((Map<String,Object>) sA.get(dir)).get("s"), ((Map<String,Object>) sB.get(dir)).get("s"));
				}
			}
		}
		
		return rval;
	}

	private void serializeBlankNode(Map<String, Object> s, String iri, MappingBuilder mb, String dir) {
		if (!(mb.processed.containsKey(iri))) {
			mb.processed.put(iri, true);
			String siri = mb.mapNode(iri);
			
			MappingBuilder original = mb.copy();
			
			List<Object> adj = (List<Object>) ((Map<String,Object>) ((Map<String,Object>) edges.get(dir)).get(iri)).get("bnodes");
			Map<String,Object> mapped = new HashMap<String, Object>();
			List<Object> notMapped = new ArrayList<Object>();
			
			for (Object i: adj) {
				if (mb.mapping.containsKey(((Map<String,Object>) i).get("s"))) {
					mapped.put(mb.mapping.get(((Map<String,Object>) i).get("s")), ((Map<String,Object>) i).get("s"));
				} else {
					notMapped.add(i);
				}
			}
			
			int combos = Math.max(1, notMapped.size());
			for (int i = 0; i < combos; ++i) {
				MappingBuilder m = (i == 0) ? mb : original.copy();
				serializeCombos(s, iri, siri, mb, dir, mapped, notMapped);
			}	
		}		
	}

	private void serializeCombos(Map<String, Object> s, String iri,	String siri, MappingBuilder mb, String dir,	Map<String, Object> mapped, List<Object> notMapped) {
		if (notMapped.size() > 0) {
			try {
				mapped = (Map<String, Object>) JSONLDUtils.clone(mapped);
			} catch (CloneNotSupportedException e) {
				// TODO: clone not being supported shouldn't be a problem because the maps should always cloneable
				LOG.error("this should never be reached");
				return;
			}
			mapped.put(mb.mapNode((String) ((Map<String,Object>) notMapped.get(0)).get("s")), ((Map<String,Object>) notMapped.get(0)).get("s"));
			
			MappingBuilder original = mb.copy();
			notMapped.remove(0);
			
			int rotations = Math.max(1, notMapped.size());
			for (int r = 0; r < rotations; ++r) {
				MappingBuilder m = (r == 0) ? mb : original.copy();
				serializeCombos(s, iri, siri, m, dir, mapped, notMapped);
				JSONLDUtils.rotate(notMapped);
			}
		} else {
			List<String> keys = new ArrayList<String>(mapped.keySet());
			Collections.sort(keys);
			Map<String,Object> tmp = new HashMap<String,Object>();
			tmp.put("i", iri);
			tmp.put("k", keys);
			tmp.put("m", mapped);
			mb.adj.put(siri, tmp);
			mb.serialize(this.subjects, this.edges);
			
			if (s.get(dir) == null || JSONLDUtils.compareSerializations(mb.s, (String) ((Map<String,Object>) s.get(dir)).get("s")) <= 0) {
				for (String k: keys) {
					serializeBlankNode(s, (String)mapped.get(k), mb, dir);
				}
				
				mb.serialize(this.subjects, this.edges);
				if (s.get(dir) == null || 
					JSONLDUtils.compareSerializations(mb.s, (String) ((Map<String,Object>) s.get(dir)).get("s")) <= 0 && 
					mb.s.length() >= ((String) ((Map<String,Object>) s.get(dir)).get("s")).length()) {
					tmp = new HashMap<String,Object>();
					tmp.put("s", mb.s);
					tmp.put("m", mb.mapping);
					s.put(dir, tmp);
				}
			}	
		}	
	}

	private int shallowCompareBlankNodes(Map<String, Object> a,	Map<String, Object> b) {
		int rval = 0;
		
		List<String> pA = new ArrayList<String>();
		pA.addAll(a.keySet());
		List<String> pB = new ArrayList<String>();
		pB.addAll(b.keySet());
		
		rval = JSONLDUtils.compare(pA.size(), pB.size());
		
		if (rval == 0) {
			Collections.sort(pA);
			Collections.sort(pB);
			rval = JSONLDUtils.compare(pA, pB);
		}
		
		if (rval == 0) {
			rval = JSONLDUtils.compareBlankNodeObjects(a, b);
		}
		
		if (rval == 0) {
			List<Object> edgesA = (List<Object>) ((Map<String,Object>) ((Map<String,Object>) edges.get("refs")).get(a.get("@id"))).get("all");
			List<Object> edgesB = (List<Object>) ((Map<String,Object>) ((Map<String,Object>) edges.get("refs")).get(b.get("@id"))).get("all");
			rval = JSONLDUtils.compare(edgesA.size(), edgesB.size());
			
			
			if (rval == 0) {
				for (int i = 0; i < edgesA.size() && rval == 0; ++i) {
					rval = compareEdges(edgesA.get(i), edgesB.get(i));
				}
			}
		}
		
		return rval;
	}
	
	private int compareEdges(Object a, Object b) {
		int rval = 0;
		
		boolean bnodeA = JSONLDUtils.isBlankNodeIri(((Map<String,Object>) a).get("s"));
		boolean bnodeB = JSONLDUtils.isBlankNodeIri(((Map<String,Object>) b).get("s"));
		JSONLDUtils.NameGenerator c14n = ngc14n;
		
		if (bnodeA != bnodeB) {
			rval = bnodeA ? 1 : -1;
		} else {
			
			if (!bnodeA) {
				rval = JSONLDUtils.compare(((Map<String,Object>) a).get("s"), ((Map<String,Object>) b).get("s"));
			}
			if (rval == 0) {
				rval = JSONLDUtils.compare(((Map<String,Object>) a).get("p"), ((Map<String,Object>) b).get("p"));
			}
			
			if (rval == 0 && c14n != null) {
				boolean c14nA = c14n.inNamespace((String) ((Map<String,Object>) a).get("s"));
				boolean c14nB = c14n.inNamespace((String) ((Map<String,Object>) b).get("s"));
				
				if (c14nA != c14nB) {
					rval = c14nA ? 1 : -1;
				} else if (c14nA) {
					rval = JSONLDUtils.compare(((Map<String,Object>) a).get("s"), ((Map<String,Object>) b).get("s"));
				}
				
			}
			
		}
		
		return rval;
	}

	/**
	 * TODO: this whole thing should probably be optimized
	 * 
	 * why doesn't Java make using maps and lists easy?!?!?!??!?
	 * 
	 * @author tristan
	 *
	 */
	private class MappingBuilder {

		public MappingBuilder() {
			this.count = 1;
			this.processed = new HashMap<String,Boolean>();
			this.mapping = new HashMap<String, String>();
			this.adj = new HashMap<String, Object>();
			
			// this.keyStack = [{ keys: ['s1'], idx: 0 }];
			this.keyStack = new ArrayList<Object>();
			Map<String,Object> t1 = new HashMap<String, Object>();
			List<String> t2 = new ArrayList<String>();
			t2.add("s1");
			t1.put("keys", t2);
			t1.put("idx", 0);
			keyStack.add(t1);
			this.done = new HashMap<String,Boolean>();
			this.s = "";
		}

		public HashMap<String, Boolean> done;
		public ArrayList<Object> keyStack;
		public String s;
		public Map<String,Object> adj;
		public Map<String,Boolean> processed;
		public int count;
		public Map<String,String> mapping;
		
		/**
		 * Maps the next name to the given bnode IRI if the bnode IRI isn't already in
		 * the mapping. If the given bnode IRI is canonical, then it will be given
		 * a shortened form of the same name.
		 * 
		 * @param iri the blank node IRI to map the next name to.
		 * 
		 * @return the mapped name.
		 */
		public String mapNode(String iri) {
			if (!this.mapping.containsKey(iri)) {
				if (iri.startsWith("_:c14n")) {
					this.mapping.put(iri, "c" + iri.substring(6));
				} else {
					this.mapping.put(iri, "s" + this.count);
					this.count += 1;
				}
			}
			return this.mapping.get(iri);
		}
		public void serialize(Map<String, Object> subjects, Map<String, Object> edges) {
			if (this.keyStack.size() > 0) {
				Map<String, Object> next = (Map<String, Object>)this.keyStack.remove(this.keyStack.size()-1);
				//for (; (Integer)next.get("idx") < ((List<String>) next.get("keys")).size(); next.put("idx", (Integer)next.get("idx")+1)) {
				while ((Integer)next.get("idx") < ((List<String>) next.get("keys")).size()) {
					String k = ((List<String>) next.get("keys")).get((Integer)next.get("idx"));
					if (!this.adj.containsKey(k)) {
						this.keyStack.add(next);
						break;
					}
					next.put("idx", (Integer)next.get("idx")+1);
					
					if (this.done.containsKey(k)) {
						this.s += "_" + k;
					} else {
						this.done.put(k, true);
						
						String s = k;
						Map<String,Object> adj = (Map<String, Object>) this.adj.get(k);
						String iri = (String) adj.get("i");
						if (subjects.containsKey(iri)) {
							Map<String,Object> b = (Map<String, Object>) subjects.get(iri);
							s += "[" + JSONLDUtils.serializeProperties(b) + "]";
							
							Boolean first = true;
							s += "[";
							List<Map<String, Object>> refs = (List<Map<String,Object>>) ((Map<String,Object>) ((Map<String,Object>) edges.get("refs")).get(iri)).get("all");
							for (Map<String,Object> r: refs) {
								if (first) {
									first = false;
								} else {
									s += "|";
								}
								s += "<" + r.get("p") + ">";
								s += JSONLDUtils.isBlankNodeIri(r.get("s")) ? 
										"_:" : ("<" + r.get("s") + ">");
							}
							s += "]";
						}
						
						for (String o: (List<String>)adj.get("k")) {
							s += o;
						}
						this.s += s;
						Map<String, Object> tmp = new HashMap<String,Object>();
						tmp.put("keys", adj.get("k"));
						tmp.put("idx", 0);
						this.keyStack.add(tmp);
						this.serialize(subjects, edges);
					}
				}
			}
		}
		public MappingBuilder copy() {
			MappingBuilder rval = new MappingBuilder();
			rval.count = this.count;
			try {
				rval.processed = (Map<String, Boolean>) JSONLDUtils.clone(this.processed);
				rval.mapping = (Map<String, String>) JSONLDUtils.clone(this.mapping);
				rval.adj = (Map<String, Object>) JSONLDUtils.clone(this.adj);
				rval.keyStack = (ArrayList<Object>) JSONLDUtils.clone(this.keyStack);
				rval.done = (HashMap<String, Boolean>) JSONLDUtils.clone(this.done);
			} catch (CloneNotSupportedException e) {
				// ignoring since this should never happen
			}
			rval.s = this.s;
			return rval;
		}
		
	}

}
