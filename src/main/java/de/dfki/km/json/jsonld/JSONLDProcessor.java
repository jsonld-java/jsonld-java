package de.dfki.km.json.jsonld;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public Object expand(Object input) {
        return expand(new HashMap<String, Object>(), null, input);
    }

    private Object expand(Map<String, Object> ctx, Object property, Object value) {
        Object rval = null;
        if (value == null) {
            return null;
        } else if (property == null && value instanceof String) {
            rval = JSONLDUtils.expandTerm(ctx, (String) value);// , null));
        } else if (value instanceof List) {
            rval = new ArrayList<Object>();
            for (Object i : (List) value) {
                ((List) rval).add(expand(ctx, property, i));
            }
        } else if (value instanceof Map) {
            rval = new HashMap<String, Object>();
            if (((Map<String, Object>) value).containsKey("@context")) {
                try {
                    ctx = JSONLDUtils.mergeContexts((Map<String, Object>) ctx, (Map<String, Object>) ((Map<String, Object>) value).get("@context"));
                } catch (Exception e) {
                    // unable to merge contexts
                    // TODO: this should probably just throw back to the calling
                    // function
                    e.printStackTrace();
                    return null;
                }
            }

            rval = new HashMap<String, Object>();
            for (String key : ((Map<String, Object>) value).keySet()) {
                if ("@embed".equals(key) || "@explicit".equals(key) || "@default".equals(key) || "@omitDefault".equals(key) || ignoredKeywords.contains(key)) {
                    JSONLDUtils.setProperty((Map<String, Object>) rval, key, JSONLDUtils.clone(((Map<String, Object>) value).get(key)));
                } else if (!"@context".equals(key)) {
                    JSONLDUtils.setProperty((Map<String, Object>) rval, JSONLDUtils.expandTerm(ctx, key),
                            expand(ctx, key, ((Map<String, Object>) value).get(key)));
                }
            }
        } else {
            // do type coercion
            String coerce = JSONLDUtils.getCoercionType(ctx, (String) property);// ,
                                                                                // null);
            Map<String, String> keywords = JSONLDUtils.getKeywords(ctx);

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
            if (property.equals("@id") || property.equals(keywords.get("@id")) || property.equals("@type") || property.equals(keywords.get("@type"))) {
                rval = JSONLDUtils.expandTerm(ctx, (String) value);
            }
            // coerce to appropriate type
            else if (coerce != null) {
                rval = new HashMap<String, Object>();

                if (coerce.equals("@id")) {
                    // expand IRI
                    ((Map<String, Object>) rval).put("@id", JSONLDUtils.expandTerm(ctx, (String) value)); // ,
                                                                                                          // null));
                } else {
                    ((Map<String, Object>) rval).put("@type", coerce);
                    if (coerce.equals(JSONLDConsts.XSD_DOUBLE)) {
                        DecimalFormat decimalFormat = new DecimalFormat("0.000000E0", new DecimalFormatSymbols(Locale.US));
                        Double v = null;
                        if (value instanceof String) {
                            v = Double.parseDouble((String) value);
                        } else if (value instanceof Integer) {
                            // TODO: what is this? really?
                            v = new Double(1.0 * (Integer) value);
                        } else {
                            v = (Double) value;
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
                    } else {
                        value = value.toString();
                    }
                    ((Map<String, Object>) rval).put("@value", value);
                }
            } else {
                // nothing to coerce
                rval = value.toString();
            }
        }

        return rval;
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

            Object expanded = expand(new HashMap<String, Object>(), null, input);

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

    /*
     * public Object simplify(Map input) { return simplify(input, new HashMap()); }
     */

    public static void generateSimplifyContext(Object input, Map<String, Object> ctx) {
        if (input instanceof List) {
            for (Object o : (List) input) {
                generateSimplifyContext(o, ctx);
            }
        } else if (input instanceof Map) {
            Map<String, Object> o = (Map<String, Object>) input;
            for (String key : o.keySet()) {
                Object val = o.get(key);
                if (key.matches("^https?://.+$")) {
                    int idx = key.lastIndexOf('#');
                    if (idx < 0) {
                        idx = key.lastIndexOf('/');
                    }
                    String skey = key.substring(idx + 1);
                    Object keyval = key;
                    if (val instanceof Map) {
                        if (((Map) val).containsKey("@id")) {
                            Map tmp = new HashMap();
                            tmp.put("@type", "@id");
                            tmp.put("@id", key);
                            keyval = tmp;
                        }
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
                if (val instanceof Map || val instanceof List) {
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

        Map<String, Object> origCtx = (Map<String, Object>) input.get("@context");
        Object expanded = expand(input);
        Map<String, Object> framectx = new HashMap<String, Object>();

        generateSimplifyContext(expanded, framectx);

        if (origCtx != null) {
            framectx = JSONLDUtils.mergeContexts(origCtx, framectx);
        }

        return compact(framectx, expanded);
    }

    private Map<String, Object> nestCore(Map<String, Object> rootObj, Map<String, Map<String, Object>> otherObjs) {
        Map<String, Object> rval = new HashMap<String, Object>();
        for (String key : rootObj.keySet()) {
            Object val = rootObj.get(key);
            if (ignoredKeywords.contains(key)) {
            	rval.put(key, val);
            } else if (val instanceof List) {
                List<Object> lv = new ArrayList<Object>();
                for (Map<String, Object> o : (List<Map<String, Object>>) val) {
                    lv.add(nestCore(o, otherObjs));
                }
                rval.put(key, lv);
            } else if (val instanceof Map) {
                // TODO: should this be true as well? ((Map) val).size() == 1
                if (((Map) val).containsKey("@id") && otherObjs.containsKey(((Map) val).get("@id"))) {
                    rval.put(key, otherObjs.get(((Map) val).get("@id")));
                } else {
                    rval.put(key, nestCore((Map<String, Object>) val, otherObjs));
                }
            } else {
                rval.put(key, val);
            }
        }
        return rval;
    }

    /**
     * expands all the objects in the input list, and nests all the non-root object lists
     * into the root object, resulting in a single object
     * 
     * TODO: i'm not sure if the way i'm keeping the original context is what i really want 
     * 
     * @param input
     * @param rootObjId
     * @return
     */
    public Object nest(List<Map<String, Object>> input, String rootObjId) {
        Map<String, Object> rootObj = null;
        Map<String, Object> rootObjCtx = null;
        Map<String, Map<String, Object>> otherObjs = new HashMap<String, Map<String, Object>>();
        // find the root object and build a map of the non-root objects mapped by id
        for (Map<String, Object> item : input) {
            if (item.containsKey("@id")) {
                if (rootObjId.equals(item.get("@id"))) {
                    rootObjCtx = (Map<String, Object>) item.get("@context");
                    rootObj = (Map<String, Object>) expand(item);
                } else {
                    //Map<String, Object> clone = (Map<String, Object>) JSONLDUtils.clone(item);
                    //clone.remove("@id");
                    otherObjs.put((String) item.get("@id"), (Map<String, Object>) expand(item));
                }
            }
        }

        if (rootObj == null) {
            // no object matching the root object found, should probably return an error actually
            // TODO: throw runtimeexception when the rest of the library is updated
            return input;
        }

        // this nests all the elements in the input once (not including the root object)
        Map<String, Map<String, Object>> nestedOthers = new HashMap<String, Map<String, Object>>();
        for (String key : otherObjs.keySet()) {
            Map<String, Object> val = otherObjs.get(key);
            nestedOthers.put(key, nestCore(val, otherObjs));
        }
        // nests the root object with the nested other objects
        Map<String, Object> rval = nestCore(rootObj, nestedOthers);

        // compact the results with the original root object's context
        return compact(rootObjCtx != null ? rootObjCtx : new HashMap<String, Object>(), rval);
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
                if (!key.equals("@id") && !ignoredKeywords.contains(key)) {
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
