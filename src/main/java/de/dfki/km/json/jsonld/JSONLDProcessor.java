package de.dfki.km.json.jsonld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        Options(String base) {
            this.base = base;
        }

        public String base;
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

    private class ActiveContext extends HashMap<String, Object> {
        public ActiveContext() {
            mappings = new HashMap<String, Object>();
            keywords = new HashMap<String, List<String>>() {
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
            };
            this.put("mappings", mappings);
            this.put("keywords", keywords);
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

    // TODO: note that in the spec, input is the first input, but in the ref
    // impl, context is.
    // using context here first because it matches everything else
    public Object compact(Object context, Object input) {

        Object rval = null;

        Map<String, Object> ctx;
        if (context == null) {
            ctx = new HashMap<String, Object>();
        } else if (context instanceof Map) {
            ctx = (Map<String, Object>) JSONLDUtils.clone(context);
        } else if (context instanceof List) {
            ctx = (Map<String, Object>) JSONLDUtils.mergeContexts(new HashMap<String, Object>(), context);
        } else {
            // TODO: are there any reasons that ctx shouldn't be a map?
            throw new RuntimeException("non-map or list contexts not yes supported");
        }

        input = expand(input);

        Map<String, Object> ctxOut = new HashMap<String, Object>();
        Object out = compact(ctx, null, input, ctxOut);
        rval = out;

        if (ctxOut.size() > 0) {
            rval = new HashMap<String, Object>();
            ((Map<String, Object>) rval).put("@context", ctxOut);
            if (out instanceof List) {
                ((Map<String, Object>) rval).put(JSONLDUtils.getKeywords(ctxOut).get("@id"), out);
            } else {
                for (String key : ((Map<String, Object>) out).keySet()) {
                    ((Map<String, Object>) rval).put(key, ((Map<String, Object>) out).get(key));
                }
            }
        }

        return rval;
    }

    /*
     * TODO: this throws runtime exception. need to look at doing something more sane.
     */
    public Object compact(Map<String, Object> ctx, Object property, Object value, Map<String, Object> usedCtx) {

        Object rval = null;
        Map<String, String> keywords = JSONLDUtils.getKeywords(ctx);

        if (value == null) {
            rval = null;
            // TODO: used ctx stuff may actually be handy here
            JSONLDUtils.getCoercionType(ctx, (String) property, usedCtx);
        } else if (value instanceof List) {
            // recursively add compacted values to array
            rval = new ArrayList<Object>();
            for (Object o : (List<Object>) value) {
                ((List) rval).add(compact(ctx, property, o, usedCtx));
            }
        } else if (value instanceof Map && ((Map) value).containsKey("@id") && ((Map) value).get("@id") instanceof List) {
            // graph literal/disjoint graph
            rval = new HashMap<String, Object>();
            ((Map<String, Object>) rval).put(keywords.get("@id"), compact(ctx, property, ((Map) value).get("@id"), usedCtx));
        } else if (JSONLDUtils.isSubject(value)) { // recurse if value is a
                                                   // subject
            // recursively handle sub-properties that aren't a sub-context
            rval = new HashMap<String, Object>();
            for (String key : ((Map<String, Object>) value).keySet()) {
                if (!"@context".equals(((Map<String, Object>) value).get(key))) {
                    // set object to compacted property, only overwrite existing
                    // properties if the property actually compacted
                    String p = JSONLDUtils.compactIRI(ctx, key, usedCtx);
                    if (!(key.equals(p)) || !((Map<String, Object>) rval).containsKey(p)) {
                        ((Map<String, Object>) rval).put(p, compact(ctx, key, ((Map<String, Object>) value).get(key), usedCtx));
                    }
                }
            }
        } else {
            // get coerce type
            String coerce = JSONLDUtils.getCoercionType(ctx, (String) property, usedCtx);

            // get type from value, to ensure coercion is valid
            String type = null;
            if (value instanceof Map) {
                // type coercion can only occur if language is not specified
                if (!((Map) value).containsKey("@language")) {
                    // type must match coerce type if specified
                    if (((Map) value).containsKey("@type")) {
                        type = (String) ((Map<String, Object>) value).get("@type");
                    } else if (((Map) value).containsKey("@id")) { // type is ID
                                                                   // (IRI)
                        type = "@id";
                    } else { // can be coerced to any type
                        type = coerce;
                    }
                }
            } else if (value instanceof String) {
                // type can be coerced to anything
                type = coerce;
            }

            // types that can be auto-coerced from a JSON-builtin
            if (coerce == null && (JSONLDConsts.XSD_BOOLEAN.equals(type) || JSONLDConsts.XSD_INTEGER.equals(type) || JSONLDConsts.XSD_DOUBLE.equals(type))) {
                coerce = type;
            }

            // do reverse type-coercion
            if (coerce != null) {
                // type is only None if a language was specified, which is an
                // error if type coercion is specified
                if (type == null) {
                    throw new RuntimeException("Cannot coerce type when a language is " + "specified. The language information would be lost.");
                } else if (!type.equals(coerce)) {
                    // if the value type does not match the coerce type, it is
                    // an error
                    throw new RuntimeException("Cannot coerce type because the type does not match.");
                } else {
                    // do reverse type-coercion
                    if (value instanceof Map) {
                        if (((Map) value).containsKey("@id")) {
                            rval = ((Map) value).get("@id");
                        } else if (((Map) value).containsKey("@value")) {
                            rval = ((Map) value).get("@value");
                        }
                    } else {
                        rval = value;
                    }

                    // do basic JSON types conversion
                    if (JSONLDConsts.XSD_BOOLEAN.equals(coerce)) {
                        // TODO: this is a bit mad (and probably not exhaustive)
                        if (rval instanceof String) {
                            rval = "true".equals(rval);
                        } else if (rval instanceof Integer) {
                            rval = ((Integer) rval) != 0;
                        } else if (rval instanceof Double) {
                            rval = ((Double) rval) != 0.0;
                        } else if (rval instanceof Long) {
                            rval = ((Long) rval) != 0L;
                        } else if (rval instanceof Float) {
                            rval = ((Float) rval) != 0.0f;
                        } else if (rval instanceof Short) {
                            rval = ((Short) rval) != 0;
                        } else if (!(rval instanceof Boolean)) {
                            rval = false;
                        }
                    } else if (JSONLDConsts.XSD_DOUBLE.equals(coerce)) {
                        if (rval instanceof String) {
                            rval = Double.parseDouble((String) rval);
                        } else if (rval instanceof Number) {
                            rval = ((Number) rval).doubleValue();
                        }
                    } else if (JSONLDConsts.XSD_INTEGER.equals(coerce)) {
                        if (rval instanceof String) {
                            rval = Integer.parseInt((String) rval);
                        } else if (rval instanceof Number) {
                            rval = ((Number) rval).intValue();
                        }
                    }
                }
            } else if (value instanceof Map) {
                // no type-coercion, just change keywords/copy value
                rval = new HashMap<String, Object>();
                for (String key : ((Map<String, Object>) value).keySet()) {
                    ((Map<String, Object>) rval).put(keywords.get(key), ((Map) value).get(key));
                }
            } else {
                rval = JSONLDUtils.clone(value);
            }

            if ("@id".equals(type)) {
                // compact IRI
                if (rval instanceof Map) {
                    ((Map) rval).put(keywords.get("@id"), JSONLDUtils.compactIRI(ctx, (String) ((Map<String, Object>) rval).get(keywords.get("@id")), usedCtx));
                } else {
                    rval = JSONLDUtils.compactIRI(ctx, (String) rval, usedCtx);
                }
            }
        }
        return rval;
    }

    public Object frame(Object input, Object frame) {
        return frame(input, frame, null);
    }

    public Object frame(Object input, Object frame, Map options) {
        Object rval = null;

        input = normalize(input);

        Object ctx = null;

        if (frame instanceof Map && ((Map) frame).containsKey("@context")) {
            ctx = JSONLDUtils.clone(((Map) frame).get("@context"));
            frame = expand(frame);
        } else if (frame instanceof List) {
            if (((List) frame).size() > 0) {
                Object f0 = ((List) frame).get(0);
                if (f0 instanceof Map && ((Map) f0).containsKey("@context")) {
                    ctx = JSONLDUtils.clone(((Map) f0).get("@context"));
                }

                List tmp = new ArrayList();
                for (Object f : (List) frame) {
                    tmp.add(expand(f));
                }
                frame = tmp;
            }
        }

        Map defaultOptions = new HashMap();
        Map tmpopts = new HashMap();
        tmpopts.put("embedOn", true);
        tmpopts.put("explicitOn", false);
        tmpopts.put("omitDefaultOn", false);
        defaultOptions.put("defaults", tmpopts);

        // TODO: merge in options from input
        options = defaultOptions;

        Map subjects = new HashMap();
        for (Object i : (List) input) {
            subjects.put(((Map) i).get("@id"), i);
        }

        rval = JSONLDUtils.frame(subjects, (List) input, frame, new HashMap(), false, null, null, options);

        if (ctx != null && rval != null) {
            if (rval instanceof List) {
                List tmp = (List) rval;
                rval = new ArrayList();
                for (Object i : tmp) {
                    ((List) rval).add(compact(ctx, i));
                }
            } else {
                rval = compact(ctx, rval);
            }
        }

        return rval;
    }

    public List<? extends Map<String, Object>> normalize(Object input) {
        // because the expanded output of items from the onlinebox are the same
        // as the normalized version
        // (just inside a list) i'm going to skip implementing the normalize
        // function for now.
        // TODO: implement this properly as if data is really to be imported
        // into the OB with this method
        // this will be needed (mainly for identifying embedded items)
        List<Map<String, Object>> rval = new ArrayList<Map<String, Object>>();

        if (input != null) {

            Object expanded = null;//expand(new HashMap<String, Object>(), null, input);

            nameBlankNodes(expanded);

            Map<String, Object> subjects = new HashMap<String, Object>();
            try {
                flatten(null, null, expanded, subjects);
            } catch (Exception e) {
                // TODO: This should probably be thrown back to the caller
                e.printStackTrace();
                LOG.error("flatten failed!");
                return null;
            }

            for (String key : subjects.keySet()) {
                Map<String, Object> s = (Map<String, Object>) subjects.get(key);
                // TODO: in javascript the keys are sorted and added back into
                // the array
                // in alphabetical order. however in HashMaps, this order isn't
                // kept
                rval.add(s);
            }

            canonicalizeBlankNodes(rval);

            // sort the output
            Collections.sort(rval, new Comparator<Map<String, Object>>() {
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
    public void triples(Object input, JSONLDTripleCallback tripleCallback) {
        Object normalized = normalize(input);

        if (tripleCallback == null) {
            // TODO: make default triple callback
        }

        List<Object> rval = new ArrayList<Object>();
        for (Map<String, Object> e : (List<Map<String, Object>>) normalized) {
            String s = (String) e.get("@id");

            for (String p : e.keySet()) {
                Object obj = e.get(p);

                // don't generate a triple for the @id or any keys that should
                // be ignored
                if (p.equals("@id") || ignoredKeywords.contains(p)) {
                    continue;
                } else if (p.equals("@type")) {
                    p = JSONLDConsts.RDF_SYNTAX_NS + "type";
                }

                if (!(obj instanceof List)) {
                    List<Object> tmp = new ArrayList<Object>();
                    tmp.add(obj);
                    obj = tmp;
                }
                for (Object o : (List<Object>) obj) {
                    if (o instanceof String) {
                        // type is a special case where the uri isn't expanded
                        // out into an object
                        if (p.toString().equals(JSONLDConsts.RDF_SYNTAX_NS + "type")) {
                            tripleCallback.triple(s, p, o.toString());
                        } else {
                            tripleCallback.triple(s, p, (String) o, JSONLDConsts.XSD_STRING, null);
                        }
                    } else if (o instanceof Map) {
                        if (((Map) o).containsKey("@value")) {
                            if (((Map) o).containsKey("@type")) {
                                String datatypeURI = (String) ((Map) o).get("@type");
                                String value = (String) ((Map) o).get("@value");
                                tripleCallback.triple(s, p, value, datatypeURI, null);
                            } else if (((Map) o).containsKey("@language")) {
                                tripleCallback.triple(s, p, (String) ((Map) o).get("@value"), JSONLDConsts.XSD_STRING, (String) ((Map) o).get("@language"));
                            } else {
                                tripleCallback.triple(s, p, (String) ((Map) o).get("@value"), JSONLDConsts.XSD_STRING, null);
                            }
                        } else if (((Map) o).containsKey("@id")) {
                            tripleCallback.triple(s, p, (String) ((Map) o).get("@id"));
                        } else {
                            // TODO: have i missed anything?
                            return;
                        }
                    }
                }
            }
        }
    }

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
     * automatically builds a frame which attempts to simplify the keys and values as much as possible
     * 
     * NOTE: this is experimental and only built for specific conditions
     * 
     * @param input
     * @return
     */
    public Object simplify(Map input) {

        Object expanded = expand(input);
        Map<String, Object> framectx = new HashMap<String, Object>();
        if (input.containsKey("@context")) {
            framectx.putAll((Map<? extends String, ? extends Object>) input.get("@context"));
        }

        generateSimplifyContext(expanded, framectx);

        return compact(framectx, expanded);
    }

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

    private void flatten(Object parent, String parentProperty, Object value, Map<String, Object> subjects) throws Exception {

        Object flattened = null;

        if (value == null) {
            // drop null values
        } else if (value instanceof List) {
            for (Object v : (List<Object>) value) {
                flatten(parent, parentProperty, v, subjects);
            }
        } else if (value instanceof Map) {
            Map<String, Object> mapVal = (Map<String, Object>) value;
            if (mapVal.containsKey("@value") || "@type".equals(parentProperty)) {
                // already-expanded value
                flattened = JSONLDUtils.clone(value);
            } else if (mapVal.get("@id") instanceof List) {
                // graph literal/disjoint graph
                if (parent != null) {
                    // cannot flatten embedded graph literals
                    throw new Exception("Embedded graph literals cannot be flattened");
                }

                // top-level graph literal
                for (Object key : (List<Object>) mapVal.get("@id")) {
                    if (!ignoredKeywords.contains(key)) {
                        flatten(parent, parentProperty, key, subjects);
                    }
                }
            } else { // regular subject
                // create of fetch existing subject
                Object subject;
                if (subjects.containsKey(mapVal.get("@id"))) {
                    subject = subjects.get(mapVal.get("@id"));
                } else {
                    subject = new HashMap<String, Object>();
                    ((Map<String, Object>) subject).put("@id", mapVal.get("@id"));
                    subjects.put((String) mapVal.get("@id"), subject);
                }
                flattened = new HashMap<String, Object>();
                ((Map<String, Object>) flattened).put("@id", ((Map<String, Object>) subject).get("@id"));

                for (String key : mapVal.keySet()) {
                    Object v = mapVal.get(key);

                    if (ignoredKeywords.contains(key)) {
                        ((Map<String, Object>) subject).put(key, v);
                    } else if (v != null && !"@id".equals(key)) {
                        if (((Map<String, Object>) subject).containsKey(key)) {
                            if (!(((Map<String, Object>) subject).get(key) instanceof List)) {
                                Object tmp = ((Map<String, Object>) subject).get(key);
                                List<Object> lst = new ArrayList<Object>();
                                lst.add(tmp);
                                ((Map<String, Object>) subject).put(key, lst);
                            }
                        } else {
                            List<Object> lst = new ArrayList<Object>();
                            ((Map<String, Object>) subject).put(key, lst);
                        }

                        flatten(((Map<String, Object>) subject).get(key), key, v, subjects);
                        if (((List<Object>) ((Map<String, Object>) subject).get(key)).size() == 1) {
                            // convert subject[key] to a single object if there
                            // is only one object in the list
                            ((Map<String, Object>) subject).put(key, ((List<Object>) ((Map<String, Object>) subject).get(key)).get(0));
                        }
                    }
                }
            }
        } else {
            // string value
            flattened = value;
        }

        if (flattened != null && parent != null) {
            if (parent instanceof List) {
                boolean duplicate = false;
                for (Object e : (List<Object>) parent) {
                    if (JSONLDUtils.compareObjects(e, flattened) == 0) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) {
                    ((List<Object>) parent).add(flattened);
                }
            } else {
                ((Map<String, Object>) parent).put(parentProperty, flattened);
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
