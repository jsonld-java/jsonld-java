package de.dfki.km.json.jsonld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.Writer;

import de.dfki.km.json.JSONUtils;

/**
 * TODO:
 * 
 * @author tristan
 * 
 */
public class JSONLDSerializer {

    private Map<String, Map<String, Object>> _subjects;
    private Map<String, Object> _context;

    JSONLDUtils.NameGenerator _ng;
    Map<String, String> _bns;

    public JSONLDSerializer() {
        reset();
    }

    /**
     * Resets the Serializer. Call this if you want to reuse the serializer for a different document
     */
    public void reset() {
        _subjects = new HashMap<String, Map<String, Object>>();
        _context = new HashMap<String, Object>();
        _context.put("rdf", JSONLDConsts.RDF_SYNTAX_NS);
        _context.put("rdfs", JSONLDConsts.RDF_SCHEMA_NS);
        _context.put("xsd", JSONLDConsts.XSD_NS);

        _ng = new JSONLDUtils.NameGenerator("bn");
        _bns = new HashMap<String, String>();
    }

    // some helper functions for extended classes
    protected Map<String, Object> getSubject(String subjURI) {
        return _subjects.get(subjURI);
    }

    protected void setSubject(String subjURI, Map<String, Object> subj) {
        _subjects.put(subjURI, subj);
    }

    protected String getNameForBlankNode(String node) {
        if (!_bns.containsKey(node)) {
            _bns.put(node, _ng.next());
        }
        return _bns.get(node);
    }

    /**
     * internal function to avoid repetition of code
     * 
     * @param s
     * @param p
     * @param value
     */
    private void triple(String s, String p, Object value) {
        // get the cached object for this subject
        Map<String, Object> subj = _subjects.get(s);
        if (subj == null) {
            // if no cached object exists, create one.
            subj = new HashMap<String, Object>();
            _subjects.put(s, subj);

            // since this is the first time we're encountering this subject, add
            // it's @id key
            subj.put("@id", s);
        }

        // check for @type predicate
        if (JSONLDConsts.RDF_TYPE.equals(p)) {
            p = "@type";
            // type doesn't need to be (TODO: should it not be?) in an object
            if (value instanceof Map) {
                if (((Map) value).containsKey("@id")) {
                    value = ((Map) value).get("@id");
                } else if (((Map) value).containsKey("@value")) {
                    // this is a weird case, which probably shouldn't happen
                    // but i'll account for it anyway.
                    value = ((Map) value).get("@value");
                } // otherwise it's a string, leave it as such (TODO: it should
                  // never be a list, but perhaps we should make sure)
            }
        }

        Object oldval = subj.get(p);
        // check if an existing value is present for this predicate
        if (oldval != null) {
            // if so, make the value a list (if it isn't already) and add the
            // new value to the end of the list
            // TODO: what if the value itself is a list, is this even possible?
            if (oldval instanceof List) {
                ((List) oldval).add(value);
                value = oldval;
            } else {
                List<Object> tmp = new ArrayList<Object>();
                tmp.add(oldval);
                tmp.add(value);
                value = tmp;
            }
        }
        // add the new value to the cache object
        subj.put(p, value);
    }

    /**
     * Call this to add a literal to the JSON-LD document
     * 
     * @param s
     *            the subjuct URI
     * @param p
     *            the predicate URI
     * @param value
     *            the literal value as a string
     * @param datatype
     *            the datatype URI (if null, a plain literal is assumed)
     * @param language
     *            the language (may be null, or an empty string)
     */
    public void triple(String s, String p, String value, String datatype, String language) {

        Object val;
        if (datatype == null || JSONLDConsts.XSD_STRING.equals(datatype)) {
            if (language == null || "".equals(language)) {
                val = value;
            } else {
                val = new HashMap<String, Object>();
                ((Map<String, Object>) val).put("@value", value);
                ((Map<String, Object>) val).put("@language", language);
            }
        } else {
            // compact the datatype (will return the same thing if no prefix has
            // been set for the uri)
            // datatype = JSONLDUtils.compactIRI(_context, datatype); NOTE:
            // going to leave this up to the user
            val = new HashMap<String, Object>();
            ((Map<String, Object>) val).put("@value", value);
            ((Map<String, Object>) val).put("@type", datatype);
        }

        triple(s, p, val);
    }

    /**
     * Call this to add a new object,predicate,object relation to the JSON-LD document
     * 
     * @param s
     *            the subject URI
     * @param p
     *            the predicate URI
     * @param o
     *            the object URI
     */
    public void triple(String s, String p, String o) {
        Map<String, Object> val = new HashMap<String, Object>();
        val.put("@id", o);

        // TODO: should we check if this object exists in the _subjects list and
        // add it if it doesn't?
        // this seems to be what the python rdflib serializer does, but it seems
        // redundant.
        triple(s, p, (Object) val);
    }

    public void setPrefix(String fullUri, String prefix) {
        _context.put(prefix, fullUri);
    }

    /**
     * Builds the JSON-LD document based on the currently stored triples, compacting the URIs based on the stored context.
     * 
     * @return A Map representing the JSON-LD document.
     */
    public Object asObject() {
        JSONLDProcessor p = new JSONLDProcessor();

        // go through the list of subjects that were built, compact them based
        // of the current context
        // then add them to a subjects list
        List<Object> subjects = new ArrayList<Object>();
        for (Map<String, Object> subj : _subjects.values()) {
            subj = (Map<String, Object>) p.compact(_context, subj);
            subjects.add(subj);
        }

        if (subjects.size() == 1) {
            // if there is only one subject, make the base object this object
            Map<String, Object> rval = new HashMap<String, Object>();
            rval = (Map<String, Object>) subjects.get(0);
            rval.put("@context", _context);
            return rval;
        } else {
            return subjects;
        }
    }

    public void toWriter(Writer writer) {
        try {
            JSONUtils.writePrettyPrint(writer, asObject());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 
     * @return A String representing the JSON-LD document.
     */
    public String asString() {
        return asString(false);
    }

    /**
     * Returns a formated String representing the JSON-LD document using PrettyPrint writer.
     * @return A String representing the JSON-LD document.
     */
    public String asString(boolean prettyPrint) {
        // TODO: catching the exceptions here and returning JSON with the error
        // messages may not
        // be the best idea
        try {
            if (!prettyPrint)
                return JSONUtils.toString(asObject());
            else
                return JSONUtils.toPrettyString(asObject());
        } catch (Exception e) {
            return "{\"error\":\"" + e.getLocalizedMessage() + "\"}";
        }
    }    

}
