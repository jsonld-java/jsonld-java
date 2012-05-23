package de.dfki.km.json.jsonld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JSONLDUtils {

    /**
     * Returns whether or not the given value is a keyword (or a keyword alias).
     *
     * @param v the value to check.
     * @param [ctx] the active context to check against.
     *
     * @return true if the value is a keyword, false if not.
     */
    public static boolean isKeyword(String key) {
        // TODO: this doesn't fit with my desire to have this list modifyable at runtime
        // I may need to make this a method of JSONLDProcessor to support this
        // which may result in a lot of the utils in this library becoming member functions
        return "@context".equals(key) || "@container".equals(key) || "@default".equals(key) || "@embed".equals(key) || "@explicit".equals(key)
                || "@graph".equals(key) || "@id".equals(key) || "@language".equals(key) || "@list".equals(key) || "@omitDefault".equals(key)
                || "@preserve".equals(key) || "@set".equals(key) || "@type".equals(key) || "@value".equals(key);
    }

    public static boolean isKeyword(String key, Map<String, Object> ctx) {
        if (ctx.containsKey("keywords")) {
            Map<String, List<String>> keywords = (Map<String, List<String>>) ctx.get("keywords");
            if (keywords.containsKey(key)) {
                return true;
            }
            for (List<String> aliases : keywords.values()) {
                if (aliases.contains(key)) {
                    return true;
                }
            }
        } else {
            throw new RuntimeException("Error: missing keywords map in context!");
        }
        return false;
    }

    public static boolean isAbsoluteIri(String value) {
        return value.contains(":");
    }

    /**
     * Adds a value to a subject. If the subject already has the value, it will
     * not be added. If the value is an array, all values in the array will be
     * added.
     *
     * Note: If the value is a subject that already exists as a property of the
     * given subject, this method makes no attempt to deeply merge properties.
     * Instead, the value will not be added.
     *
     * @param subject the subject to add the value to.
     * @param property the property that relates the value to the subject.
     * @param value the value to add.
     * @param [propertyIsArray] true if the property is always an array, false
     *          if not (default: false).
     * @param [propertyIsList] true if the property is a @list, false
     *          if not (default: false).
     */
    public static void addValue(Map<String, Object> subject, String property, Object value, boolean propertyIsArray, boolean propertyIsList) {
        if (value instanceof List) {
            if (((List) value).size() == 0 && propertyIsArray && !subject.containsKey(property)) {
                subject.put(property, new ArrayList<Object>());
            }
            for (Object val : (List) value) {
                addValue(subject, property, val, propertyIsArray, propertyIsList);
            }
        } else if (subject.containsKey(property)) {
            boolean hasValue = !propertyIsList && hasValue(subject, property, value);
            if (!(subject.get(property) instanceof List) && (!hasValue || propertyIsArray)) {
                List<Object> tmp = new ArrayList<Object>();
                tmp.add(subject.get(property));
                subject.put(property, tmp);
            }
            if (!hasValue) {
                ((List<Object>) subject.get(property)).add(value);
            }
        } else {
            Object tmp;
            if (propertyIsArray) {
                tmp = new ArrayList<Object>();
                ((List<Object>) tmp).add(value);
            } else {
                tmp = value;
            }
            subject.put(property, tmp);
        }
    }

    public static void addValue(Map<String, Object> subject, String property, Object value, boolean propertyIsArray) {
        addValue(subject, property, value, propertyIsArray, "@list".equals(property));
    }

    public static void addValue(Map<String, Object> subject, String property, Object value) {
        addValue(subject, property, value, false, "@list".equals(property));
    }

    /**
     * Determines if the given value is a property of the given subject.
     *
     * @param subject the subject to check.
     * @param property the property to check.
     * @param value the value to check.
     *
     * @return true if the value exists, false if not.
     */
    public static boolean hasValue(Map<String, Object> subject, String property, Object value) {
        boolean rval = false;
        if (hasProperty(subject, property)) {
            Object val = subject.get(property);
            boolean isList = (val instanceof Map && ((Map<String, Object>) val).containsKey("@list"));
            if (isList || val instanceof List) {
                if (isList) {
                    val = ((Map<String, Object>) val).get("@list");
                }
                for (Object i : (List) val) {
                    if (compareValues(value, i)) {
                        rval = true;
                        break;
                    }
                }
            } else if (!(value instanceof List)) {
                rval = compareValues(value, val);
            }
        }
        return rval;
    }

    private static boolean hasProperty(Map<String, Object> subject, String property) {
        boolean rval = false;
        if (subject.containsKey(property)) {
            Object value = subject.get(property);
            rval = (!(value instanceof List) || ((List) value).size() > 0);
        }
        return rval;
    }

    /**
     * Compares two JSON-LD values for equality. Two JSON-LD values will be
     * considered equal if:
     *
     * 1. They are both primitives of the same type and value.
     * 2. They are both @values with the same @value, @type, and @language, OR
     * 3. They both have @ids they are the same.
     *
     * @param v1 the first value.
     * @param v2 the second value.
     *
     * @return true if v1 and v2 are considered equal, false if not.
     */
    public static boolean compareValues(Object v1, Object v2) {
        if (v1.equals(v2)) {
            return true;
        }

        if ((v1 instanceof Map && ((Map<String, Object>) v1).containsKey("@value")) && (v2 instanceof Map && ((Map<String, Object>) v2).containsKey("@value"))
                && ((Map<String, Object>) v1).get("@value").equals(((Map<String, Object>) v2).get("@value"))
                && ((Map<String, Object>) v1).get("@type").equals(((Map<String, Object>) v2).get("@type"))
                && ((Map<String, Object>) v1).get("@language").equals(((Map<String, Object>) v2).get("@language"))) {
            return true;
        }

        if ((v1 instanceof Map && ((Map<String, Object>) v1).containsKey("@id")) && (v2 instanceof Map && ((Map<String, Object>) v2).containsKey("@id"))
                && ((Map<String, Object>) v1).get("@id").equals(((Map<String, Object>) v2).get("@id"))) {
            return true;
        }

        return false;
    }

    // END OF NEW CODE

    public static class NameGenerator {
        private String prefix;
        private int count;

        public NameGenerator(String prefix) {
            this.prefix = prefix;
            this.count = -1;
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

    public static Map<String, String> getKeywords(Object ctx) {
        Map<String, String> rval = new HashMap<String, String>();
        rval.put("@id", "@id");
        rval.put("@language", "@language");
        rval.put("@value", "@value");
        rval.put("@type", "@type");

        if (ctx != null && ctx instanceof Map) {
            Map<String, String> keywords = new HashMap<String, String>();
            for (String key : ((Map<String, Object>) ctx).keySet()) {
                Object value = ((Map<String, String>) ctx).get(key);
                if (value instanceof String && rval.containsKey(value)) {
                    keywords.put((String) value, key);
                }
            }

            rval.putAll(keywords);
        }
        return rval;
    }

    public static String getTermIri(Object ctx, String term) {
        String rval = null;

        if (((Map<String, Object>) ctx).containsKey(term)) {
            Object t = ((Map<String, Object>) ctx).get(term);
            if (t instanceof String) {
                rval = (String) t;
            } else if (t instanceof Map && ((Map<String, Object>) t).containsKey("@id")) {
                rval = (String) ((Map<String, Object>) t).get("@id");
            }
        }

        return rval;
    }

    public static Map<String, Object> mergeContexts(Object ctxOne, Object ctxTwo) {

        Map<String, Object> ctx1;
        Map<String, Object> ctx2;

        if (ctxOne instanceof List) {
            ctx1 = mergeContexts(new HashMap<String, Object>(), ctxOne);
        } else {
            ctx1 = (Map<String, Object>) ctxOne;
        }

        Map<String, Object> merged = (Map<String, Object>) clone(ctx1);

        if (ctxTwo instanceof List) {
            for (Object i : (List<Object>) ctxTwo) {
                merged = mergeContexts(merged, i);
            }
        } else {
            ctx2 = (Map<String, Object>) ctxTwo;

            for (String key : ctx2.keySet()) {
                if (!key.startsWith("@")) {
                    for (String mkey : merged.keySet()) {
                        if (merged.get(mkey).equals(ctx2.get(key))) {
                            // FIXME: update related @coerce rules
                            merged.remove(mkey);
                            break;
                        }
                    }
                }
            }

            // merge contexts
            for (String key : ctx2.keySet()) {
                merged.put(key, ctx2.get(key));
            }
        }
        return merged;
    }

    @Deprecated
    // TODO: it may make sense to keep this function, as usedCtx can be null
    public static String compactIRI(Map<String, Object> ctx, String iri) {
        return compactIRI(ctx, iri, null);
    }

    public static String compactIRI(Map<String, Object> ctx, String iri, Map<String, Object> usedCtx) {
        String rval = null;

        for (String key : ctx.keySet()) {
            if (!key.startsWith("@")) {
                if (iri.equals(getTermIri(ctx, key))) {
                    // compact to a term
                    rval = key;
                    if (usedCtx != null) {
                        usedCtx.put(key, clone(ctx.get(key)));
                    }
                    break;
                }
            }
        }

        // term not found, if term is keyword, use alias
        if (rval == null) {
            Map<String, String> keywords = getKeywords(ctx);
            if (keywords.containsKey(iri)) {
                rval = keywords.get(iri);
                if (!rval.equals(iri) && usedCtx != null) {
                    usedCtx.put(rval, iri);
                }
            }
        }

        if (rval == null) {
            // rval still not found, check the context for a CURIE prefix
            for (String key : ctx.keySet()) {
                if (!key.startsWith("@")) {
                    String ctxIRI = getTermIri(ctx, key);

                    if (ctxIRI != null) {
                        if (iri.startsWith(ctxIRI) && iri.length() > ctxIRI.length()) {
                            rval = key + ":" + iri.substring(ctxIRI.length());
                            if (usedCtx != null) {
                                usedCtx.put(key, clone(ctx.get(key)));
                            }
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

    @Deprecated
    // TODO: dep'd for backwards compatibility, but perhaps it's a valid
    // function as usedCtx can be null
    public static String getCoercionType(Map<String, Object> ctx, String property) {
        return getCoercionType(ctx, property, new HashMap<String, Object>());
    }

    public static String getCoercionType(Map<String, Object> ctx, String property, Map<String, Object> usedCtx) {
        String rval = null;

        // get expanded property
        String p = expandTerm(ctx, property, null);

        // built-in type coercion JSON-LD-isms
        if ("@id".equals(p) || "@type".equals(p)) {
            rval = "@id";
        } else {
            p = compactIRI(ctx, p, null);// , null);

            if (ctx.containsKey(p) && ctx.get(p) instanceof Map && ((Map<String, String>) ctx.get(p)).containsKey("@type")) {
                String type = ((Map<String, String>) ctx.get(p)).get("@type");
                rval = expandTerm(ctx, type, usedCtx);
                if (usedCtx != null) {
                    usedCtx.put(p, clone(ctx.get(p)));
                }
            }

        }

        return rval;
    }

    public static String expandTerm(Map<String, Object> ctx, String term, Map<String, Object> usedCtx) {
        Map<String, String> keywords = getKeywords(ctx);
        String rval = term;

        // 1. If the property has a colon, it has a prefix or an absolute IRI:
        int idx = term.indexOf(":");
        if (idx != -1) {
            String prefix = term.substring(0, idx);

            // 1.1 See if the prefix is in the context:
            if (ctx.containsKey(prefix)) {
                String iri = getTermIri(ctx, prefix);
                rval = iri + term.substring(idx + 1);
                if (usedCtx != null) {
                    usedCtx.put(prefix, clone(ctx.get(prefix)));
                }
            }
        } else if (ctx.containsKey(term)) {
            // 2. If the property is in the context, then it's a term.
            rval = getTermIri(ctx, term); // TODO: assuming string
            if (usedCtx != null) {
                usedCtx.put(term, clone(ctx.get(term)));
            }
        } else {
            // 3. The property is a keyword.
            for (String k : keywords.keySet()) {
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
        return (value != null && value instanceof Map && ((Map<String, Object>) value).containsKey("@id") && ((Map<String, Object>) value).size() == 1);
    }

    public static boolean isSubject(Object value) {
        boolean rval = false;
        if (value != null && value instanceof Map && !((Map<String, Object>) value).containsKey("@value")) {
            rval = ((Map<String, Object>) value).size() > 1 || !((Map<String, Object>) value).containsKey("@id");
        }
        return rval;
    }

    public static boolean isBlankNode(Object v) {
        return isSubject(v) && (!((Map<String, Object>) v).containsKey("@id") || isNamedBlankNode(v));
    }

    public static boolean isNamedBlankNode(Object v) {
        return v instanceof Map && ((Map<String, Object>) v).containsKey("@id") && isBlankNodeIri(((Map<String, Object>) v).get("@id"));
    }

    public static boolean isBlankNodeIri(Object input) {
        return input instanceof String && ((String) input).startsWith("_:");
    }

    public static Object clone(Object value) {// throws
                                              // CloneNotSupportedException {
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
            if (value instanceof String || value instanceof Number || value instanceof Boolean) {
                // strings numbers and booleans are immutable
                rval = value;
            } else {
                // TODO: making this throw runtime exception so it doesn't have
                // to be caught
                // because simply it should never fail in the case of JSON-LD
                // and means that
                // the input JSON-LD is invalid
                throw new RuntimeException(new CloneNotSupportedException((rval instanceof Exception ? ((Exception) rval).getMessage() : "")));
            }
        }
        return rval;
    }

    public static int compare(Object v1, Object v2) {
        int rval = 0;

        if (v1 instanceof List && v2 instanceof List) {
            if (((List) v1).size() != ((List) v2).size()) {
                rval = 1;
            } else {
                // TODO: should the order of things in the list matter?
                for (int i = 0; i < ((List<Object>) v1).size() && rval == 0; i++) {
                    rval = compare(((List<Object>) v1).get(i), ((List<Object>) v2).get(i));
                }
            }
        } else if (v1 instanceof Number && v2 instanceof Number) {
            // TODO: this is VERY sketchy
            double n1 = ((Number) v1).doubleValue();
            double n2 = ((Number) v2).doubleValue();

            rval = (n1 < n2 ? -1 : (n1 > n2 ? 1 : 0));
        } else if (v1 instanceof String && v2 instanceof String) {
            rval = ((String) v1).compareTo((String) v2);
            if (rval > 1)
                rval = 1;
            else if (rval < -1)
                rval = -1;
        } else if (v1 instanceof Map && v2 instanceof Map) {
            throw new RuntimeException("I don't know how I should handle this case yet!");
            /*
             * TODO: not sure what to do here exactly...
             * 
             * python can compare objects using the < and > operators. js pretends it can (i.e. it doesn't throw an error) but always returns false. thus the js
             * code and the py code are inconsistant.
             * 
             * // TODO: this assumes the order of keys doesn't matter if (((Map) v1).size() != ((Map) v2).size() ) { rval = 1; } else { if (((Map) v1).size() !=
             * ((Map) v2).size()) { rval = 1; } else { for (Object k1: ((Map) v1).keySet()) { rval = ((Map) v2).containsKey(k1) ? compare(((Map) v1).get(k1),
             * ((Map) v2).get(k1)) : 1; if (rval != 0) { break; } } } } } else if (v1 instanceof Boolean && v2 instanceof Boolean) { //rval = (v1 == v2 ? 0 :
             * 1);
             */
        } else {
            // TODO: this is probably something I don't want to allow either
            throw new RuntimeException("compare unspecified for these objects");
            // rval = (v1.equals(v2) ? 0 : 1);
        }
        return rval;
    }

    public static int compareBlankNodeObjects(Map<String, Object> a, Map<String, Object> b) {
        int rval = 0;

        // the keys tend to come up unsorted, so try the following lines if this
        // causes trouble
        // List<String> akeys = new ArrayList<String>();
        // akeys.addAll(a.keySet());
        // Collections.sort(akeys);

        for (String p : a.keySet()) {

            if (!p.equals("@id")) {
                int lenA = (a.get(p) instanceof List ? ((List<Object>) a.get(p)).size() : 1);
                int lenB = (b.get(p) instanceof List ? ((List<Object>) b.get(p)).size() : 1);
                rval = compare(lenA, lenB);

                if (rval == 0) {
                    List<Object> objsA;
                    List<Object> objsB;

                    if (a.get(p) instanceof List) {
                        objsA = (List<Object>) clone((List<Object>) a.get(p));
                        objsB = (List<Object>) clone((List<Object>) b.get(p));
                    } else {
                        objsA = new ArrayList<Object>();
                        objsA.add(a.get(p));
                        objsB = new ArrayList<Object>();
                        objsB.add(b.get(p));
                    }

                    for (int i = 0; i < objsA.size(); i++) {
                        Object e = objsA.get(i);
                        if (isNamedBlankNode(e)) {
                            objsA.remove(i);
                            --i;
                        }
                    }
                    for (int i = 0; i < objsB.size(); i++) {
                        Object e = objsB.get(i);
                        if (isNamedBlankNode(e)) {
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
        } else if (o1 instanceof Map) {
            rval = compareObjectKeys(o1, o2, "@value");
            if (rval == 0) {
                if (((Map) o1).containsKey("@value")) {
                    rval = compareObjectKeys(o1, o2, "@type");
                    if (rval == 0) {
                        rval = compareObjectKeys(o1, o2, "@language");
                    }
                } else {
                    rval = compare(((Map<String, Object>) o1).get("@id"), ((Map<String, Object>) o2).get("@id"));
                }
            }

        }
        return rval;
    }

    private static int compareObjectKeys(Object o1, Object o2, String key) {
        int rval = 0;
        if (((Map<String, Object>) o1).containsKey(key)) {
            if (((Map<String, Object>) o2).containsKey(key)) {
                rval = compare(((Map<String, Object>) o1).get(key), ((Map<String, Object>) o2).get(key));
            } else {
                rval = -1;
            }
        } else if (((Map<String, Object>) o2).containsKey(key)) {
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
            rval = compare(s1, s2);
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
        for (String p : b.keySet()) {
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

                for (Object o : objs) {
                    if (o instanceof Map) {
                        if (((Map) o).containsKey("@id")) { // iri
                            if (isBlankNodeIri(((Map<String, Object>) o).get("@id"))) {
                                rval += "_:";
                            } else {
                                rval += "<" + ((Map<String, Object>) o).get("@id") + ">";
                            }
                        } else { // literal
                            rval += "\"" + ((Map<String, Object>) o).get("@value") + "\"";
                            if (((Map<String, Object>) o).containsKey("@type")) {
                                rval += "^^<" + ((Map<String, Object>) o).get("@type") + ">";
                            } else if (((Map<String, Object>) o).containsKey("@language")) {
                                rval += "@" + ((Map<String, Object>) o).get("@language");
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

    public static void setProperty(Map<String, Object> s, String p, Object o) {
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

    /**
     * Recursively frames the given input according to the given frame.
     * 
     * @param subjects
     *            a map of subjects in the graph.
     * @param input
     *            the input to frame.
     * @param frame
     *            the frame to use.
     * @param embeds
     *            a map of previously embedded subjects, used to prevent cycles.
     * @param autoembed
     *            true if auto-embed is on, false if not.
     * @param parent
     *            the parent object (for subframing).
     * @param parentKey
     *            the parent key (for subframing).
     * @param options
     *            the framing options.
     * @return the framed input.
     */
    public static Object frame(Map subjects, List input, Object frame, HashMap embeds, boolean autoembed, Object parent, String parentKey, Map options) {
        Object rval = null;

        int limit = -1;
        List frames = null;

        if (frame instanceof List) {
            rval = new ArrayList();
            frames = (List) frame;
            if (frames.isEmpty()) {
                frames.add(new HashMap());
            }
        } else {
            frames = new ArrayList();
            frames.add(frame);
            limit = 1;
        }

        boolean omitOn = false;
        if (options != null) {
            if (options.containsKey("defaults")) {
                Map defopts = (Map) options.get("defaults");
                if (defopts.containsKey("omitDefaultOn")) {
                    omitOn = (Boolean) defopts.get("omitDefaultOn");
                }
            }
        }

        List<List> values = new ArrayList<List>();
        for (Object f : frames) {
            if (!(f instanceof List || f instanceof Map)) {
                throw new RuntimeException("Invalid JSON - LD frame. Frame type is not a map or array.");
            }
            List vali = new ArrayList();
            for (Object n : input) {
                if (n instanceof Map && ((Map) n).containsKey("@id") && subjects.containsKey(((Map) n).get("@id"))) {
                    n = subjects.get(((Map) n).get("@id"));
                }

                if (JSONLDUtils.isType(n, (Map) f) || JSONLDUtils.isDuckType(n, (Map) f)) {
                    vali.add(n);
                    limit -= 1;
                    if (limit == 0) {
                        break;
                    }
                }
            }
            values.add(vali);
            if (limit == 0) {
                break;
            }
        }

        for (int i = 0; i < frames.size(); i++) {
            Object f = frames.get(i);
            List v = values.get(i);

            for (Object value : v) {
                if (JSONLDUtils.isSubject(value)) {
                    value = subframe(subjects, (Map) value, (Map) f, embeds, autoembed, parent, parentKey, options);
                }

                if (rval == null) {
                    rval = value;
                } else {
                    if (!((JSONLDUtils.isReference(value) && embeds.containsKey(((Map) value).get("@id"))) && parent == null)) {
                        ((List) rval).add(value);
                    }
                }
            }
        }

        return rval;
    }

    /**
     * Subframes a value.
     * 
     * @param subjects
     *            a map of subjects in the graph.
     * @param value
     *            the value to subframe.
     * @param frame
     *            the frame to use.
     * @param embeds
     *            a map of previously embedded subjects, used to prevent cycles.
     * @param autoembed
     *            true if auto-embed is on, false if not.
     * @param parent
     *            the parent object.
     * @param parentKey
     *            the parent key.
     * @param options
     *            the framing options.
     * @return the framed input.
     */
    private static Object subframe(Map subjects, Map value, Map frame, HashMap embeds, boolean autoembed, Object parent, String parentKey, Map options) {
        String iri = (String) value.get("@id");
        Map embed = (Map) embeds.get(iri);

        boolean embedOn = (((frame.containsKey("@embed") && (Boolean) frame.get("@embed")) || (!frame.containsKey("@embed") && (Boolean) ((Map) options
                .get("defaults")).get("embedOn"))) && (embed == null || ((Boolean) embed.get("autoembed") && !autoembed)));

        if (!embedOn) {
            Map tmp = new HashMap();
            tmp.put("@id", value.get("@id"));
            return tmp;
        } else {
            if (embed == null) {
                embed = new HashMap();
                embeds.put(iri, embed);
            } else if (embed.get("parent") != null) {
                Object objs = ((Map) embed.get("parent")).get(embed.get("key"));
                if (objs instanceof List) {
                    for (int i = 0; i < ((List) objs).size(); i++) {
                        Object oi = ((List) objs).get(i);
                        if (oi instanceof Map && ((Map) oi).containsKey("@id") && ((Map) oi).get("@id").equals(iri)) {
                            Map tmp = new HashMap();
                            tmp.put("@id", value.get("id"));
                            ((List) objs).set(i, tmp);
                            break;
                        }
                    }
                } else {
                    Map tmp = new HashMap();
                    tmp.put("@id", value.get("@id"));
                    ((Map) embed.get("parent")).put(embed.get("key"), tmp);
                }

                removeDependentEmbeds(iri, embeds);
            }

            embed.put("autoembed", autoembed);
            embed.put("parent", parent);
            embed.put("key", parentKey);

            boolean explicitOn = (frame.containsKey("@explicit") ? (Boolean) frame.get("@explicit") : (Boolean) ((Map) options.get("defaults"))
                    .get("explicitOn"));

            if (explicitOn) {
                for (String key : new HashSet<String>((Set<String>) value.keySet())) {
                    if (!"@id".equals(key) && !frame.containsKey(key)) {
                        value.remove(key);
                    }
                }
            }

            for (String key : (Set<String>) value.keySet()) {
                Object v = value.get(key);
                if (!key.startsWith("@")) {
                    Object f = frame.get(key);
                    boolean _autoembed = (f == null);
                    if (_autoembed) {
                        f = value.get(key) instanceof List ? new ArrayList() : new HashMap();
                    }
                    List input = null;
                    if (value.get(key) instanceof List) {
                        input = (List) value.get(key);
                    } else {
                        List tmp = new ArrayList();
                        tmp.add(value.get(key));
                        input = tmp;
                    }
                    for (int n = 0; n < input.size(); n++) {
                        Object in = input.get(n);
                        if (in instanceof Map && ((Map) in).containsKey("@id") && subjects.containsKey(((Map) in).get("@id"))) {
                            input.set(n, subjects.get(((Map) in).get("@id")));
                        }
                    }
                    value.put(key, frame(subjects, input, f, embeds, _autoembed, value, key, options));
                }
            }

            for (String key : (Set<String>) frame.keySet()) {
                Object f = frame.get(key);
                if (!key.startsWith("@") && (!value.containsKey(key) || value.get(key) == null)) {
                    if (f instanceof List) {
                        value.put(key, new ArrayList());
                    } else {
                        // TODO: jsonld.js and pyld.js have a block here that will never be reached
                        // because they can only run if the previous if was true

                        boolean omitOn = ((Map) f).containsKey("@omitDefault") ? (Boolean) ((Map) f).get("@omitDefault") : (Boolean) ((Map) options
                                .get("defaults")).get("omitDefaultOn");

                        if (!omitOn) {
                            if (((Map) f).containsKey("@default")) {
                                value.put(key, ((Map) f).get("@default"));
                            } else {
                                value.put(key, null);
                            }
                        }
                    }
                }
            }
        }
        return value;
    }

    private static void removeDependentEmbeds(String iri, Map embeds) {
        Set<String> iris = new HashSet(embeds.keySet());
        for (String i : iris) {
            if (embeds.containsKey(i) && ((Map) embeds.get(i)).get("parent") != null && iri.equals(((Map) ((Map) embeds.get(i)).get("parent")).get("@id"))) {
                embeds.remove(i);
                removeDependentEmbeds(i, embeds);
            }
        }
    }

    /**
     * Returns True if the given src matches the given frame via duck-typing.
     * 
     * @param src
     *            the input.
     * @param frame
     *            the frame to check against.
     * @return True if the src matches the frame.
     */
    private static boolean isDuckType(Object src, Map frame) {
        String rType = "@type";
        if (!frame.containsKey(rType)) {
            Set<String> props1 = frame.keySet();
            Set<String> props = new HashSet<String>();
            for (String p : props1) {
                if (!p.startsWith("@")) {
                    props.add(p);
                }
            }
            if (props.isEmpty()) {
                return true;
            }
            if (src instanceof Map && ((Map) src).containsKey("@id")) {
                for (String i : props) {
                    if (!((Map) src).containsKey(i)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Returns True if the given source is a subject and has one of the given types in the given frame.
     * 
     * @param src
     *            the input.
     * @param frame
     *            the frame with types to look for.
     * @return True if the src has one of the given types.
     */
    private static boolean isType(Object src, Map frame) {
        String rType = "@type";
        if (frame.containsKey(rType) && src instanceof Map && ((Map) src).containsKey(rType)) {
            List tmp = null;
            if (((Map) src).get(rType) instanceof List) {
                tmp = (List) ((Map) src).get(rType);
            } else {
                tmp = new ArrayList();
                tmp.add(((Map) src).get(rType));
            }
            List types = null;
            if (frame.get(rType) instanceof List) {
                types = (List) frame.get(rType);
            } else {
                types = new ArrayList();
                types.add(frame.get(rType));
            }

            for (Object typ : types) {
                for (Object i : tmp) {
                    if (i.equals(typ)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
