package de.dfki.km.json.jsonld;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.reasoner.rulesys.builtins.IsBNode;

import de.dfki.km.json.jsonld.JSONLDUtils.NameGenerator;

public class JSONLDProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(JSONLDProcessor.class);
    private NameGenerator ngtmp;

    private Set<String> ignoredKeywords = new HashSet<String>();

    /**
     * Tells the processor to skip over the key specified by keyword any time it encounters it. Objects under this key will not be manipulated by any of the
     * processor functions and no triples will be created using it.
     * 
     * @param keyword
     *            The name of the key this processor should ignore.
     */
    public void ignoreKeyword(String keyword) {
        ignoredKeywords.add(keyword);
    }

    private Map<String, Object> edges;
    private Map<String, Object> subjects;
    private Map<String, Object> renamed;
    private Map<String, Object> serializations;
    private NameGenerator ngc14n;

    public static class Options {
        Options() {
            this.base = "";
            this.strict = true;
        }

        Options(String base) {
            this.base = base;
            this.strict = true;
        }

        Options(String base, Boolean strict) {
            this.base = base;
            this.strict = strict;
        }

        public String base = null;
        public Boolean strict = null;
        public Boolean graph = null;
        public Boolean optimize = null;
        public Map<String, Object> optimizeCtx = null;
		public Boolean embed = null;
		public Boolean explicit = null;
    }

    Options opts;

    public JSONLDProcessor() {
        opts = new Options("");
    }

    public JSONLDProcessor(Options opts) {
        if (opts == null) {
            opts = new Options("");
        } else {
            this.opts = opts;
        }
    }

    /**
     * A helper class which still stores all the values in a map
     * but gives member variables easily access certain keys 
     * 
     * @author tristan
     *
     */
    private class ActiveContext extends HashMap<String, Object> {
        public ActiveContext() {
        	super();
        	init();
        }
        
        public ActiveContext(Map<String,Object> copy) {
        	super(copy);
        	init();
        }
        
        private void init() {
        	if (!this.containsKey("mappings")) {
        		this.put("mappings", new HashMap<String,Object>());
        	}
        	if (!this.containsKey("keywords")) {
        		this.put("keywords", new HashMap<String, List<String>>() {
                    {
                        put("@context", new ArrayList<String>());
                        put("@container", new ArrayList<String>());
                        put("@default", new ArrayList<String>());
                        put("@embed", new ArrayList<String>());
                        put("@explicit", new ArrayList<String>());
                        put("@graph", new ArrayList<String>());
                        put("@id", new ArrayList<String>());
                        put("@language", new ArrayList<String>());
                        put("@list", new ArrayList<String>());
                        put("@omitDefault", new ArrayList<String>());
                        put("@preserve", new ArrayList<String>());
                        put("@set", new ArrayList<String>());
                        put("@type", new ArrayList<String>());
                        put("@value", new ArrayList<String>());
                        // add ignored keywords
                        for (String keyword : ignoredKeywords) {
                            put(keyword, new ArrayList<String>());
                        }
                    }
        		});
        	}
        	mappings = (Map<String, Object>) this.get("mappings");
        	keywords = (Map<String, List<String>>) this.get("keywords");
        }

        public Object getContextValue(String key, String type) {
            if (key == null) {
                return null;
            }
            Object rval = null;
            if ("@language".equals(type) && this.containsKey(type)) {
                rval = this.get(type);
            }

            if (this.mappings.containsKey(key)) {
                Map<String, Object> entry = (Map<String, Object>) this.mappings.get(key);

                if (type == null) {
                    rval = entry;
                } else if (entry.containsKey(type)) {
                    rval = entry.get(type);
                }
            }

            return rval;
        }

        public Map<String, Object> mappings;
        public Map<String, List<String>> keywords;
    }

    private class UniqueNamer {
    	private String prefix;
		private int counter;
		private HashMap<String, String> existing;

		/**
		 * Creates a new UniqueNamer. A UniqueNamer issues unique names, keeping
		 * track of any previously issued names.
		 *
		 * @param prefix the prefix to use ('<prefix><counter>').
		 */
		UniqueNamer(String prefix) {
    		this.prefix = prefix;
    		this.counter = 0;
    		this.existing = new HashMap<String,String>();
    	}
		
		/**
		 * Copies this UniqueNamer.
		 *
		 * @return a copy of this UniqueNamer.
		 */
		public UniqueNamer clone() {
			UniqueNamer copy = new UniqueNamer(this.prefix);
			copy.counter = this.counter;
			copy.existing = (HashMap<String, String>) JSONLDUtils.clone(this.existing);
			return copy;
		}
		
		/**
		 * Gets the new name for the given old name, where if no old name is given
		 * a new name will be generated.
		 *
		 * @param [oldName] the old name to get the new name for.
		 *
		 * @return the new name.
		 */
		public String getName(String oldName) {
			if (oldName != null && this.existing.containsKey(oldName)) {
				return this.existing.get(oldName);
			}
			
			String name = this.prefix + this.counter;
			this.counter++;
			
			if (oldName != null) {
				this.existing.put(oldName, name);
			}
			
			return name;
		}
		
		public String getName() {
			return getName(null);
		}
		
		public Boolean isNamed(String oldName) {
			return this.existing.containsKey(oldName);
		}
    }
    
    /**
     * Defines a context mapping during context processing.
     *
     * @param activeCtx the current active context.
     * @param ctx the local context being processed.
     * @param key the key in the local context to define the mapping for.
     * @param base the base IRI.
     * @param defined a map of defining/defined keys to detect cycles and prevent
     *          double definitions.
     */
    private void defineContextMapping(ActiveContext activeCtx, Map<String, Object> ctx, String key, String base, Map<String, Boolean> defined) {
        if (defined.containsKey(key)) {
            if (defined.get(key) == Boolean.TRUE) {
                return;
            }
            throw new RuntimeException("Cyclical context definition detected");
        }
        defined.put(key, Boolean.FALSE);

        String prefix = null;
        int colon = key.indexOf(":");
        if (colon != -1) {
            prefix = key.substring(0, colon);
            if (ctx.containsKey(prefix)) {
                defineContextMapping(activeCtx, ctx, prefix, base, defined);
            }
        }

        Object value = ctx.get(key);

        if (JSONLDUtils.isKeyword(key)) {
            if (!"@language".equals(key)) {
                throw new RuntimeException("Invalid JSON-LD syntax; keywords cannot be overridden");
            }
            if (value != null && !(value instanceof String)) {
                throw new RuntimeException("Invalid JSON-LD syntax; the value of \"@language\" in a @context must be a string or null");
            }
            if (value == null) {
                activeCtx.remove("@language");
            } else {
                activeCtx.put("@language", value);
            }
            defined.put(key, Boolean.TRUE);
            return;
        }

        if (value == null || (value instanceof Map && ((Map<String, Object>) value).containsKey("@id") && ((Map<String, Object>) value).get("@id") == null)) {
            if (activeCtx.mappings.containsKey(key)) {
                String kw = (String) ((Map<String, Object>) activeCtx.mappings.get(key)).get("@id");
                if (JSONLDUtils.isKeyword(kw)) {
                    List<String> aliases = activeCtx.keywords.get(kw);
                    aliases.remove(key);
                }
                activeCtx.mappings.remove(key);
            }
            defined.put(key, Boolean.TRUE);
            return;
        }

        if (value instanceof String) {
            if (JSONLDUtils.isKeyword((String) value)) {
                if ("@context".equals(value) || "@preserve".equals(value)) {
                    throw new RuntimeException("Invalid JSON-LD syntax; @context and @preserve cannot be aliased");
                }

                List<String> aliases = activeCtx.keywords.get(value);
                if (!aliases.contains(key)) {
                    aliases.add(key);
                    Collections.sort(aliases, new Comparator<String>() {
                        // Compares two strings first based on length and then lexicographically
                        public int compare(String a, String b) {
                            if (a.length() < b.length()) {
                                return -1;
                            } else if (b.length() < a.length()) {
                                return 1;
                            }
                            return a.compareTo(b);
                        }
                    });
                }
            } else {
                value = expandContextIri(activeCtx, ctx, (String) value, base, defined);
            }

            Map<String, Object> tmp = new HashMap<String, Object>();
            tmp.put("@id", value);
            activeCtx.mappings.put(key, tmp);
            defined.put(key, Boolean.TRUE);
            return;
        }

        if (!(value instanceof Map)) {
            throw new RuntimeException("Invalid JSON-LD syntax; @context property values must be strings or objects.");
        }

        Map<String, Object> val = (Map<String, Object>) value;
        Map<String, Object> mapping = new HashMap<String, Object>();

        if (val.containsKey("@id")) {
            if (!(val.get("@id") instanceof String)) {
                throw new RuntimeException("Invalid JSON-LD syntax; @context @id values must be strings.");
            }
            String id = (String) val.get("@id");

            if (!"@type".equals(id)) {
                id = expandContextIri(activeCtx, ctx, id, base, defined);
            }

            mapping.put("@id", id);
        } else {
            // non-IRIs *must* define @ids
            if (prefix == null) {
                throw new RuntimeException("Invalid JSON-LD syntax; @context terms must define an @id.");
            }

            if (activeCtx.mappings.containsKey(prefix)) {
                String suffix = key.substring(colon + 1);
                mapping.put("@id", (String) ((Map<String, Object>) activeCtx.mappings.get(prefix)).get("@id") + suffix);
            } else {
                mapping.put("@id", key);
            }
        }

        if (val.containsKey("@type")) {
            if (!(val.get("@type") instanceof String)) {
                throw new RuntimeException("Invalid JSON-LD syntax; @context @type values must be strings.");
            }
            String type = (String) val.get("@type");
            if (!"@id".equals(type)) {
                type = expandContextIri(activeCtx, ctx, type, "", defined);
            }

            mapping.put("@type", type);
        }

        if (val.containsKey("@container")) {
            Object container = val.get("@container");
            if (!("@list".equals(container) || "@set".equals(container))) {
                throw new RuntimeException("Invalid JSON-LD syntax; @context @container value must be \"@list\" or \"@set\".");
            }
            mapping.put("@container", container);
        }

        if (val.containsKey("@language")) {
            Object lang = val.get("@language");
            if (lang != null && !(lang instanceof String)) {
                throw new RuntimeException("Invalid JSON-LD syntax; @context @language must be a string or null.");
            }
            mapping.put("@language", lang);
        }

        if (prefix != null && activeCtx.mappings.containsKey(prefix)) {
            Map<String, Object> child = mapping;
            mapping = (Map<String, Object>) JSONLDUtils.clone(activeCtx.mappings.get(prefix));
            for (String k : child.keySet()) {
                mapping.put(k, child.get(k));
            }
        }

        activeCtx.mappings.put(key, mapping);
        defined.put(key, Boolean.TRUE);
    }

    /**
     * Expands a string value to a full IRI during context processing. It can
     * be assumed that the value is not a keyword.
     *
     * @param activeCtx the current active context.
     * @param ctx the local context being processed.
     * @param value the string value to expand.
     * @param base the base IRI.
     * @param defined a map for tracking cycles in context definitions.
     *
     * @return the expanded value.
     */
    private String expandContextIri(ActiveContext activeCtx, Map<String, Object> ctx, String value, String base, Map<String, Boolean> defined) {
        if (ctx.containsKey(value) && defined.get(value) != Boolean.TRUE) {
            defineContextMapping(activeCtx, ctx, value, base, defined);
        }

        if (activeCtx.mappings.containsKey(value)) {
            String id = ((Map<String, String>) activeCtx.mappings.get(value)).get("@id");
            if (id != null && id.equals(value)) {
                return value;
            }
            return expandContextIri(activeCtx, ctx, id, base, defined);
        }

        int colon = value.indexOf(':');
        if (colon != -1) {
            String prefix = value.substring(0, colon);
            String suffix = value.substring(colon + 1);

            // indicates the value is a blank node
            if ("_".equals(prefix)) {
                return value;
            }

            // indicates the value is an absolute IRI
            if (suffix.startsWith("//")) {
                return value;
            }

            // dependency not defined, define it
            if (ctx.containsKey(prefix) && defined.get(prefix) != Boolean.TRUE) {
                defineContextMapping(activeCtx, ctx, prefix, base, defined);
            }

            // recurse if prefix is defined
            if (activeCtx.mappings.containsKey(prefix)) {
                String id = ((Map<String, String>) activeCtx.mappings.get(prefix)).get("@id");
                return expandContextIri(activeCtx, ctx, id, base, defined) + suffix;
            }

            // consider the value an absolute IRI
            return value;
        }

        if ("".equals(value) || value.startsWith("#")) {
            value = base + value;
        } else {
            value = base.substring(0, base.lastIndexOf('/') + 1) + value;
        }

        if (!JSONLDUtils.isAbsoluteIri(value)) {
            throw new RuntimeException("Invalid JSON-LD syntax; a @context value does not expand to an absolute IRI.");
        }

        return value;
    }

    /**
     * Processes a local context, resolving any URLs as necessary, and returns a
     * new active context in its callback.
     *
     * @param activeCtx the current active context.
     * @param localCtx the local context to process.
     * @param [options] the options to use:
     *          [resolver(url, callback(err, jsonCtx))] the URL resolver to use.
     * @param callback(err, ctx) called once the operation completes.
     */
    public ActiveContext processContext(ActiveContext activeCtx, Object localCtx) {
        ActiveContext rval = (ActiveContext) JSONLDUtils.clone(activeCtx);

        if (localCtx instanceof Map && ((Map) localCtx).containsKey("@context") && ((Map) localCtx).get("@context") instanceof List) {
            localCtx = ((Map) localCtx).get("@context");
        }

        List<Map<String, Object>> ctxs;
        if (localCtx instanceof List) {
            ctxs = (List<Map<String, Object>>) localCtx;
        } else {
            ctxs = new ArrayList<Map<String, Object>>();
            ctxs.add((Map<String, Object>) localCtx);
        }

        try {
            for (Map<String, Object> ctx : ctxs) {
                if (ctx == null) {
                    // reset to initial context
                    rval = new ActiveContext();
                    continue;
                }

                if (ctx.containsKey("@context")) {
                    ctx = (Map<String, Object>) ctx.get("@context");
                }

                HashMap<String, Boolean> defined = new HashMap<String, Boolean>();
                for (String key : ctx.keySet()) {
                    defineContextMapping(rval, ctx, key, opts.base, defined);
                }
            }
        } catch (ClassCastException e) {
            throw new RuntimeException("@context must be an object");
        }

        return rval;
    }

    /**
     * Processes a local context and returns a new active context.
     *
     * @param activeCtx the current active context.
     * @param localCtx the local context to process.
     * @param options the context processing options.
     *
     * @return the new active context.
     */
    private static ActiveContext processContext(ActiveContext activeCtx, Object localCtx, Options opts) {
        JSONLDProcessor p = new JSONLDProcessor(opts);
        if (localCtx == null) {
            return p.new ActiveContext();
        }

        localCtx = JSONLDUtils.clone(localCtx);
        if (localCtx instanceof Map && !((Map) localCtx).containsKey("@context")) {
            Map<String, Object> tmp = new HashMap<String, Object>();
            tmp.put("@context", localCtx);
            localCtx = tmp;
        }
        return p.processContext(activeCtx, localCtx);
    }

    /**
     * Expands a term into an absolute IRI. The term may be a regular term, a
     * prefix, a relative IRI, or an absolute IRI. In any case, the associated
     * absolute IRI will be returned.
     *
     * @param ctx the active context to use.
     * @param term the term to expand.
     * @param base the base IRI to use if a relative IRI is detected.
     *
     * @return the expanded term as an absolute IRI.
     */
    private String expandTerm(ActiveContext ctx, String term, String base) {
        if (term == null) {
            return null;
        }

        if (ctx.mappings.containsKey(term)) {
            String id = (String) ((Map<String, Object>) ctx.mappings.get(term)).get("@id");
            if (term.equals(id)) {
                return term;
            }
            return expandTerm(ctx, id, base);
        }

        int colon = term.indexOf(':');
        if (colon != -1) {
            String prefix = term.substring(0, colon);
            String suffix = term.substring(colon + 1);

            // indicates the value is a blank node
            if ("_".equals(prefix)) {
                return term;
            }

            // indicates the value is an absolute IRI
            if (suffix.startsWith("//")) {
                return term;
            }

            if (ctx.mappings.containsKey(prefix)) {
                return expandTerm(ctx, (String) ((Map<String, Object>) ctx.mappings.get(prefix)).get("@id"), base) + suffix;
            }

            return term;
        }

        if (base != null) {
            if ("".equals(term) || term.startsWith("#")) {
                term = base + term;
            } else {
                term = base.substring(0, base.lastIndexOf('/') + 1) + term;
            }
        }

        return term;
    }

    private String expandTerm(ActiveContext ctx, String term) {
        return expandTerm(ctx, term, null);
    }

    /**
     * Expands the given value by using the coercion and keyword rules in the
     * given context.
     *
     * @param ctx the active context to use.
     * @param property the property the value is associated with.
     * @param value the value to expand.
     * @param base the base IRI to use.
     *
     * @return the expanded value.
     */
    private Object expandValue(ActiveContext ctx, String property, Object value, String base) {
        if (value == null) {
            return null;
        }
        Object rval = value;
        String prop = expandTerm(ctx, property);
        if ("@id".equals(prop)) {
            rval = expandTerm(ctx, (String) value, base);
        } else if ("@type".equals(prop)) {
            rval = expandTerm(ctx, (String) value);
        } else {
            Object type = ctx.getContextValue(property, "@type");

            if ("@id".equals(type) || "@graph".equals(prop)) {
                Map<String, Object> tmp = new HashMap<String, Object>();
                tmp.put("@id", expandTerm(ctx, (String) value, base));
                rval = tmp;
            } else if (!JSONLDUtils.isKeyword(prop)) {
                Map<String, Object> tmp = new HashMap<String, Object>();
                tmp.put("@value", value);
                rval = tmp;
                if (type != null) {
                    tmp.put("@type", type);
                } else {
                    Object language = ctx.getContextValue(property, "@language");
                    if (language != null) {
                        tmp.put("@language", language);
                    }
                }
            }
        }
        return rval;
    }

    /**
     * Throws an exception if the given value is not a valid @type value.
     *
     * @param v the value to check.
     */
    private void validateTypeValue(Object v) {
        if (v instanceof String || (v instanceof Map && (((Map<String, Object>) v).containsKey("@id") || ((Map<String, Object>) v).size() == 0))) {
            return;
        }

        boolean isValid = false;
        if (v instanceof List) {
            isValid = true;
            for (Object i : (List) v) {
                if (!(i instanceof String || i instanceof Map && ((Map<String, Object>) i).containsKey("@id"))) {
                    isValid = false;
                    break;
                }
            }
        }

        if (!isValid) {
            throw new RuntimeException(
                    "Invalid JSON-LD syntax; \"@type\" value must a string, a subject reference, an array of strings or subject references, or an empty object.");
        }
    }

    /**
     * Compacts an IRI or keyword into a term or prefix if it can be. If the
     * IRI has an associated value it may be passed.
     *
     * @param ctx the active context to use.
     * @param iri the IRI to compact.
     * @param value the value to check or null.
     *
     * @return the compacted term, prefix, keyword alias, or the original IRI.
     */
    private static String compactIri(ActiveContext ctx, String iri, Object value) {
        if (iri == null) {
            return iri;
        }

        if (JSONLDUtils.isKeyword(iri)) {
            List<String> aliases = ctx.keywords.get(iri);
            if (aliases.size() > 0) {
                return aliases.get(0);
            } else {
                return iri;
            }
        }

        List<String> terms = new ArrayList<String>();
        int highest = 0;
        boolean listContainer = false;
        boolean isList = (value instanceof Map && ((Map<String, Object>) value).containsKey("@list"));
        for (String term : ctx.mappings.keySet()) {
            Map<String, Object> entry = (Map<String, Object>) ctx.mappings.get(term);
            if (!iri.equals(entry.get("@id"))) {
                continue;
            }
            if (isList && "@set".equals(entry.get("@container"))) {
                continue;
            }
            if (!isList && "@list".equals(entry.get("@container"))) {
                continue;
            }
            if (isList && listContainer && !"@list".equals(entry.get("@container"))) {
                continue;
            }

            int rank = rankTerm(ctx, term, value);
            if (rank > 0) {
                if ("@set".equals(entry.get("@container"))) {
                    rank += 1;
                }

                if (isList && !listContainer && "@list".equals(entry.get("@container"))) {
                    listContainer = true;
                    terms.clear();
                    highest = rank;
                    terms.add(term);
                } else if (rank >= highest) {
                    if (rank > highest) {
                        terms.clear();
                        highest = rank;
                    }
                    terms.add(term);
                }
            }
        }

        if (terms.size() == 0) {
            for (String term : ctx.mappings.keySet()) {
                if (term.contains(":")) {
                    continue;
                }

                Map<String, Object> entry = (Map<String, Object>) ctx.mappings.get(term);
                if (iri.equals(entry.get("@id")) || iri.indexOf((String) entry.get("@id")) != 0) {
                    continue;
                }

                String curie = term + ":" + iri.substring(((String) entry.get("@id")).length());
                if (!(ctx.mappings.containsKey(curie))) {
                    terms.add(curie);
                }
            }
        }

        if (terms.size() == 0) {
            return iri;
        }

        Collections.sort(terms, new Comparator<String>() {
            // Compares two strings first based on length and then lexicographically
            public int compare(String a, String b) {
                if (a.length() < b.length()) {
                    return -1;
                } else if (b.length() < a.length()) {
                    return 1;
                }
                return a.compareTo(b);
            }
        });

        return terms.get(0);
    }

    private static String compactIri(ActiveContext ctx, String iri) {
        return compactIri(ctx, iri, null);
    }

    /**
     * Ranks a term that is possible choice for compacting an IRI associated with
     * the given value.
     *
     * @param ctx the active context.
     * @param term the term to rank.
     * @param value the associated value.
     *
     * @return the term rank.
     */
    private static int rankTerm(ActiveContext ctx, String term, Object value) {
        if (value == null) {
            return 3;
        }

        Map<String, Object> entry = (Map<String, Object>) ctx.mappings.get(term);
        if (value instanceof Map && ((Map<String, Object>) value).containsKey("@list")) {
            List<Object> list = (List<Object>) ((Map<String, Object>) value).get("@list");
            if (list.size() == 0) {
                return "@list".equals(entry.get("@container")) ? 1 : 0;
            }
            int sum = 0;
            for (Object i : list) {
                sum += rankTerm(ctx, term, i);
            }
            return sum;
        }

        if (value instanceof Map && ((Map<String, Object>) value).containsKey("@value")) {
            if (((Map<String, Object>) value).containsKey("@type")) {
                if (entry.containsKey("@type")) {
                    Object vt = ((Map<String, Object>) value).get("@type");
                    Object et = entry.get("@type");
                    if ((vt == null && et == null) || (vt != null && vt.equals(et))) {
                        return 3;
                    }
                }
                return (!entry.containsKey("@type") && !entry.containsKey("@language")) ? 1 : 0;
            }

            if (!(((Map<String, Object>) value).get("@value") instanceof String)) {
                return (!entry.containsKey("@type") && !entry.containsKey("@language")) ? 2 : 1;
            }

            if (!((Map<String, Object>) value).containsKey("@language")) {
                if ((entry.containsKey("@language") && entry.get("@language") == null)
                        || (!entry.containsKey("@type") && !entry.containsKey("@language") && !ctx.containsKey("@language"))) {
                    return 3;
                }
                return 0;
            }

            Object vl = ((Map<String, Object>) value).get("@language");
            Object el = entry.get("@language");
            Object cl = ctx.get("@language");
            if ((entry.containsKey("@language") && ((vl == null && el == null) || vl.equals(el)))
                    || (!entry.containsKey("@type") && !entry.containsKey("@language") && (ctx.containsKey("@language") && ((vl == null && cl == null) || vl
                            .equals(cl))))) {
                return 3;
            }
            return (!entry.containsKey("@type") && !entry.containsKey("@language")) ? 1 : 0;
        }

        if ("@id".equals(entry.get("@type"))) {
            return 3;
        }

        return (!entry.containsKey("@type") && !entry.containsKey("@language")) ? 1 : 0;
    }

    /**
     * Recursively expands an element using the given context. Any context in
     * the element will be removed. All context URLs must have been resolved
     * before calling this method.
     *
     * @param ctx the context to use.
     * @param property the property for the element, null for none.
     * @param element the element to expand.
     * 
     * TODO:
     * @param options the expansion options.
     * @param propertyIsList true if the property is a list, false if not.
     *
     * @return the expanded value.
     */
    private Object expand(ActiveContext ctx, Object property, Object element) {
        if (element instanceof List) {
            List<Object> rval = new ArrayList<Object>();
            for (Object i : (List<Object>) element) {
                Object e = expand(ctx, property, i);
                if (e instanceof List && "@list".equals(property)) {
                    throw new RuntimeException("Invalid JSON-LD syntax; lists of lists are not permitted.");
                } else if (e != null) {
                    // drop null values
                    rval.add(e);
                }
            }
            return rval;
        }
        if (element instanceof Map) {
            Map<String, Object> elem = (Map<String, Object>) element;
            if (elem.containsKey("@context")) {
                ctx = processContext(ctx, elem.get("@context"));
                elem.remove("@context");
            }

            Map<String, Object> rval = new HashMap<String, Object>();

            for (String key : elem.keySet()) {
                String prop = expandTerm(ctx, key);

                if (!JSONLDUtils.isAbsoluteIri(prop) && !JSONLDUtils.isKeyword(prop, ctx)) {
                    continue;
                }

                Object value = elem.get(key);
                if (value == null && !"@value".equals(prop)) {
                    continue;
                }

                if ("@id".equals(prop) && !(value instanceof String)) {
                    throw new RuntimeException("Invalid JSON-LD syntax; \"@id\" value must a string.");
                }

                if ("@type".equals(prop)) {
                    validateTypeValue(value);
                }

                if ("@graph".equals(prop) && !(value instanceof Map || value instanceof List)) {
                    throw new RuntimeException("Invalid JSON-LD syntax; \"@graph\" value must be an object or an array.");
                }

                if ("@value".equals(prop) && (value instanceof Map || value instanceof List)) {
                    throw new RuntimeException("Invalid JSON-LD syntax; \"@value\" value must not be an object or an array.");
                }

                if ("@language".equals(prop) && !(value instanceof String)) {
                    throw new RuntimeException("Invalid JSON-LD syntax; \"@language\" value must be a string.");
                }

                // 2.2.7
                if ("@list".equals(prop) || "@set".equals(prop)) {
                    value = expand(ctx, property, value);
                    if ("@list".equals(prop) && (value instanceof Map && ((Map<String, Object>) value).containsKey("@list"))) {
                        throw new RuntimeException("Invalid JSON-LD syntax; lists of lists are not permitted.");
                    }
                } else {
                    // 2.2.8
                    property = key;
                    value = expand(ctx, property, value);
                }

                if (value != null || "@value".equals(prop)) {
                    if (!"@list".equals(prop) && !(value instanceof Map && ((Map<String, Object>) value).containsKey("@list"))) {
                        Object container = ctx.getContextValue((String) property, "@container");
                        if ("@list".equals(container)) {
                            Map<String, Object> tmp = new HashMap<String, Object>();
                            List<Object> tl;
                            if (value instanceof List) {
                                tl = (List) value;
                            } else {
                                tl = new ArrayList<Object>();
                                tl.add(value);
                            }
                            tmp.put("@list", tl);
                            value = tmp;
                        }
                    }

                    if ("@type".equals(prop)) {
                        if (value instanceof Map && ((Map<String, Object>) value).containsKey("@id")) {
                            value = ((Map<String, Object>) value).get("@id");
                        } else if (value instanceof List) {
                            List<Object> val = new ArrayList<Object>();
                            for (Object v : (List) value) {
                                if (v instanceof Map && ((Map<String, Object>) v).containsKey("@id")) {
                                    val.add(((Map<String, Object>) v).get("@id"));
                                } else {
                                    val.add(v);
                                }
                            }
                            value = val;
                        }
                    }

                    boolean useArray = !("@id".equals(prop) || "@type".equals(prop) || "@value".equals(prop) || "@language".equals(prop));
                    JSONLDUtils.addValue(rval, prop, value, useArray);
                }
            }

            if (rval.containsKey("@value")) {
                if ((rval.size() == 2 && !rval.containsKey("@type") && !rval.containsKey("@language")) || rval.size() > 2) {
                    throw new RuntimeException(
                            "Invalid JSON-LD syntax; an element containing \"@value\" must have at most one other property which can be \"@type\" or \"@language\".");
                }
                if (rval.containsKey("@type") && !(rval.get("@type") instanceof String)) {
                    throw new RuntimeException("Invalid JSON-LD syntax; the \"@type\" value of an element containing \"@value\" must be a string.");
                }
                if (rval.get("@value") == null) {
                    rval = null;
                }
            } else if (rval.containsKey("@type") && !(rval.get("@type") instanceof List)) {
                List<Object> tmp = new ArrayList<Object>();
                tmp.add(rval.get("@type"));
                rval.put("@type", tmp);
            } else if (rval.containsKey("@set") || rval.containsKey("@list")) {
                if (rval.size() != 1) {
                    throw new RuntimeException(
                            "Invalid JSON-LD syntax; if an element has the property \"@set\" or \"@list\", then it must be its only property.");
                }
                if (rval.containsKey("@set")) {
                    return rval.get("@set");
                }
            } else if (rval.containsKey("@language") && rval.size() == 1) {
                rval = null;
            }

            return rval;
        }

        return expandValue(ctx, (String) property, element, opts.base);
    }

    /**
     * Performs JSON-LD expansion.
     *
     * @param input the JSON-LD input to expand.
     */
    public static Object expand(Object input, Options opts) {
        JSONLDProcessor p = new JSONLDProcessor(opts);
        Object expanded = p.expand(p.new ActiveContext(), null, input);

        if (expanded instanceof Map && ((Map) expanded).containsKey("@graph") && ((Map) expanded).size() == 1) {
            expanded = ((Map<String, Object>) expanded).get("@graph");
        }

        if (!(expanded instanceof List)) {
            List<Object> tmp = new ArrayList<Object>();
            tmp.add(expanded);
            expanded = tmp;
        }
        return expanded;
    }

    public static Object expand(Object input) {
        return expand(input, new Options(""));
    }

    /**
     * Recursively compacts an element using the given active context. All values
     * must be in expanded form before this method is called.
     *
     * @param ctx the active context to use.
     * @param property the property that points to the element, null for none.
     * @param element the element to compact.
     *
     * @return the compacted value.
     */
    public Object compact(ActiveContext ctx, String property, Object element) {

        if (element instanceof List) {
            List<Object> rval = new ArrayList<Object>();
            for (Object i : (List) element) {
                Object e = compact(ctx, property, i);
                if (e != null) {
                    rval.add(e);
                }
            }
            if (rval.size() == 1) {
                Object container = ctx.getContextValue(property, "@container");
                if (!("@list".equals(container) || "@set".equals(container))) {
                    return rval.get(0);
                }
            }
            return rval;
        }

        if (element instanceof Map) {
            Map<String, Object> elem = (Map<String, Object>) element;
            if (elem.containsKey("@value")) {
                if (elem.size() == 1) {
                    return elem.get("@value");
                }

                Object type = ctx.getContextValue(property, "@type");
                Object language = ctx.getContextValue(property, "@language");

                if (type != null && elem.containsKey("@type") && type.equals(elem.get("@type"))) {
                    element = elem.get("@value");
                } else if (language != null && elem.containsKey("@language") && language.equals(elem.get("@language"))) {
                    element = elem.get("@value");
                } else if (elem.containsKey("@type")) {
                    elem.put("@type", compactIri(ctx, (String) elem.get("@type")));
                }

                return element;
            }

            // compact subject references
            if (elem.size() == 1 && elem.containsKey("@id")) {
                Object type = ctx.getContextValue(property, "@type");
                if ("@id".equals(type) || "@graph".equals(property)) {
                    return compactIri(ctx, (String) elem.get("@id"));
                }
            }

            Map<String, Object> rval = new HashMap<String, Object>();
            for (String key : elem.keySet()) {
                Object value = elem.get(key);
                if ("@id".equals(key) || "@type".equals(key)) {
                    if (value instanceof String) {
                        value = compactIri(ctx, (String) value);
                    } else {
                        List<String> types = new ArrayList<String>();
                        for (String i : (List<String>) value) {
                            types.add(compactIri(ctx, i));
                        }
                        value = types;
                    }

                    String prop = compactIri(ctx, key);
                    JSONLDUtils.addValue(rval, prop, value, value instanceof List && ((List) value).size() == 0);
                    continue;
                }

                // NOTE: value must be an array due to expansion algorithm

                if (((List) value).size() == 0) {
                    String prop = compactIri(ctx, key);
                    JSONLDUtils.addValue(rval, prop, new ArrayList<Object>(), true);
                }

                for (Object v : (List) value) {
                    boolean isList = (v instanceof Map && ((Map<String, Object>) v).containsKey("@list"));
                    String prop = compactIri(ctx, key, v);

                    if (isList) {
                        v = ((Map<String, Object>) v).get("@list");
                    }

                    v = compact(ctx, prop, v);

                    Object container = ctx.getContextValue(prop, "@container");

                    if (isList && !"@list".equals(container)) {
                        if (rval.containsKey(prop) && opts.strict) {
                            throw new RuntimeException(
                                    "JSON-LD compact error; property has a \"@list\" @container rule but there is more than a single @list that matches the compacted term in the document. Compaction might mix unwanted items into the list.");
                        }
                        String kwlist = compactIri(ctx, "@list");
                        Map<String, Object> val = new HashMap<String, Object>();
                        val.put(kwlist, v);
                        v = val;

                    }
                    boolean isArray = ("@set".equals(container) || "@list".equals(container) || (v instanceof List && ((List) v).size() == 0));
                    JSONLDUtils.addValue(rval, prop, v, isArray, "@list".equals(container));
                }
            }
            return rval;
        }
        return element;
    }

    /**
     * Performs JSON-LD compaction.
     *
     * @param input the JSON-LD input to compact.
     * @param ctx the context to compact with.
     * @param [options] options to use:
     *          [base] the base IRI to use.
     *          [strict] use strict mode (default: true).
     *          [optimize] true to optimize the compaction (default: false).
     *          [graph] true to always output a top-level graph (default: false).
     *          [resolver(url, callback(err, jsonCtx))] the URL resolver to use.
     * @param callback(err, compacted, ctx) called once the operation completes.
     */
    public static Object compact(Object input, Object ctx, Options opts) {
        if (input == null) {
            return null;
        }
        if (opts.strict == null) {
            opts.strict = true;
        }
        if (opts.graph == null) {
            opts.graph = false;
        }
        if (opts.optimize == null) {
            opts.optimize = false;
        }
        JSONLDProcessor p = new JSONLDProcessor(opts);

        // expand input then do compaction
        Object expanded;
        try {
            expanded = p.expand(p.new ActiveContext(), null, input);
        } catch (RuntimeException e) {
            throw new RuntimeException("Count not expand input before compaction", e);
        }

        // process context
        ActiveContext activeCtx = p.new ActiveContext();
        try {
            activeCtx = JSONLDProcessor.processContext(activeCtx, ctx, opts);
        } catch (RuntimeException e) {
            throw new RuntimeException("Could not process context before compaction.", e);
        }

        if (opts.optimize) {
            opts.optimizeCtx = new HashMap<String, Object>();
        }

        // do compaction
        Object compacted = p.compact(activeCtx, null, expanded);

        // cleanup
        if (!opts.graph && compacted instanceof List && ((List<Object>) compacted).size() == 1) {
            compacted = ((List<Object>) compacted).get(0);
        } else if (opts.graph && compacted instanceof Map) {
            List<Object> tmp = new ArrayList<Object>();
            tmp.add(compacted);
            compacted = tmp;
        }

        if (ctx instanceof Map && ((Map<String, Object>) ctx).containsKey("@context")) {
            ctx = ((Map<String, Object>) ctx).get("@context");
        }

        ctx = JSONLDUtils.clone(ctx);
        if (!(ctx instanceof List)) {
            List<Object> lctx = new ArrayList<Object>();
            lctx.add(ctx);
            ctx = lctx;
        }
        // TODO: i need some cases where ctx is a list!

        if (opts.optimize) {
            ((List<Object>) ctx).add(opts.optimizeCtx);
        }

        List<Object> tmp = (List<Object>) ctx;
        ctx = new ArrayList<Object>();
        for (Object i : tmp) {
            if (!(i instanceof Map) || ((Map) i).size() > 0) {
                ((List<Object>) ctx).add(i);
            }
        }

        boolean hasContext = ((List) ctx).size() > 0;
        if (((List) ctx).size() == 1) {
            ctx = ((List) ctx).get(0);
        }

        if (hasContext || opts.graph) {
            if (compacted instanceof List) {
                String kwgraph = p.compactIri(activeCtx, "@graph");
                Object graph = compacted;
                compacted = new HashMap<String, Object>();
                if (hasContext) {
                    ((Map<String, Object>) compacted).put("@context", ctx);
                }
                ((Map<String, Object>) compacted).put(kwgraph, graph);
            } else if (compacted instanceof Map) {
                Map<String, Object> graph = (Map<String, Object>) compacted;
                compacted = new HashMap<String, Object>();
                ((Map) compacted).put("@context", ctx);
                for (String key : graph.keySet()) {
                    ((Map<String, Object>) compacted).put(key, graph.get(key));
                }
            }
        }

        return compacted;
    }

    public static Object compact(Object input, Map<String, Object> ctx) {
        return compact(input, ctx, new Options("", true));
    }

    private class FramingContext {
    	public Map<String,Object> embeds = null;
    	public Map<String,Object> graphs = null;
    	public Map<String,Object> subjects = null;
    	public Boolean embed = true;
    	public Boolean explicit = false;
    	public Boolean omit = false;
    	public Options options = opts;
    }
    
    /**
     * Performs JSON-LD framing.
     *
     * @param input the expanded JSON-LD to frame.
     * @param frame the expanded JSON-LD frame to use.
     * @param options the framing options.
     *
     * @return the framed output.
     */
    public Object frame(Object input, Object frame) {
    	// create framing state
    	FramingContext state = new FramingContext();
    	//Map<String,Object> state = new HashMap<String, Object>();
    	//state.put("options", this.opts);
    	state.graphs = new HashMap<String, Object>();
    	state.graphs.put("@default", new HashMap<String, Object>());
    	state.graphs.put("@merged", new HashMap<String, Object>());
    	
    	// produce a map of all graphs and name each bnode
    	UniqueNamer namer = new UniqueNamer("_:t");
    	flatten(input, state.graphs, "@default", namer);
    	namer = new UniqueNamer("_:t");
    	flatten(input, state.graphs, "@merged", namer);
    	// FIXME: currently uses subjects from @merged graph only
    	state.subjects = (Map<String, Object>) state.graphs.get("@merged");
    	
    	// frame the subjects
        List framed = new ArrayList();
        frame(state, state.subjects.keySet(), frame, framed, null);
    	return framed;
    }

    /**
     * Frames subjects according to the given frame.
     *
     * @param state the current framing state.
     * @param subjects the subjects to filter.
     * @param frame the frame.
     * @param parent the parent subject or top-level array.
     * @param property the parent property, initialized to null.
     */
    private static void frame(FramingContext state, Collection<String> subjects,
			Object frame, Object parent, Object property) {
		// validate the frame
    	validateFrame(state, frame);
    	frame(state, subjects, (Map)((List)frame).get(0), parent, property);
    }
    
    private static void frame(FramingContext state, Collection<String> subjects, 
    		Map<String,Object> frame, Object parent, Object property) {
    	// filter out subjects that match the frame
    	Map<String,Object> matches = filterSubjects(state, subjects, frame);
    	
    	Options options = state.options;
    	Boolean embedOn = (frame.containsKey("@embed")) ? (Boolean)((List)frame.get("@embed")).get(0) : options.embed;
    	Boolean explicicOn = (frame.containsKey("@explicit")) ? (Boolean)((List)frame.get("@explicit")).get(0) : options.explicit;
    	
    	// add matches to output
    	for (String id: matches.keySet()) {
    		
    		// Note: In order to treat each top-level match as a compartmentalized
    	    // result, create an independent copy of the embedded subjects map when the
    	    // property is null, which only occurs at the top-level.
    		if (property == null) {
    			state.embeds = new HashMap<String,Object>();
    		}
    		
    		// start output
    		Map<String,Object> output = new HashMap<String,Object>();
    		output.put("@id", id);
    		
    		// prepare embed meta info
    		Map<String,Object> embed = new HashMap<String, Object>();
    		embed.put("parent", parent);
    		embed.put("property", property);
    		
    		if (embedOn && state.embeds.containsKey(id)) {
    			embedOn = false;
    			
    			Map<String,Object> existing = (Map<String, Object>) state.embeds.get(id);
    			if (existing.get("parent") instanceof List) {
    				for (Object o: (List)existing.get("parent")) {
    					if (JSONLDUtils.compareValues(output, o)) {
    						embedOn = true;
    						break;
    					}
    				}
    			// existing embed's parent is an object
    			} else if (JSONLDUtils.hasValue((Map<String,Object>)existing.get("parent"), (String)existing.get("property"), output)) {
    				embedOn = true;
    			}
    			
    			// existing embed has already been added, so allow an overwrite
    			if (embedOn) {
    				removeEmbed(state, id);
    			}
    		}
    		
    		if (!embedOn) {
    			addFrameOutput(state, parent, property, output);
    		} else {
    			// add embed meta info
    			state.embeds.put(id, embed);
    			
    			// iterate over subject properties
    			Map<String,Object> subject = (Map<String,Object>) matches.get(id);
    			for (Object prop: subject.values()) {
    				
    				// copy keywords to output
    				if (prop instanceof String && JSONLDUtils.isKeyword((String)prop)) {
    					output.put((String) prop, JSONLDUtils.clone(subject.get(prop)));
    					continue;
    				}
    				
    				// if property isn't in the frame
    				if (!frame.containsKey(prop)) {
    					// if explicit is off, embed values
    					if (!explicicOn) {
    						embedValues(state, subject, prop, output);
    					}
    					continue;
    				}
    				
    				// add objects
    				Object objects = subject.get(prop); 
    				// TODO: i've done some crazy stuff here because i'm unsure if objects is always a list or if it can
    				// be a map as well
    				for (Object i: objects instanceof List ? (List)objects : ((Map)objects).keySet()) {
    					Object o = objects instanceof List ? ((List)objects).get((Integer)i) : ((Map)objects).get(i);
    					
    					if (o instanceof Map && ((Map)o).containsKey("@list")) {
    						// add empty list
    						Map<String,Object> list = new HashMap<String, Object>();
    						list.put("@list", new ArrayList<Object>());
    						addFrameOutput(state, output, prop, list);
    						
    						// add list objects
    						List src = (List)((Map)o).get("@list");
    						for (Object n: src) {
    							// recurse into subject reference
    							if (n instanceof Map && ((Map)n).size() == 1 && ((Map) n).containsKey("@id")) {
    								List tmp = new ArrayList();
    								tmp.add(((Map)n).get("@id"));
    								frame(state, tmp, frame.get(prop), list, "@list");
    							} else {
    								// include other values automatcially
    								addFrameOutput(state, list, "@list", JSONLDUtils.clone(n));
    							}
    						}
    						continue;
    					}
    					
    					// recurse into subject reference
    					if (o instanceof Map && ((Map)o).size() == 1 && ((Map) o).containsKey("@id")) {
    						List tmp = new ArrayList();
							tmp.add(((Map)o).get("@id"));
							frame(state, tmp, frame.get(prop), output, prop);
    					} else {
    						// include other values automatically
    						addFrameOutput(state, output, prop, JSONLDUtils.clone(o));
    					}
    				}
    			}
    			
    			// handle defaults
    			
    			// TODO: continue impl here
    		}
    	}

	}

    /**
     * Returns a map of all of the subjects that match a parsed frame.
     *
     * @param state the current framing state.
     * @param subjects the set of subjects to filter.
     * @param frame the parsed frame.
     *
     * @return all of the matched subjects.
     */
    private static Map<String,Object> filterSubjects(FramingContext state,
			Collection<String> subjects, Map<String,Object> frame) {
    	// filter subjects in @id order
		Map<String,Object> rval = new HashMap<String,Object>();
		for (String id: subjects) {
			Map<String,Object> subject = (Map<String, Object>) state.subjects.get(id);
			if (filterSubject(subject, frame)) {
				rval.put(id, subject);
			}
		}
		return rval;
	}

    /**
     * Returns true if the given subject matches the given frame.
     *
     * @param subject the subject to check.
     * @param frame the frame to check.
     *
     * @return true if the subject matches, false if not.
     */
	private static boolean filterSubject(Map<String,Object> subject, Map<String,Object> frame) {
		// check @type (object value means 'any' type, fall through to ducktyping)
		Object t = frame.get("@type");
		// TODO: it seems @type should always be a list
		if (frame.containsKey("@type") && !(t instanceof List && ((List)t).size() == 1 && ((List)t).get(0) instanceof Map)) {
			for (Object i: (List)t) {
				if (JSONLDUtils.hasValue(subject, "@type", i)) {
					return true;
				}
			}
			return false;
		}
		
		// check ducktype
		for (String key: frame.keySet()) {
			if ("@id".equals(key) || !JSONLDUtils.isKeyword(key) && !(subject.containsKey(key))) {
				return false;
			}
		}
		return true;
	}

	/**
     * Validates a JSON-LD frame, throwing an exception if the frame is invalid.
     *
     * @param state the current frame state.
     * @param frame the frame to validate.
     */
	private static void validateFrame(FramingContext state, Object frame) {
		if (!(frame instanceof List) || ((List)frame).size() != 1 || !(((List) frame).get(0) instanceof Map)) {
			throw new RuntimeException("Invalid JSON-LD syntax; a JSON-LD frame must be a single object.");
		}
	}

	/**
     * Performs JSON-LD framing.
     *
     * @param input the JSON-LD input to frame.
     * @param frame the JSON-LD frame to use.
     * @param [options] the framing options.
     *          [base] the base IRI to use.
     *          [embed] default @embed flag (default: true).
     *          [explicit] default @explicit flag (default: false).
     *          [omitDefault] default @omitDefault flag (default: false).
     *          [optimize] optimize when compacting (default: false).
     *          [resolver(url, callback(err, jsonCtx))] the URL resolver to use.
     * @param callback(err, framed) called once the operation completes.
     */
    public static Object frame(Object input, Object frame, Options opts) {
    	if (input == null) {
            return null;
        }
    	if (opts.embed == null) {
    		opts.embed  = true;
    	}
    	if (opts.optimize == null) {
    		opts.optimize = false;
    	}
    	
    	JSONLDProcessor p = new JSONLDProcessor(opts);
    	
    	// preserve frame context
    	ActiveContext ctx;
    	if (frame instanceof Map && ((Map<String, Object>) frame).containsKey("@context")) {
    		ctx = p.new ActiveContext((Map<String, Object>) frame);
    	} else {
    		ctx = p.new ActiveContext();
    	}
    	
    	// expand input
    	Object _input = expand(input, opts);
    	Object _frame = expand(frame, opts);
    	
    	Object framed = p.frame(_input, _frame);
    	
    	opts.graph = true;
    	
    	Map<String,Object> compacted = (Map<String, Object>) compact(framed, ctx, opts);
    	String graph = compactIri(ctx, "@graph");
    	compacted.put(graph, removePreserve(ctx, compacted.get(graph)));
        return compacted;
    }

    /**
     * Removes the @preserve keywords as the last step of the framing algorithm.
     *
     * @param ctx the active context used to compact the input.
     * @param input the framed, compacted output.
     *
     * @return the resulting output.
     */
    private static Object removePreserve(ActiveContext ctx, Object input) {
		if (input instanceof List) {
			List<Object> l = new ArrayList<Object>();
			for (Object i: (List)input) {
				Object r = removePreserve(ctx, i);
				if (r != null) {
					l.add(r);
				}
			}
			input = l;
		} else if (input instanceof Map) {
			Map<String,Object> imap = (Map<String,Object>)input;
			if (imap.containsKey("@preserve")) {
				if ("@null".equals(imap.get("@preserve"))) {
					return null;
				}
				return imap.get("@preserve");
			}
			
			if (imap.containsKey("@value")) {
				return input;
			}
			
			if (imap.containsKey("@list")) {
				imap.put("@list", removePreserve(ctx, imap.get("@list")));
				return input;
			}
			
			for (String key: imap.keySet()) {
				Object res = removePreserve(ctx, imap.get(key));
				Object container = ctx.getContextValue(key, "@container");
				if (res instanceof List && ((List)res).size() == 1 && !"@set".equals(container) && !"@list".equals(container)) {
					res = ((List<String>) res).get(0);
				}
				imap.put(key, res);
			}
		}
		return input;
	}

    
    /**
     * Recursively flattens the subjects in the given JSON-LD expanded input.
     *
     * @param input the JSON-LD expanded input.
     * @param graphs a map of graph name to subject map.
     * @param graph the name of the current graph.
     * @param namer the blank node namer.
     * @param name the name assigned to the current input if it is a bnode.
     * @param list the list to append to, null for none.
     */
    private static void flatten(Object input, Map<String,Object> graphs, String graph, UniqueNamer namer, String name, List<Object> list) {
    	if (input instanceof List) {
    		for (Object i: (List)input) {
    			flatten(i, graphs, graph, namer, null, list);
    		}
    		return;
    	}
    	if (!(input instanceof Map) || ((Map)input).containsKey("@value")) {
    		if (list != null) {
    			list.add(input);
    		}
    		return;
    	}
    	
    	// TODO: isUndefined (in js this is different from === null
    	if (name == null) {
    		name = JSONLDUtils.isBlankNode(input) ? namer.getName((String) ((Map<String, Object>) input).get("@id")) : (String)((Map<String, Object>) input).get("@id");
    	}
    	
    	if (list != null) {
    		HashMap<String, Object> map = new HashMap<String,Object>();
    		map.put("@id", name);
    		list.add(map);
    	}
    	
    	Map<String,Object> subjects = (Map<String, Object>) graphs.get(graph);
    	Map<String,Object> subject;
    	if (subjects.containsKey(name)) {
    		subject = (Map<String, Object>) subjects.get(name);
    	} else {
    		subject = new HashMap<String, Object>();
    		subjects.put(name, subject);
    	}
    	subject.put("@id", name);
    	for (String prop: ((Map<String, Object>) input).keySet()) {
    		if ("@id".equals(prop)) {
    			continue;
    		}
    		
    		// recurse into graph
    		if ("@graph".equals(prop)) {
    			if (!graphs.containsKey(name)) {
    				graphs.put(name, new HashMap<String, Object>());
    			}
    			String g = "@merged".equals(graph) ? graph : name;
    			flatten(((Map<String, Object>) input).get(prop), graphs, g, namer, null, null);
    			continue;
    		}
    		
    		// copy non-@type keywords
    		if (!"@type".equals(prop) && JSONLDUtils.isKeyword(prop)) {
    			subject.put(prop, ((Map<String, Object>) input).get(prop));
    			continue;
    		}
    		
    		// iterate over objects
    		Object objects = ((Map<String, Object>) input).get(prop);
    		Object[] keys = null;
    		int len = 0;
    		if (objects instanceof Map) {
    			keys = ((Map<String,Object>)objects).keySet().toArray();
    			len = keys.length;
    		} else {
    			len = ((List)objects).size();
    		}
    		for (int i = 0; i < len; i++) {
    			Object o;
    			if (objects instanceof Map) {
    				o = ((Map)objects).get(keys[i]);
    			} else {
    				o = ((List)objects).get(i);
    			}
    			
    			if (JSONLDUtils.isSubject(o) || JSONLDUtils.isReference(o)) {
    				// rename blank node @id
    				String id = (String) (JSONLDUtils.isBlankNode(o) ? namer.getName((String) ((Map<String,Object>)o).get("@id")) : ((Map<String,Object>)o).get("@id"));
    				
    				// add reference and recurse
    				Map<String,Object> tmp = new HashMap<String, Object>();
    				tmp.put("@id", id);
    				JSONLDUtils.addValue(subject, prop, tmp, true);
    				flatten(o, graphs, graph, namer, id);
    			} else {
    				// recurse into list
    				if (o instanceof Map && ((Map)o).containsKey("@list")) {
    					List<Object> _list = new ArrayList<Object>();
    					flatten(((Map)o).get("@list"), graphs, graph, namer, name, _list);
    					o = new HashMap<String, Object>();
    					((Map<String, Object>) o).put("@list", _list);
    				// special-handle @type IRIs
    				} else if ("@type".equals(prop) && o instanceof String && ((String) o).startsWith("_:")) {
    					o = namer.getName((String) o);
    				}
    				
    				// add non-subject
    				JSONLDUtils.addValue(subject, prop, o, true);
    			}
    		}
    	}
    }
    
    private static void flatten(Object input, Map<String,Object> graphs, String graph, UniqueNamer namer, String name) {
    	flatten(input, graphs, graph, namer, name, null);
    }
    
    private static void flatten(Object input, Map<String,Object> graphs, String graph, UniqueNamer namer) {
    	flatten(input, graphs, graph, namer, null, null);
    }
    
	/**
     * Generates a unique simplified key from a URI and add it to the context 
     * 
     * @param key to full URI to generate the simplified key from
     * @param ctx the context to add the simplified key too
     * @param isid whether to set the type to @id
     */
    private static void processKeyVal(String key, Map<String, Object> ctx, Boolean isid) {
        int idx = key.lastIndexOf('#');
        if (idx < 0) {
            idx = key.lastIndexOf('/');
        }
        String skey = key.substring(idx + 1);
        Object keyval = key;
        if (isid) {
            Map tmp = new HashMap();
            tmp.put("@type", "@id");
            tmp.put("@id", key);
            keyval = tmp;
        }
        while (true) {
            // check if the key is already in the frame ctx
            if (ctx.containsKey(skey)) {
                // if so, check if the values are the same
                if (ctx.get(skey).equals(keyval)) {
                    // if they are, skip adding this
                    break;
                }
                // if not, add a _ to the simple key and try again
                skey += "_";
            } else {
                ctx.put(skey, keyval);
                break;
            }
        }
    }

    /**
     * Generates the context to be used by simplify.
     * 
     * @param input
     * @param ctx
     */
    private static void generateSimplifyContext(Object input, Map<String, Object> ctx) {
        if (input instanceof List) {
            for (Object o : (List) input) {
                generateSimplifyContext(o, ctx);
            }
        } else if (input instanceof Map) {
            Map<String, Object> o = (Map<String, Object>) input;
            for (String key : o.keySet()) {
                Object val = o.get(key);
                if (key.matches("^https?://.+$")) {
                    processKeyVal(key, ctx, (val instanceof Map) && ((Map) val).containsKey("@id"));
                }
                if ("@type".equals(key)) {
                    if (!(val instanceof List)) {
                        List<Object> tmp = new ArrayList<Object>();
                        tmp.add(val);
                        val = tmp;
                    }
                    for (Object t : (List<Object>) val) {
                        if (t instanceof String) {
                            processKeyVal((String) t, ctx, true);
                        } else {
                            throw new RuntimeException("TODO: don't yet know how to handle non-string types in @type");
                        }
                    }
                } else if (val instanceof Map || val instanceof List) {
                    generateSimplifyContext(val, ctx);
                }
            }
        }
    }

    /**
     * Automatically builds a context which attempts to simplify the keys and values as much as possible
     * and uses that context to compact the input
     * 
     * NOTE: this is experimental and only built for specific conditions
     * 
     * @param input
     * @return the simplified version of input
     */
    public Object simplify(Map input) {

        Object expanded = expand(input);
        Map<String, Object> framectx = new HashMap<String, Object>();
        if (input.containsKey("@context")) {
            framectx.putAll((Map<? extends String, ? extends Object>) input.get("@context"));
        }

        generateSimplifyContext(expanded, framectx);

        return compact(expanded, framectx);
    }

    
    
    // ALL CODE BELOW THIS IS UNUSED
    
    
    
    public void nameBlankNodes(Object input) {
        JSONLDUtils.NameGenerator ng = new JSONLDUtils.NameGenerator("tmp");
        this.ngtmp = ng;

        // Map<String,Object> subjects = new HashMap<String, Object>();
        subjects = new HashMap<String, Object>();
        List<Map<String, Object>> bnodes = new ArrayList<Map<String, Object>>();

        collectSubjects(input, subjects, bnodes);

        for (Map<String, Object> bnode : bnodes) {
            if (!bnode.containsKey("@id")) {
                while (subjects.containsKey(ng.next()))
                    ;
                ((Map<String, Object>) bnode).put("@id", ng.current());
                subjects.put(ng.current(), bnode);
            }
        }
    }

    private void collectEdges() {
        Map<String, Object> refs = (Map<String, Object>) this.edges.get("refs");
        Map<String, Object> props = (Map<String, Object>) this.edges.get("props");

        for (String iri : this.subjects.keySet()) {
            Map<String, Object> subject = (Map<String, Object>) this.subjects.get(iri);
            for (String key : subject.keySet()) {
                if (!key.equals("@id")) {
                    Object object = subject.get(key);
                    List<Object> tmp = null;
                    if (object instanceof List) {
                        tmp = (List<Object>) object;
                    } else {
                        tmp = new ArrayList<Object>();
                        tmp.add(object);
                    }
                    for (Object o : tmp) {
                        if (o instanceof Map && ((Map) o).containsKey("@id") && this.subjects.containsKey(((Map) o).get("@id"))) {
                            Object objIri = ((Map<String, Object>) o).get("@id");
                            Map<String, Object> tmp1 = new HashMap<String, Object>();
                            tmp1.put("s", iri);
                            tmp1.put("p", key);
                            ((Map<String, List>) refs.get(objIri)).get("all").add(tmp1);
                            tmp1 = new HashMap<String, Object>();
                            tmp1.put("s", objIri);
                            tmp1.put("p", key);
                            ((Map<String, List>) props.get(iri)).get("all").add(tmp1);
                        }
                    }
                }
            }
        }

        Comparator<Object> edgesCmp = new Comparator<Object>() {
            public int compare(Object a, Object b) {
                return compareEdges(a, b);
            }
        };

        for (String iri : refs.keySet()) {
            List<Object> all = (List<Object>) ((Map<String, Object>) refs.get(iri)).get("all");
            Collections.sort(all, edgesCmp);
            List<Object> bnodes = new ArrayList<Object>();
            for (Object edge : all) {
                if (JSONLDUtils.isBlankNodeIri(((Map<String, Object>) edge).get("s"))) {
                    bnodes.add(edge);
                }
            }
            ((Map<String, Object>) refs.get(iri)).put("bnodes", bnodes);
        }
        for (String iri : props.keySet()) {
            List<Object> all = (List<Object>) ((Map<String, Object>) props.get(iri)).get("all");
            Collections.sort(all, edgesCmp);
            List<Object> bnodes = new ArrayList<Object>();
            for (Object edge : all) {
                if (JSONLDUtils.isBlankNodeIri(((Map<String, Object>) edge).get("s"))) {
                    bnodes.add(edge);
                }
            }
            ((Map<String, Object>) props.get(iri)).put("bnodes", bnodes);
        }

    }

    public void canonicalizeBlankNodes(List<Map<String, Object>> input) {

        this.renamed = new HashMap<String, Object>();
        this.serializations = new HashMap<String, Object>();
        this.edges = new HashMap<String, Object>();
        edges.put("refs", new HashMap<String, Object>());
        edges.put("props", new HashMap<String, Object>());

        this.subjects = new HashMap<String, Object>();
        List<Map<String, Object>> bnodes = new ArrayList<Map<String, Object>>();

        for (Map<String, Object> s : input) {
            String iri = (String) s.get("@id");
            subjects.put(iri, s);
            Map<String, Object> refs = (Map<String, Object>) edges.get("refs");
            Map<String, List> tmp = new HashMap<String, List>();
            tmp.put("all", new ArrayList<Object>());
            tmp.put("bnodes", new ArrayList<Object>());
            refs.put(iri, tmp);
            Map<String, Object> props = (Map<String, Object>) edges.get("props");
            tmp = new HashMap<String, List>();
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

        for (Map<String, Object> bnode : bnodes) {
            String iri = (String) bnode.get("@id");
            if (c14n.inNamespace(iri)) {
                while (subjects.containsKey(ngTmp.next()))
                    ;
                renameBlankNode(bnode, ngTmp.current());
                iri = (String) bnode.get("@id");
            }
            Map<String, Object> tmp = new HashMap<String, Object>();
            tmp.put("props", null);
            tmp.put("refs", null);
            serializations.put(iri, tmp);
        }

        Comparator<Map<String, Object>> bnodeSort = new Comparator<Map<String, Object>>() {
            private JSONLDProcessor processor;

            public Comparator<Map<String, Object>> setProcessor(JSONLDProcessor p) {
                processor = p;
                return this;
            }

            public int compare(Map<String, Object> a, Map<String, Object> b) {
                int rval = processor.deepCompareBlankNodes(a, b);
                return rval;
            }
        }.setProcessor(this);

        // keep sorting and naming blank nodes until they are all named
        boolean resort = true;
        while (bnodes.size() > 0) {

            if (resort) {
                resort = false;
                Collections.sort(bnodes, bnodeSort);
            }

            Map<String, Object> bnode = bnodes.get(0);
            bnodes.remove(0);
            String iri = (String) bnode.get("@id");
            resort = serializations.containsKey(iri) && ((Map<String, Object>) serializations.get(iri)).get("props") != null;
            Map<String, Object> mapping = null;
            for (String dir : new String[] { "props", "refs" }) {
                // if no serialization has been computed, name only the first
                // node
                if (serializations.containsKey(iri) && ((Map<String, Object>) serializations.get(iri)).containsKey(dir)
                        && ((Map<String, Object>) serializations.get(iri)).get(dir) != null) {
                    mapping = (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) serializations.get(iri)).get(dir)).get("m");
                } else {
                    mapping = new HashMap<String, Object>();
                    mapping.put(iri, "s1");
                }

                // TODO: sort keys by value to name them in order
                List<String> keys = new ArrayList<String>(mapping.keySet());
                Collections.sort(keys, new Comparator<String>() {
                    private Map<String, Object> mapping;

                    public Comparator<String> setMapping(Map<String, Object> m) {
                        this.mapping = m;
                        return this;
                    }

                    public int compare(String a, String b) {
                        return JSONLDUtils.compare(this.mapping.get(a), this.mapping.get(b));
                    }
                }.setMapping(mapping));

                // name bnodes in mapping
                List<String> renamed = new ArrayList<String>();
                for (String iriK : keys) {
                    if (!c14n.inNamespace(iri) && subjects.containsKey(iriK)) {
                        renameBlankNode((Map<String, Object>) subjects.get(iriK), c14n.next());
                        renamed.add(iriK);
                    }
                }

                // only keep non-canonically named bnodes
                List<Map<String, Object>> tmp = bnodes;
                bnodes = new ArrayList<Map<String, Object>>();
                for (Map<String, Object> b : tmp) {
                    String iriB = (String) b.get("@id");
                    if (!c14n.inNamespace(iriB)) {
                        for (Object i2 : renamed) {
                            if (markSerializationDirty(iriB, i2, dir)) {
                                resort = true;
                            }
                        }
                        bnodes.add(b);
                    }
                }
            }
        }

        for (String key : ((Map<String, Object>) edges.get("props")).keySet()) {
            if (((List<Object>) ((Map<String, Object>) ((Map<String, Object>) edges.get("props")).get(key)).get("bnodes")).size() > 0) {
                Map<String, Object> bnode = (Map<String, Object>) subjects.get(key);
                for (String p : bnode.keySet()) {
                    if (!p.startsWith("@") && bnode.get(p) instanceof List) {
                        Collections.sort((List<Object>) bnode.get(p), new Comparator<Object>() {
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
        if (((Map<String, Object>) s).containsKey(dir) && ((Map<String, Object>) s).get(dir) != null
                && ((Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) s).get(dir)).get("m")).containsKey(changed)) {
            ((Map<String, Object>) s).put(dir, null);
            rval = true;
        }
        return rval;
    }

    private void renameBlankNode(Map<String, Object> b, String id) {

        String old = (String) b.get("@id");
        b.put("@id", id);

        subjects.put(id, subjects.get(old));
        subjects.remove(old);

        // update reference and property lists
        ((Map<String, Object>) edges.get("refs")).put(id, ((Map<String, Object>) edges.get("refs")).get(old));
        ((Map<String, Object>) edges.get("props")).put(id, ((Map<String, Object>) edges.get("props")).get(old));
        ((Map<String, Object>) edges.get("refs")).remove(old);
        ((Map<String, Object>) edges.get("props")).remove(old);

        // update references to this bnode
        List<Map<String, Object>> refs = (List<Map<String, Object>>) ((Map<String, Object>) ((Map<String, Object>) edges.get("refs")).get(id)).get("all");
        for (Map<String, Object> i : refs) {
            String iri = (String) i.get("s");
            if (iri.equals(old)) {
                iri = id;
            }
            Map<String, Object> ref = (Map<String, Object>) subjects.get(iri);
            List<Map<String, Object>> props = (List<Map<String, Object>>) ((Map<String, Object>) ((Map<String, Object>) edges.get("props")).get(iri))
                    .get("all");
            for (Map<String, Object> i2 : props) {
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

                    for (Object n : tmp) {
                        if (n instanceof Map && ((Map) n).containsKey("@id") && old.equals(((Map<String, Object>) n).get("@id"))) {
                            ((Map<String, Object>) n).put("@id", id);
                        }
                    }
                }
            }
        }

        // update references from this bnode
        List<Map<String, Object>> props = (List<Map<String, Object>>) ((Map<String, Object>) ((Map<String, Object>) edges.get("props")).get(id)).get("all");
        for (Map<String, Object> i : props) {
            String iri = (String) i.get("s");
            refs = (List<Map<String, Object>>) ((Map<String, Object>) ((Map<String, Object>) edges.get("refs")).get(iri)).get("all");
            for (Map<String, Object> r : refs) {
                if (old.equals(r.get("s"))) {
                    r.put("s", id);
                }
            }
        }
    }

    private int deepCompareBlankNodes(Map<String, Object> a, Map<String, Object> b) {
        int rval = 0;

        String iriA = (String) a.get("@id");
        String iriB = (String) b.get("@id");

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
                    Map<String, Object> sA = (Map<String, Object>) serializations.get(iriA);
                    Map<String, Object> sB = (Map<String, Object>) serializations.get(iriB);
                    if (sA.get(dir) == null) {
                        MappingBuilder mb = new MappingBuilder();
                        if (dir.equals("refs")) {
                            mb.mapping = (Map<String, String>) JSONLDUtils.clone(((Map<String, Object>) sA.get("props")).get("m"));
                            mb.count = mb.mapping.size() + 1;
                        }
                        serializeBlankNode(sA, iriA, mb, dir);
                    }
                    if (sB.get(dir) == null) {
                        MappingBuilder mb = new MappingBuilder();
                        if (dir.equals("refs")) {
                            mb.mapping = (Map<String, String>) JSONLDUtils.clone(((Map<String, Object>) sB.get("props")).get("m"));
                            mb.count = mb.mapping.size() + 1;
                        }
                        serializeBlankNode(sB, iriB, mb, dir);
                    }

                    rval = JSONLDUtils.compare(((Map<String, Object>) sA.get(dir)).get("s"), ((Map<String, Object>) sB.get(dir)).get("s"));
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

            List<Object> adj = (List<Object>) ((Map<String, Object>) ((Map<String, Object>) edges.get(dir)).get(iri)).get("bnodes");
            Map<String, Object> mapped = new HashMap<String, Object>();
            List<Object> notMapped = new ArrayList<Object>();

            for (Object i : adj) {
                if (mb.mapping.containsKey(((Map<String, Object>) i).get("s"))) {
                    mapped.put(mb.mapping.get(((Map<String, Object>) i).get("s")), ((Map<String, Object>) i).get("s"));
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

    private void serializeCombos(Map<String, Object> s, String iri, String siri, MappingBuilder mb, String dir, Map<String, Object> mapped,
            List<Object> notMapped) {
        if (notMapped.size() > 0) {
            mapped = (Map<String, Object>) JSONLDUtils.clone(mapped);
            mapped.put(mb.mapNode((String) ((Map<String, Object>) notMapped.get(0)).get("s")), ((Map<String, Object>) notMapped.get(0)).get("s"));

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
            Map<String, Object> tmp = new HashMap<String, Object>();
            tmp.put("i", iri);
            tmp.put("k", keys);
            tmp.put("m", mapped);
            mb.adj.put(siri, tmp);
            mb.serialize(this.subjects, this.edges);

            if (s.get(dir) == null || JSONLDUtils.compareSerializations(mb.s, (String) ((Map<String, Object>) s.get(dir)).get("s")) <= 0) {
                for (String k : keys) {
                    serializeBlankNode(s, (String) mapped.get(k), mb, dir);
                }

                mb.serialize(this.subjects, this.edges);
                if (s.get(dir) == null || JSONLDUtils.compareSerializations(mb.s, (String) ((Map<String, Object>) s.get(dir)).get("s")) <= 0
                        && mb.s.length() >= ((String) ((Map<String, Object>) s.get(dir)).get("s")).length()) {
                    tmp = new HashMap<String, Object>();
                    tmp.put("s", mb.s);
                    tmp.put("m", mb.mapping);
                    s.put(dir, tmp);
                }
            }
        }
    }

    private int shallowCompareBlankNodes(Map<String, Object> a, Map<String, Object> b) {
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
            List<Object> edgesA = (List<Object>) ((Map<String, Object>) ((Map<String, Object>) edges.get("refs")).get(a.get("@id"))).get("all");
            List<Object> edgesB = (List<Object>) ((Map<String, Object>) ((Map<String, Object>) edges.get("refs")).get(b.get("@id"))).get("all");
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

        boolean bnodeA = JSONLDUtils.isBlankNodeIri(((Map<String, Object>) a).get("s"));
        boolean bnodeB = JSONLDUtils.isBlankNodeIri(((Map<String, Object>) b).get("s"));
        JSONLDUtils.NameGenerator c14n = ngc14n;

        if (bnodeA != bnodeB) {
            rval = bnodeA ? 1 : -1;
        } else {

            if (!bnodeA) {
                rval = JSONLDUtils.compare(((Map<String, Object>) a).get("s"), ((Map<String, Object>) b).get("s"));
            }
            if (rval == 0) {
                rval = JSONLDUtils.compare(((Map<String, Object>) a).get("p"), ((Map<String, Object>) b).get("p"));
            }

            if (rval == 0 && c14n != null) {
                boolean c14nA = c14n.inNamespace((String) ((Map<String, Object>) a).get("s"));
                boolean c14nB = c14n.inNamespace((String) ((Map<String, Object>) b).get("s"));

                if (c14nA != c14nB) {
                    rval = c14nA ? 1 : -1;
                } else if (c14nA) {
                    rval = JSONLDUtils.compare(((Map<String, Object>) a).get("s"), ((Map<String, Object>) b).get("s"));
                }

            }

        }

        return rval;
    }

    private void collectSubjects(Object input, Map<String, Object> subjects, List<Map<String, Object>> bnodes) {
        if (input == null) {
            return;
        } else if (input instanceof List) {
            for (Object o : (List<Object>) input) {
                collectSubjects(o, subjects, bnodes);
            }
        } else if (input instanceof Map) {
            if (((Map<String, Object>) input).containsKey("@id")) {
                Object id = ((Map<String, Object>) input).get("@id");
                if (id instanceof List) {
                    // graph literal
                    collectSubjects(id, subjects, bnodes);
                } else if (JSONLDUtils.isSubject(input)) {
                    // named subject
                    subjects.put((String) id, input);
                }

            } else if (JSONLDUtils.isBlankNode(input)) {
                bnodes.add((Map<String, Object>) input);
            }

            for (String key : ((Map<String, Object>) input).keySet()) {
                if (!ignoredKeywords.contains(key)) {
                    collectSubjects(((Map<String, Object>) input).get(key), subjects, bnodes);
                }
            }
        }
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
            this.processed = new HashMap<String, Boolean>();
            this.mapping = new HashMap<String, String>();
            this.adj = new HashMap<String, Object>();

            // this.keyStack = [{ keys: ['s1'], idx: 0 }];
            this.keyStack = new ArrayList<Object>();
            Map<String, Object> t1 = new HashMap<String, Object>();
            List<String> t2 = new ArrayList<String>();
            t2.add("s1");
            t1.put("keys", t2);
            t1.put("idx", 0);
            keyStack.add(t1);
            this.done = new HashMap<String, Boolean>();
            this.s = "";
        }

        public HashMap<String, Boolean> done;
        public ArrayList<Object> keyStack;
        public String s;
        public Map<String, Object> adj;
        public Map<String, Boolean> processed;
        public int count;
        public Map<String, String> mapping;

        /**
         * Maps the next name to the given bnode IRI if the bnode IRI isn't already in the mapping. If the given bnode IRI is canonical, then it will be given a
         * shortened form of the same name.
         * 
         * @param iri
         *            the blank node IRI to map the next name to.
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
                Map<String, Object> next = (Map<String, Object>) this.keyStack.remove(this.keyStack.size() - 1);
                // for (; (Integer)next.get("idx") < ((List<String>)
                // next.get("keys")).size(); next.put("idx",
                // (Integer)next.get("idx")+1)) {
                while ((Integer) next.get("idx") < ((List<String>) next.get("keys")).size()) {
                    String k = ((List<String>) next.get("keys")).get((Integer) next.get("idx"));
                    if (!this.adj.containsKey(k)) {
                        this.keyStack.add(next);
                        break;
                    }
                    next.put("idx", (Integer) next.get("idx") + 1);

                    if (this.done.containsKey(k)) {
                        this.s += "_" + k;
                    } else {
                        this.done.put(k, true);

                        String s = k;
                        Map<String, Object> adj = (Map<String, Object>) this.adj.get(k);
                        String iri = (String) adj.get("i");
                        if (subjects.containsKey(iri)) {
                            Map<String, Object> b = (Map<String, Object>) subjects.get(iri);
                            s += "[" + JSONLDUtils.serializeProperties(b) + "]";

                            Boolean first = true;
                            s += "[";
                            List<Map<String, Object>> refs = (List<Map<String, Object>>) ((Map<String, Object>) ((Map<String, Object>) edges.get("refs"))
                                    .get(iri)).get("all");
                            for (Map<String, Object> r : refs) {
                                if (first) {
                                    first = false;
                                } else {
                                    s += "|";
                                }
                                s += "<" + r.get("p") + ">";
                                s += JSONLDUtils.isBlankNodeIri(r.get("s")) ? "_:" : ("<" + r.get("s") + ">");
                            }
                            s += "]";
                        }

                        for (String o : (List<String>) adj.get("k")) {
                            s += o;
                        }
                        this.s += s;
                        Map<String, Object> tmp = new HashMap<String, Object>();
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
            rval.processed = (Map<String, Boolean>) JSONLDUtils.clone(this.processed);
            rval.mapping = (Map<String, String>) JSONLDUtils.clone(this.mapping);
            rval.adj = (Map<String, Object>) JSONLDUtils.clone(this.adj);
            rval.keyStack = (ArrayList<Object>) JSONLDUtils.clone(this.keyStack);
            rval.done = (HashMap<String, Boolean>) JSONLDUtils.clone(this.done);

            rval.s = this.s;
            return rval;
        }

    }

}
