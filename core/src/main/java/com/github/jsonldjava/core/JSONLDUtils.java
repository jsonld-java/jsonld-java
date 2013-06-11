package com.github.jsonldjava.core;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonParseException;
import com.github.jsonldjava.utils.JSONUtils;
import com.github.jsonldjava.utils.Obj;
import com.github.jsonldjava.utils.URL;

public class JSONLDUtils {

    private static final int MAX_CONTEXT_URLS = 10;

    /**
     * Returns whether or not the given value is a keyword (or a keyword alias).
     * 
     * @param v
     *            the value to check.
     * @param [ctx] the active context to check against.
     * 
     * @return true if the value is a keyword, false if not.
     */
    static boolean isKeyword(Object key) {
        if (!isString(key)) {
            return false;
        }
        return "@base".equals(key) || "@context".equals(key) || "@container".equals(key)
                || "@default".equals(key) || "@embed".equals(key) || "@explicit".equals(key)
                || "@graph".equals(key) || "@id".equals(key) || "@index".equals(key)
                || "@language".equals(key) || "@list".equals(key) || "@omitDefault".equals(key)
                || "@reverse".equals(key) || "@preserve".equals(key) || "@set".equals(key)
                || "@type".equals(key) || "@value".equals(key) || "@vocab".equals(key);
    }

    static boolean isAbsoluteIri(String value) {
        return value.contains(":");
    }

    /**
     * Adds a value to a subject. If the value is an array, all values in the
     * array will be added.
     * 
     * Note: If the value is a subject that already exists as a property of the
     * given subject, this method makes no attempt to deeply merge properties.
     * Instead, the value will not be added.
     * 
     * @param subject
     *            the subject to add the value to.
     * @param property
     *            the property that relates the value to the subject.
     * @param value
     *            the value to add.
     * @param [propertyIsArray] true if the property is always an array, false
     *        if not (default: false).
     * @param [allowDuplicate] true if the property is a @list, false if not
     *        (default: false).
     */
    static void addValue(Map<String, Object> subject, String property, Object value,
            boolean propertyIsArray, boolean allowDuplicate) {

        if (isArray(value)) {
            if (((List) value).size() == 0 && propertyIsArray && !subject.containsKey(property)) {
                subject.put(property, new ArrayList<Object>());
            }
            for (final Object val : (List) value) {
                addValue(subject, property, val, propertyIsArray, allowDuplicate);
            }
        } else if (subject.containsKey(property)) {
            // check if subject already has the value if duplicates not allowed
            final boolean hasValue = !allowDuplicate && hasValue(subject, property, value);

            // make property an array if value not present or always an array
            if (!isArray(subject.get(property)) && (!hasValue || propertyIsArray)) {
                final List<Object> tmp = new ArrayList<Object>();
                tmp.add(subject.get(property));
                subject.put(property, tmp);
            }

            // add new value
            if (!hasValue) {
                ((List<Object>) subject.get(property)).add(value);
            }
        } else {
            // add new value as a set or single value
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

    static void addValue(Map<String, Object> subject, String property, Object value,
            boolean propertyIsArray) {
        addValue(subject, property, value, propertyIsArray, true);
    }

    static void addValue(Map<String, Object> subject, String property, Object value) {
        addValue(subject, property, value, false, true);
    }

    /**
     * Creates a term definition during context processing.
     * 
     * @param activeCtx
     *            the current active context.
     * @param localCtx
     *            the local context being processed.
     * @param term
     *            the term in the local context to define the mapping for.
     * @param defined
     *            a map of defining/defined keys to detect cycles and prevent
     *            double definitions.
     * @throws JSONLDProcessingError
     */
    static void createTermDefinition(ActiveContext activeCtx, Map<String, Object> localCtx,
            String term, Map<String, Boolean> defined) throws JSONLDProcessingError {
        if (defined.containsKey(term)) {
            // term already defined
            if (defined.get(term)) {
                return;
            }
            // cycle detected
            throw new JSONLDProcessingError("Cyclical context definition detected.")
                    .setType(JSONLDProcessingError.Error.CYCLICAL_CONTEXT)
                    .setDetail("context", localCtx).setDetail("term", term);
        }

        // now defining term
        defined.put(term, false);

        if (isKeyword(term)) {
            throw new JSONLDProcessingError(
                    "Invalid JSON-LD syntax; keywords cannot be overridden.").setType(
                    JSONLDProcessingError.Error.SYNTAX_ERROR).setDetail("context", localCtx);
        }

        // remove old mapping
        activeCtx.mappings.remove(term);

        // get context term value
        Object value = localCtx.get(term);

        // clean context entry
        if (value == null
                || (isObject(value) && // NOTE: object[key] === null will return
                                       // false if the key doesn't exist in the
                                       // object
                        ((Map<String, Object>) value).containsKey("@id") && ((Map<String, Object>) value)
                        .get("@id") == null)) {
            activeCtx.mappings.put(term, null);
            defined.put(term, true);
            return;
        }

        // convert short-hand value to object w/@id
        if (isString(value)) {
            final Map<String, Object> tmp = new LinkedHashMap<String, Object>();
            tmp.put("@id", value);
            value = tmp;
        }

        if (!isObject(value)) {
            throw new JSONLDProcessingError(
                    "Invalid JSON-LD syntax; @context property values must be string or objects.")
                    .setDetail("context", localCtx).setType(
                            JSONLDProcessingError.Error.SYNTAX_ERROR);
        }

        final Map<String, Object> val = (Map<String, Object>) value;

        // create new mapping
        final Map<String, Object> mapping = new LinkedHashMap<String, Object>();
        activeCtx.mappings.put(term, mapping);
        mapping.put("reverse", false);

        if (val.containsKey("@reverse")) {
            if (val.containsKey("@id") || val.containsKey("@type") || val.containsKey("@language")) {
                throw new JSONLDProcessingError(
                        "Invalid JSON-LD syntax; a @reverse term definition must not contain @id, @type or @language.")
                        .setDetail("context", localCtx).setType(
                                JSONLDProcessingError.Error.SYNTAX_ERROR);
            }
            if (!isString(val.get("@reverse"))) {
                throw new JSONLDProcessingError(
                        "Invalid JSON-LD syntax; a @context @reverse value must be a string.")
                        .setDetail("context", localCtx).setType(
                                JSONLDProcessingError.Error.SYNTAX_ERROR);
            }
            final String reverse = (String) val.get("@reverse");

            // expand and add @id mapping, set @type to @id
            mapping.put("@id", expandIri(activeCtx, reverse, false, true, localCtx, defined));
            mapping.put("@type", "@id");
            mapping.put("reverse", true);
        } else if (val.containsKey("@id")) {
            if (!isString(val.get("@id"))) {
                throw new JSONLDProcessingError(
                        "Invalid JSON-LD syntax; a @context @id value must be an array of strings or a string.")
                        .setDetail("context", localCtx).setType(
                                JSONLDProcessingError.Error.SYNTAX_ERROR);
            }
            final String id = (String) val.get("@id");
            if (id != null && !id.equals(term)) {
                // expand and add @id mapping
                mapping.put("@id", expandIri(activeCtx, id, false, true, localCtx, defined));
            }
        }

        if (!mapping.containsKey("@id")) {
            // see if the term has a prefix
            final int colon = term.indexOf(':');
            if (colon != -1) {
                final String prefix = term.substring(0, colon);
                if (localCtx.containsKey(prefix)) {
                    // define parent prefix
                    createTermDefinition(activeCtx, localCtx, prefix, defined);
                }

                // set @id based on prefix parent
                if (activeCtx.mappings.containsKey(prefix)) {
                    final String suffix = term.substring(colon + 1);
                    mapping.put(
                            "@id",
                            (String) ((Map<String, Object>) activeCtx.mappings.get(prefix))
                                    .get("@id") + suffix);
                }
                // term is an absolute IRI
                else {
                    mapping.put("@id", term);
                }
            } else {
                // non-IRIs *must* define @ids if @vocab is not available
                if (!activeCtx.containsKey("@vocab")) {
                    throw new JSONLDProcessingError(
                            "Invalid JSON-LD syntax; @context terms must define an @id.")
                            .setDetail("context", localCtx).setDetail("term", term)
                            .setType(JSONLDProcessingError.Error.SYNTAX_ERROR);
                }
                // prepend vocab to term
                mapping.put("@id", (String) activeCtx.get("@vocab") + term);
            }
        }

        // IRI mapping now defined
        defined.put(term, true);

        if (val.containsKey("@type")) {
            if (!isString(val.get("@type"))) {
                throw new JSONLDProcessingError(
                        "Invalid JSON-LD syntax; a @context @type values must be strings.")
                        .setDetail("context", localCtx).setType(
                                JSONLDProcessingError.Error.SYNTAX_ERROR);
            }
            String type = (String) val.get("@type");
            if (!"@id".equals(type)) {
                // expand @type to full IRI
                type = expandIri(activeCtx, type, true, true, localCtx, defined);
            }
            mapping.put("@type", type);
        }

        if (val.containsKey("@container")) {
            final String container = (String) val.get("@container");
            if (!"@list".equals(container) && !"@set".equals(container)
                    && !"@index".equals(container) && !"@language".equals(container)) {
                throw new JSONLDProcessingError(
                        "Invalid JSON-LD syntax; @context @container value must be one of the following: "
                                + "@list, @set, @index or @language.").setDetail("context",
                        localCtx).setType(JSONLDProcessingError.Error.SYNTAX_ERROR);
            }
            if ((Boolean) mapping.get("reverse") && !"@index".equals(container)) {
                throw new JSONLDProcessingError(
                        "Invalid JSON-LD syntax; @context @container value for a @reverse type "
                                + "definition must be @index.").setDetail("context", localCtx)
                        .setType(JSONLDProcessingError.Error.SYNTAX_ERROR);
            }

            // add @container to mapping
            mapping.put("@container", container);
        }

        if (val.containsKey("@language") && !val.containsKey("@type")) {

            if (val.get("@language") != null && !isString(val.get("@language"))) {
                throw new JSONLDProcessingError(
                        "Invalid JSON-LD syntax; @context @language value value must be a string or null.")
                        .setDetail("context", localCtx).setType(
                                JSONLDProcessingError.Error.SYNTAX_ERROR);
            }
            String language = (String) val.get("@language");

            // add @language to mapping
            if (language != null) {
                language = language.toLowerCase();
            }
            mapping.put("@language", language);
        }

        // disallow aliasing @context and @preserve
        final String id = (String) mapping.get("@id");
        if ("@context".equals(id) || "@preserve".equals(id)) {
            throw new JSONLDProcessingError(
                    "Invalid JSON-LD syntax; @context and @preserve cannot be aliased.")
                    .setType(JSONLDProcessingError.Error.SYNTAX_ERROR);
        }
    }

    /**
     * Expands a string to a full IRI. The string may be a term, a prefix, a
     * relative IRI, or an absolute IRI. The associated absolute IRI will be
     * returned.
     * 
     * @param activeCtx
     *            the current active context.
     * @param value
     *            the string to expand.
     * @param relativeTo
     *            options for how to resolve relative IRIs: base: true to
     *            resolve against the base IRI, false not to. vocab: true to
     *            concatenate after @vocab, false not to.
     * @param localCtx
     *            the local context being processed (only given if called during
     *            context processing).
     * @param defined
     *            a map for tracking cycles in context definitions (only given
     *            if called during context processing).
     * 
     * @return the expanded value.
     * @throws JSONLDProcessingError
     */
    static String expandIri(ActiveContext activeCtx, String value, Boolean relativeToBase,
            Boolean relativeToVocab, Map<String, Object> localCtx, Map<String, Boolean> defined)
            throws JSONLDProcessingError {
        // already expanded
        if (value == null || isKeyword(value)) {
            return value;
        }

        // define term dependency if not defined
        if (localCtx != null && localCtx.containsKey(value)
                && !Boolean.TRUE.equals(defined.get(value))) {
            createTermDefinition(activeCtx, localCtx, value, defined);
        }

        if (relativeToVocab) {
            final Map<String, Object> mapping = (Map<String, Object>) activeCtx.mappings.get(value);

            // value is explicitly ignored with a null mapping
            if (mapping == null && activeCtx.mappings.containsKey(value)) {
                return null;
            }

            if (mapping != null) {
                // value is a term
                return (String) mapping.get("@id");
            }
        }

        // split value into prefix:suffix
        final int colon = value.indexOf(':');
        if (colon != -1) {
            final String prefix = value.substring(0, colon);
            final String suffix = value.substring(colon + 1);

            // do not expand blank nodes (prefix of '_') or already-absolute
            // IRIs (suffix of '//')
            if ("_".equals(prefix) || suffix.startsWith("//")) {
                return value;
            }

            // prefix dependency not defined, define it
            if (localCtx != null && localCtx.containsKey(prefix)) {
                createTermDefinition(activeCtx, localCtx, prefix, defined);
            }

            // use mapping if prefix is defined
            if (activeCtx.mappings.containsKey(prefix)) {
                final String id = ((Map<String, String>) activeCtx.mappings.get(prefix)).get("@id");
                return id + suffix;
            }

            // already absolute IRI
            return value;
        }

        // prepend vocab
        if (relativeToVocab && activeCtx.containsKey("@vocab")) {
            return activeCtx.get("@vocab") + value;
        }

        // prepend base
        String rval = value;
        if (relativeToBase) {
            rval = prependBase(activeCtx.get("@base"), rval);
        }

        if (localCtx != null) {
            // value must now be an absolute IRI
            if (!isAbsoluteIri(rval)) {
                throw new JSONLDProcessingError(
                        "Invalid JSON-LD syntax; a @context value does not expand to an absolue IRI.")
                        .setDetail("context", localCtx).setDetail("value", value)
                        .setType(JSONLDProcessingError.Error.SYNTAX_ERROR);
            }
        }

        return rval;
    }

    /**
     * Prepends a base IRI to the given relative IRI.
     * 
     * @param base
     *            the base IRI.
     * @param iri
     *            the relative IRI.
     * 
     * @return the absolute IRI.
     * 
     *         TODO: the URL class isn't as forgiving as the Node.js url parser,
     *         we may need to re-implement the parser here to support the
     *         flexibility required
     */
    private static String prependBase(Object baseobj, String iri) {
        // already an absolute IRI
        if (iri.indexOf(":") != -1) {
            return iri;
        }

        // parse base if it is a string
        URL base;
        if (isString(baseobj)) {
            base = URL.parse((String) baseobj);
        } else {
            // assume base is already a URL
            base = (URL) baseobj;
        }

        final URL rel = URL.parse(iri);

        // start hierarchical part
        String hierPart = base.protocol;
        if (!"".equals(rel.authority)) {
            hierPart += "//" + rel.authority;
        } else if (!"".equals(base.href)) {
            hierPart += "//" + base.authority;
        }

        // per RFC3986 normalize
        String path;

        // IRI represents an absolute path
        if (rel.pathname.indexOf("/") == 0) {
            path = rel.pathname;
        } else {
            path = base.pathname;

            // append relative path to the end of the last directory from base
            if (!"".equals(rel.pathname)) {
                path = path.substring(0, path.lastIndexOf("/") + 1);
                if (path.length() > 0 && !path.endsWith("/")) {
                    path += "/";
                }
                path += rel.pathname;
            }
        }

        // remove slashes anddots in path
        path = URL.removeDotSegments(path, !"".equals(hierPart));

        // add query and hash
        if (!"".equals(rel.query)) {
            path += "?" + rel.query;
        }

        if (!"".equals(rel.hash)) {
            path += rel.hash;
        }

        final String rval = hierPart + path;

        if ("".equals(rval)) {
            return "./";
        }
        return rval;
    }

    /**
     * Expands a language map.
     * 
     * @param languageMap
     *            the language map to expand.
     * 
     * @return the expanded language map.
     * @throws JSONLDProcessingError
     */
    static List<Object> expandLanguageMap(Map<String, Object> languageMap)
            throws JSONLDProcessingError {
        final List<Object> rval = new ArrayList<Object>();
        final List<String> keys = new ArrayList<String>(languageMap.keySet());
        Collections.sort(keys); // lexicographically sort languages
        for (final String key : keys) {
            List<Object> val;
            if (!isArray(languageMap.get(key))) {
                val = new ArrayList<Object>();
                val.add(languageMap.get(key));
            } else {
                val = (List<Object>) languageMap.get(key);
            }
            for (final Object item : val) {
                if (!isString(item)) {
                    throw new JSONLDProcessingError(
                            "Invalid JSON-LD syntax; language map values must be strings.")
                            .setDetail("languageMap", languageMap).setType(
                                    JSONLDProcessingError.Error.SYNTAX_ERROR);
                }
                final Map<String, Object> tmp = new LinkedHashMap<String, Object>();
                tmp.put("@value", item);
                tmp.put("@language", key.toLowerCase());
                rval.add(tmp);
            }
        }

        return rval;
    }

    /**
     * Expands the given value by using the coercion and keyword rules in the
     * given context.
     * 
     * @param ctx
     *            the active context to use.
     * @param property
     *            the property the value is associated with.
     * @param value
     *            the value to expand.
     * @param base
     *            the base IRI to use.
     * 
     * @return the expanded value.
     * @throws JSONLDProcessingError
     */
    static Object expandValue(ActiveContext activeCtx, String activeProperty, Object value)
            throws JSONLDProcessingError {
        // nothing to expand
        if (value == null) {
            return null;
        }

        // special-case expand @id and @type (skips '@id' expansion)
        final String expandedProperty = expandIri(activeCtx, activeProperty, false, true, null,
                null);
        if ("@id".equals(expandedProperty)) {
            return expandIri(activeCtx, (String) value, true, false, null, null);
        } else if ("@type".equals(expandedProperty)) {
            return expandIri(activeCtx, (String) value, true, true, null, null);
        }

        // get type definition from context
        final Object type = activeCtx.getContextValue(activeProperty, "@type");

        // do @id expansion (automatic for @graph)
        if ("@id".equals(type) || ("@graph".equals(expandedProperty) && isString(value))) {
            final Map<String, Object> tmp = new LinkedHashMap<String, Object>();
            tmp.put("@id", expandIri(activeCtx, (String) value, true, false, null, null));
            return tmp;
        }

        // do @id expansion w/vocab
        if ("@vocab".equals(type)) {
            final Map<String, Object> tmp = new LinkedHashMap<String, Object>();
            tmp.put("@id", expandIri(activeCtx, (String) value, true, true, null, null));
            return tmp;
        }

        // do not expand keyword values
        if (isKeyword(expandedProperty)) {
            return value;
        }

        final Map<String, Object> rval = new LinkedHashMap<String, Object>();

        // other type
        if (type != null) {
            rval.put("@type", type);
        }
        // check for language tagging
        else if (isString(value)) {
            final Object language = activeCtx.getContextValue(activeProperty, "@language");
            if (language != null) {
                rval.put("@language", language);
            }
        }
        rval.put("@value", value);

        return rval;
    }

    /**
     * Throws an exception if the given value is not a valid @type value.
     * 
     * @param v
     *            the value to check.
     * @throws JSONLDProcessingError
     */
    static boolean validateTypeValue(Object v) throws JSONLDProcessingError {
        if (v == null) {
            throw new NullPointerException("\"@type\" value cannot be null");
        }

        // must be a string, subject reference, or empty object
        if (v instanceof String
                || (v instanceof Map && (((Map<String, Object>) v).containsKey("@id") || ((Map<String, Object>) v)
                        .size() == 0))) {
            return true;
        }

        // must be an array
        boolean isValid = false;
        if (v instanceof List) {
            isValid = true;
            for (final Object i : (List) v) {
                if (!(i instanceof String || i instanceof Map
                        && ((Map<String, Object>) i).containsKey("@id"))) {
                    isValid = false;
                    break;
                }
            }
        }

        if (!isValid) {
            throw new JSONLDProcessingError(
                    "Invalid JSON-LD syntax; \"@type\" value must a string, a subject reference, an array of strings or subject references, or an empty object.")
                    .setType(JSONLDProcessingError.Error.SYNTAX_ERROR).setDetail("value", v);
        }
        return true;
    }

    /**
     * Compacts an IRI or keyword into a term or prefix if it can be. If the IRI
     * has an associated value it may be passed.
     * 
     * @param activeCtx
     *            the active context to use.
     * @param iri
     *            the IRI to compact.
     * @param value
     *            the value to check or null.
     * @param relativeTo
     *            options for how to compact IRIs: vocab: true to split after
     * @vocab, false not to.
     * @param reverse
     *            true if a reverse property is being compacted, false if not.
     * 
     * @return the compacted term, prefix, keyword alias, or the original IRI.
     */
    static String compactIri(ActiveContext activeCtx, String iri, Object value,
            boolean relativeToVocab, boolean reverse) {
        // can't compact null
        if (iri == null) {
            return iri;
        }

        // term is a keyword, default vocab to true
        if (isKeyword(iri)) {
            relativeToVocab = true;
        }

        // use inverse context to pick a term if iri is relative to vocab
        if (relativeToVocab && activeCtx.getInverse().containsKey(iri)) {
            String defaultLanguage = (String) activeCtx.get("@language");
            if (defaultLanguage == null) {
                defaultLanguage = "@none";
            }

            // prefer @index if available in value
            final List<String> containers = new ArrayList<String>();
            if (isObject(value) && ((Map<String, Object>) value).containsKey("@index")) {
                containers.add("@index");
            }

            // defaults for term selection based on type/language
            String typeOrLanguage = "@language";
            String typeOrLanguageValue = "@null";

            if (reverse) {
                typeOrLanguage = "@type";
                typeOrLanguageValue = "@reverse";
                containers.add("@set");
            }
            // choose the most specific term that works for all elements in
            // @list
            else if (isList(value)) {
                // only select @list containers if @index is NOT in value
                if (!((Map<String, Object>) value).containsKey("@index")) {
                    containers.add("@list");
                }
                final List<Object> list = (List<Object>) ((Map<String, Object>) value).get("@list");
                String commonLanguage = (list.size() == 0) ? defaultLanguage : null;
                String commonType = null;
                for (final Object item : list) {
                    String itemLanguage = "@none";
                    String itemType = "@none";
                    if (isValue(item)) {
                        if (((Map<String, Object>) item).containsKey("@language")) {
                            itemLanguage = (String) ((Map<String, Object>) item).get("@language");
                        } else if (((Map<String, Object>) item).containsKey("@type")) {
                            itemType = (String) ((Map<String, Object>) item).get("@type");
                        }
                        // plain literal
                        else {
                            itemLanguage = "@null";
                        }
                    } else {
                        itemType = "@id";
                    }
                    if (commonLanguage == null) {
                        commonLanguage = itemLanguage;
                    } else if (!itemLanguage.equals(commonLanguage) && isValue(item)) {
                        commonLanguage = "@none";
                    }
                    if (commonType == null) {
                        commonType = itemType;
                    } else if (!itemType.equals(commonType)) {
                        commonType = "@none";
                    }
                    // there are different languages and types in the list, so
                    // choose
                    // the most generic term, no need to keep iterating the list
                    if ("@none".equals(commonLanguage) && "@none".equals(commonType)) {
                        break;
                    }
                }
                commonLanguage = (commonLanguage != null) ? commonLanguage : "@none";
                commonType = (commonType != null) ? commonType : "@none";
                if (!"@none".equals(commonType)) {
                    typeOrLanguage = "@type";
                    typeOrLanguageValue = commonType;
                } else {
                    typeOrLanguageValue = commonLanguage;
                }
            } else {
                if (isValue(value)) {
                    if (((Map<String, Object>) value).containsKey("@language")
                            && !((Map<String, Object>) value).containsKey("@index")) {
                        containers.add("@language");
                        typeOrLanguageValue = (String) ((Map<String, Object>) value)
                                .get("@language");
                    } else if (((Map<String, Object>) value).containsKey("@type")) {
                        typeOrLanguage = "@type";
                        typeOrLanguageValue = (String) ((Map<String, Object>) value).get("@type");
                    }
                } else {
                    typeOrLanguage = "@type";
                    typeOrLanguageValue = "@id";
                }
                containers.add("@set");
            }

            // do term selection
            containers.add("@none");
            final String term = selectTerm(activeCtx, iri, value, containers, typeOrLanguage,
                    typeOrLanguageValue);
            if (term != null) {
                return term;
            }
        }

        // no term match, use @vocab if available
        if (relativeToVocab) {
            if (activeCtx.containsKey("@vocab")) {
                // determine if vocab is a prefix of the iri
                final String vocab = (String) activeCtx.get("@vocab");
                if (iri.indexOf(vocab) == 0 && !iri.equals(vocab)) {
                    // use suffix as relative iri if it is not a term in the
                    // active context
                    final String suffix = iri.substring(vocab.length());
                    if (!activeCtx.mappings.containsKey(suffix)) {
                        return suffix;
                    }
                }
            }
        }

        // no term of @vocab match, check for possible CURIEs
        String choice = null;
        for (final String term : activeCtx.mappings.keySet()) {
            // skip terms with colons, they can't be prefixes
            if (term.indexOf(":") != -1) {
                continue;
            }
            // skip entries with @ids that are not partial matches
            final Map<String, Object> definition = (Map<String, Object>) activeCtx.mappings
                    .get(term);
            if (definition == null || iri.equals(definition.get("@id"))
                    || iri.indexOf((String) definition.get("@id")) != 0) {
                continue;
            }

            // a CURIE is usable if:
            // 1. it has no mapping, OR
            // 2. value is null, which means we're not compacting an @value, AND
            // the mapping matches the IRI
            final String curie = term + ":"
                    + iri.substring(((String) definition.get("@id")).length());
            final Boolean isUsableCurie = (!activeCtx.mappings.containsKey(curie) || (value == null
                    && activeCtx.mappings.get(curie) != null && iri
                    .equals(((Map<String, Object>) activeCtx.mappings.get(curie)).get("@id"))));

            // select curie if it is shorter or the same length but
            // lexicographically
            // less than the current choice
            if (isUsableCurie && (choice == null || compareShortestLeast(curie, choice) < 0)) {
                choice = curie;
            }
        }

        // return chosen curie
        if (choice != null) {
            return choice;
        }

        // compact IRI relative to base
        if (!relativeToVocab) {
            return removeBase(activeCtx.get("@base"), iri);
        }

        // return IRI as is
        return iri;
    }

    static String compactIri(ActiveContext ctx, String iri) {
        return compactIri(ctx, iri, null, false, false);
    }

    /**
     * Removes a base IRI from the given absolute IRI.
     * 
     * @param base
     *            the base IRI.
     * @param iri
     *            the absolute IRI.
     * 
     * @return the relative IRI if relative to base, otherwise the absolute IRI.
     */
    private static String removeBase(Object baseobj, String iri) {
        URL base;
        if (isString(baseobj)) {
            base = URL.parse((String) baseobj);
        } else {
            base = (URL) baseobj;
        }

        // establish base root
        String root = "";
        if (!"".equals(base.href)) {
            root += (base.protocol) + "//" + base.authority;
        }
        // support network-path reference with empty base
        else if (iri.indexOf("//") != 0) {
            root += "//";
        }

        // IRI not relative to base
        if (iri.indexOf(root) != 0) {
            return iri;
        }

        // remove root from IRI and parse remainder
        final URL rel = URL.parse(iri.substring(root.length()));

        // remove path segments that match
        final List<String> baseSegments = _split(base.normalizedPath, "/");
        final List<String> iriSegments = _split(rel.normalizedPath, "/");

        while (baseSegments.size() > 0 && iriSegments.size() > 0) {
            if (!baseSegments.get(0).equals(iriSegments.get(0))) {
                break;
            }
            if (baseSegments.size() > 0) {
                baseSegments.remove(0);
            }
            if (iriSegments.size() > 0) {
                iriSegments.remove(0);
            }
        }

        // use '../' for each non-matching base segment
        String rval = "";
        if (baseSegments.size() > 0) {
            // don't count the last segment if it isn't a path (doesn't end in
            // '/')
            // don't count empty first segment, it means base began with '/'
            if (!base.normalizedPath.endsWith("/") || "".equals(baseSegments.get(0))) {
                baseSegments.remove(baseSegments.size() - 1);
            }
            for (int i = 0; i < baseSegments.size(); ++i) {
                rval += "../";
            }
        }

        // prepend remaining segments
        rval += _join(iriSegments, "/");

        // add query and hash
        if (!"".equals(rel.query)) {
            rval += "?" + rel.query;
        }
        if (!"".equals(rel.hash)) {
            rval += rel.hash;
        }

        if ("".equals(rval)) {
            rval = "./";
        }

        return rval;
    }

    /**
     * Removes the @preserve keywords as the last step of the framing algorithm.
     * 
     * @param ctx
     *            the active context used to compact the input.
     * @param input
     *            the framed, compacted output.
     * @param options
     *            the compaction options used.
     * 
     * @return the resulting output.
     */
    static Object removePreserve(ActiveContext ctx, Object input, Options opts) {
        // recurse through arrays
        if (isArray(input)) {
            final List<Object> output = new ArrayList<Object>();
            for (final Object i : (List<Object>) input) {
                final Object result = removePreserve(ctx, i, opts);
                // drop nulls from arrays
                if (result != null) {
                    output.add(result);
                }
            }
            input = output;
        } else if (isObject(input)) {
            // remove @preserve
            if (((Map<String, Object>) input).containsKey("@preserve")) {
                if ("@null".equals(((Map<String, Object>) input).get("@preserve"))) {
                    return null;
                }
                return ((Map<String, Object>) input).get("@preserve");
            }

            // skip @values
            if (isValue(input)) {
                return input;
            }

            // recurse through @lists
            if (isList(input)) {
                ((Map<String, Object>) input).put("@list",
                        removePreserve(ctx, ((Map<String, Object>) input).get("@list"), opts));
                return input;
            }

            // recurse through properties
            for (final String prop : ((Map<String, Object>) input).keySet()) {
                Object result = removePreserve(ctx, ((Map<String, Object>) input).get(prop), opts);
                final String container = (String) ctx.getContextValue(prop, "@container");
                if (opts.compactArrays && isArray(result) && ((List<Object>) result).size() == 1
                        && container == null) {
                    result = ((List<Object>) result).get(0);
                }
                ((Map<String, Object>) input).put(prop, result);
            }
        }
        return input;
    }

    /**
     * replicate javascript .join because i'm too lazy to keep doing it manually
     * 
     * @param iriSegments
     * @param string
     * @return
     */
    private static String _join(List<String> list, String joiner) {
        String rval = "";
        if (list.size() > 0) {
            rval += list.get(0);
        }
        for (int i = 1; i < list.size(); i++) {
            rval += joiner + list.get(i);
        }
        return rval;
    }

    /**
     * replicates the functionality of javascript .split, which has different
     * results to java's String.split if there is a trailing /
     * 
     * @param string
     * @param delim
     * @return
     */
    private static List<String> _split(String string, String delim) {
        final List<String> rval = new ArrayList<String>(Arrays.asList(string.split(delim)));
        if (string.endsWith("/")) {
            // javascript .split includes a blank entry if the string ends with
            // the delimiter, java .split does not so we need to add it manually
            rval.add("");
        }
        return rval;
    }

    /**
     * Compares two strings first based on length and then lexicographically.
     * 
     * @param a
     *            the first string.
     * @param b
     *            the second string.
     * 
     * @return -1 if a < b, 1 if a > b, 0 if a == b.
     */
    static int compareShortestLeast(String a, String b) {
        if (a.length() < b.length()) {
            return -1;
        } else if (b.length() < a.length()) {
            return 1;
        }
        return Integer.signum(a.compareTo(b));
    }

    /**
     * Picks the preferred compaction term from the given inverse context entry.
     * 
     * @param activeCtx
     *            the active context.
     * @param iri
     *            the IRI to pick the term for.
     * @param value
     *            the value to pick the term for.
     * @param containers
     *            the preferred containers.
     * @param typeOrLanguage
     *            either '@type' or '@language'.
     * @param typeOrLanguageValue
     *            the preferred value for '@type' or '@language'.
     * 
     * @return the preferred term.
     */
    private static String selectTerm(ActiveContext activeCtx, String iri, Object value,
            List<String> containers, String typeOrLanguage, String typeOrLanguageValue) {
        if (typeOrLanguageValue == null) {
            typeOrLanguageValue = "@null";
        }

        // preferences for the value of @type or @language
        final List<String> prefs = new ArrayList<String>();

        // determine prefs for @id based on whether or not value compacts to a
        // term
        if ((("@id").equals(typeOrLanguageValue) || "@reverse".equals(typeOrLanguageValue))
                && isSubjectReference(value)) {
            // prefer @reverse first
            if ("@reverse".equals(typeOrLanguageValue)) {
                prefs.add("@reverse");
            }
            // try to compact value to a term
            final String term = compactIri(activeCtx,
                    (String) ((Map<String, Object>) value).get("@id"), null, true, false);
            if (activeCtx.mappings.containsKey(term)
                    && activeCtx.mappings.get(term) != null
                    && ((Map<String, Object>) value).get("@id").equals(
                            ((Map<String, Object>) activeCtx.mappings.get(term)).get("@id"))) {
                // prefer @vocab
                prefs.add("@vocab");
                prefs.add("@id");
            } else {
                // prefer @id
                prefs.add("@id");
                prefs.add("@vocab");
            }
        } else {
            prefs.add(typeOrLanguageValue);
        }
        prefs.add("@none");

        final Map<String, Object> containerMap = (Map<String, Object>) activeCtx.inverse.get(iri);
        for (final String container : containers) {
            // if container not available in the map, continue
            if (!containerMap.containsKey(container)) {
                continue;
            }

            final Map<String, Object> typeOrLanguageValueMap = (Map<String, Object>) Obj.get(
                    containerMap, container, typeOrLanguage);
            for (final String pref : prefs) {
                // if type/language option not available in the map, continue
                if (!typeOrLanguageValueMap.containsKey(pref)) {
                    continue;
                }

                // select term
                return (String) typeOrLanguageValueMap.get(pref);
            }
        }

        return null;
    }

    /**
     * Performs value compaction on an object with '@value' or '@id' as the only
     * property.
     * 
     * @param activeCtx
     *            the active context.
     * @param activeProperty
     *            the active property that points to the value.
     * @param value
     *            the value to compact.
     * 
     * @return the compaction result.
     * @throws JSONLDProcessingError
     */
    static Object compactValue(ActiveContext activeCtx, String activeProperty, Object value)
            throws JSONLDProcessingError {
        // value is a @value
        if (isValue(value)) {
            // get context rules
            final String type = (String) activeCtx.getContextValue(activeProperty, "@type");
            final String language = (String) activeCtx.getContextValue(activeProperty, "@language");
            final String container = (String) activeCtx.getContextValue(activeProperty,
                    "@container");

            // whether or not the value has an @index that must be preserved
            final Boolean preserveIndex = (((Map<String, Object>) value).containsKey("@index") && !"@index"
                    .equals(container));

            // if there's no @index to preserve ...
            if (!preserveIndex) {
                // matching @type or @language specified in context, compact
                // value
                if ((((Map<String, Object>) value).containsKey("@type") && JSONUtils.equals(
                        ((Map<String, Object>) value).get("@type"), type))
                        || (((Map<String, Object>) value).containsKey("@language") && JSONUtils
                                .equals(((Map<String, Object>) value).get("@language"), language))) {
                    // NOTE: have to check containsKey here as javascript
                    // version relies on undefined !== null
                    return ((Map<String, Object>) value).get("@value");
                }
            }

            // return just the value of @value if all are true:
            // 1. @value is the only key or @index isn't being preserved
            // 2. there is no default language or @value is not a string or
            // the key has a mapping with a null @language
            final int keyCount = ((Map<String, Object>) value).size();
            final Boolean isValueOnlyKey = (keyCount == 1 || (keyCount == 2
                    && ((Map<String, Object>) value).containsKey("@index") && !preserveIndex));
            final Boolean hasDefaultLanguage = activeCtx.containsKey("@language");
            final Boolean isValueString = isString(((Map<String, Object>) value).get("@value"));
            final Boolean hasNullMapping = activeCtx.mappings.containsKey(activeProperty)
                    && ((Map<String, Object>) activeCtx.mappings.get(activeProperty))
                            .containsKey("@language")
                    && Obj.get(activeCtx.mappings, activeProperty, "@language") == null;
            if (isValueOnlyKey && (!hasDefaultLanguage || !isValueString || hasNullMapping)) {
                return ((Map<String, Object>) value).get("@value");
            }

            final Map<String, Object> rval = new LinkedHashMap<String, Object>();

            // preserve @index
            if (preserveIndex) {
                rval.put(compactIri(activeCtx, "@index"),
                        ((Map<String, Object>) value).get("@index"));
            }

            // compact @type IRI
            if (((Map<String, Object>) value).containsKey("@type")) {
                rval.put(
                        compactIri(activeCtx, "@type"),
                        compactIri(activeCtx, (String) ((Map<String, Object>) value).get("@type"),
                                null, true, false));
            }
            // alias @language
            else if (((Map<String, Object>) value).containsKey("@language")) {
                rval.put(compactIri(activeCtx, "@language"),
                        ((Map<String, Object>) value).get("@language"));
            }

            // alias @value
            rval.put(compactIri(activeCtx, "@value"), ((Map<String, Object>) value).get("@value"));

            return rval;
        }

        // value is a subject reference
        final String expandedProperty = expandIri(activeCtx, activeProperty, false, true, null,
                null);
        final String type = (String) activeCtx.getContextValue(activeProperty, "@type");
        final Object compacted = compactIri(activeCtx,
                (String) ((Map<String, Object>) value).get("@id"), null, "@vocab".equals(type),
                false);

        if ("@id".equals(type) || "@vocab".equals(type) || "@graph".equals(type)) {
            return compacted;
        }

        final Map<String, Object> rval = new LinkedHashMap<String, Object>();
        rval.put(compactIri(activeCtx, "@id"), compacted);
        return rval;
    }

    /**
     * Recursively flattens the subjects in the given JSON-LD expanded input
     * into a node map.
     * 
     * @param input
     *            the JSON-LD expanded input.
     * @param graphs
     *            a map of graph name to subject map.
     * @param graph
     *            the name of the current graph.
     * @param namer
     *            the blank node namer.
     * @param name
     *            the name assigned to the current input if it is a bnode.
     * @param list
     *            the list to append to, null for none.
     * @throws JSONLDProcessingError
     */
    static void createNodeMap(Object input, Map<String, Object> graphs, String graph,
            UniqueNamer namer, String name, List<Object> list) throws JSONLDProcessingError {
        // recurce through array
        if (isArray(input)) {
            for (final Object i : (List<Object>) input) {
                createNodeMap(i, graphs, graph, namer, null, list);
            }
            return;
        }

        // add non-object to list
        if (!isObject(input)) {
            if (list != null) {
                list.add(input);
            }
            return;
        }

        // add value to list
        if (isValue(input)) {
            if (((Map<String, Object>) input).containsKey("@type")) {
                String type = (String) ((Map<String, Object>) input).get("@type");
                // rename @type blank node
                if (type.indexOf("_:") == 0) {
                    type = namer.getName(type);
                    ((Map<String, Object>) input).put("@type", type);
                }
                if (!((Map<String, Object>) graphs.get(graph)).containsKey(type)) {
                    final Map<String, Object> tmp = new LinkedHashMap<String, Object>();
                    tmp.put("@id", type);
                    ((Map<String, Object>) graphs.get(graph)).put(type, tmp);
                }
            }
            if (list != null) {
                list.add(input);
            }
            return;
        }

        // NOTE: At this point, input must be a subject.

        // get name for subject
        if (name == null) {
            name = isBlankNode(input) ? namer.getName((String) ((Map<String, Object>) input)
                    .get("@id")) : (String) ((Map<String, Object>) input).get("@id");
        }

        // add subject reference to list
        if (list != null) {
            final Map<String, Object> tmp = new LinkedHashMap<String, Object>();
            tmp.put("@id", name);
            list.add(tmp);
        }

        // create new subject or merge into existing one
        final Map<String, Object> subjects = (Map<String, Object>) graphs.get(graph);
        Map<String, Object> subject;
        if (subjects.containsKey(name)) {
            subject = (Map<String, Object>) subjects.get(name);
        } else {
            subject = new LinkedHashMap<String, Object>();
            subjects.put(name, subject);
        }
        subject.put("@id", name);
        final List<String> properties = new ArrayList<String>(
                ((Map<String, Object>) input).keySet());
        Collections.sort(properties);
        for (String property : properties) {
            // skip @id
            if ("@id".equals(property)) {
                continue;
            }

            // handle reverse properties
            if ("@reverse".equals(property)) {
                final Map<String, Object> referencedNode = new LinkedHashMap<String, Object>();
                referencedNode.put("@id", name);
                final Map<String, Object> reverseMap = (Map<String, Object>) ((Map<String, Object>) input)
                        .get("@reverse");
                for (final String reverseProperty : reverseMap.keySet()) {
                    for (final Object item : (List<Object>) reverseMap.get(reverseProperty)) {
                        addValue((Map<String, Object>) item, reverseProperty, referencedNode, true,
                                false);
                        createNodeMap(item, graphs, graph, namer);
                    }
                }
                continue;
            }

            // recurse into graph
            if ("@graph".equals(property)) {
                // add graph subjects map entry
                if (!graphs.containsKey(name)) {
                    graphs.put(name, new LinkedHashMap<String, Object>());
                }
                final String g = "@merged".equals(graph) ? graph : name;
                createNodeMap(((Map<String, Object>) input).get(property), graphs, g, namer);
                continue;
            }

            // copy non-@type keywords
            if (!"@type".equals(property) && isKeyword(property)) {
                if ("@index".equals(property) && subjects.containsKey("@index")) {
                    throw new JSONLDProcessingError(
                            "Invalid JSON-LD syntax; conflicting @index property detected.")
                            .setType(JSONLDProcessingError.Error.SYNTAX_ERROR).setDetail("subject",
                                    subject);
                }
                subject.put(property, ((Map<String, Object>) input).get(property));
                continue;
            }

            // iterate over objects
            final List<Object> objects = (List<Object>) ((Map<String, Object>) input).get(property);

            // if property is a bnode, assign it a new id
            if (property.indexOf("_:") == 0) {
                property = namer.getName(property);
            }

            // ensure property is added for empty arrays
            if (objects.size() == 0) {
                addValue(subject, property, new ArrayList<Object>(), true);
                continue;
            }

            for (Object o : objects) {
                if ("@type".equals(property)) {
                    // rename @type blank nodes
                    o = (((String) o).indexOf("_:") == 0) ? namer.getName((String) o) : o;
                    if (!((Map<String, Object>) graphs.get(graph)).containsKey(o)) {
                        final Map<String, Object> tmp = new LinkedHashMap<String, Object>();
                        tmp.put("@id", o);
                        ((Map<String, Object>) graphs.get(graph)).put((String) o, tmp);
                    }
                }

                // handle embedded subject or subject reference
                if (isSubject(o) || isSubjectReference(o)) {
                    // rename blank node @id
                    final String id = isBlankNode(o) ? namer
                            .getName((String) ((Map<String, Object>) o).get("@id"))
                            : (String) ((Map<String, Object>) o).get("@id");

                    // add reference and recurse
                    final Map<String, Object> tmp = new LinkedHashMap<String, Object>();
                    tmp.put("@id", id);
                    addValue(subject, property, tmp, true, false);
                    createNodeMap(o, graphs, graph, namer, id);
                }
                // handle @list
                else if (isList(o)) {
                    final List<Object> _list = new ArrayList<Object>();
                    createNodeMap(((Map<String, Object>) o).get("@list"), graphs, graph, namer,
                            name, _list);
                    o = new LinkedHashMap<String, Object>();
                    ((Map<String, Object>) o).put("@list", _list);
                    addValue(subject, property, o, true, false);
                }
                // handle @value
                else {
                    createNodeMap(o, graphs, graph, namer, name);
                    addValue(subject, property, o, true, false);
                }
            }
        }
    }

    static void createNodeMap(Object input, Map<String, Object> graphs, String graph,
            UniqueNamer namer, String name) throws JSONLDProcessingError {
        createNodeMap(input, graphs, graph, namer, name, null);
    }

    static void createNodeMap(Object input, Map<String, Object> graphs, String graph,
            UniqueNamer namer) throws JSONLDProcessingError {
        createNodeMap(input, graphs, graph, namer, null, null);
    }

    /**
     * Determines if the given value is a property of the given subject.
     * 
     * @param subject
     *            the subject to check.
     * @param property
     *            the property to check.
     * @param value
     *            the value to check.
     * 
     * @return true if the value exists, false if not.
     */
    static boolean hasValue(Map<String, Object> subject, String property, Object value) {
        boolean rval = false;
        if (hasProperty(subject, property)) {
            Object val = subject.get(property);
            final boolean isList = isList(val);
            if (isList || val instanceof List) {
                if (isList) {
                    val = ((Map<String, Object>) val).get("@list");
                }
                for (final Object i : (List) val) {
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
            final Object value = subject.get(property);
            rval = (!(value instanceof List) || ((List) value).size() > 0);
        }
        return rval;
    }

    /**
     * Compares two JSON-LD values for equality. Two JSON-LD values will be
     * considered equal if:
     * 
     * 1. They are both primitives of the same type and value. 2. They are both @values
     * with the same @value, @type, and @language, OR 3. They both have @ids
     * they are the same.
     * 
     * @param v1
     *            the first value.
     * @param v2
     *            the second value.
     * 
     * @return true if v1 and v2 are considered equal, false if not.
     */
    static boolean compareValues(Object v1, Object v2) {
        if (v1.equals(v2)) {
            return true;
        }

        if (isValue(v1)
                && isValue(v2)
                && JSONUtils.equals(((Map<String, Object>) v1).get("@value"),
                        ((Map<String, Object>) v2).get("@value"))
                && JSONUtils.equals(((Map<String, Object>) v1).get("@type"),
                        ((Map<String, Object>) v2).get("@type"))
                && JSONUtils.equals(((Map<String, Object>) v1).get("@language"),
                        ((Map<String, Object>) v2).get("@language"))
                && JSONUtils.equals(((Map<String, Object>) v1).get("@index"),
                        ((Map<String, Object>) v2).get("@index"))) {
            return true;
        }

        if ((v1 instanceof Map && ((Map<String, Object>) v1).containsKey("@id"))
                && (v2 instanceof Map && ((Map<String, Object>) v2).containsKey("@id"))
                && ((Map<String, Object>) v1).get("@id").equals(
                        ((Map<String, Object>) v2).get("@id"))) {
            return true;
        }

        return false;
    }

    /**
     * Removes a value from a subject.
     * 
     * @param subject
     *            the subject.
     * @param property
     *            the property that relates the value to the subject.
     * @param value
     *            the value to remove.
     * @param [options] the options to use: [propertyIsArray] true if the
     *        property is always an array, false if not (default: false).
     */
    static void removeValue(Map<String, Object> subject, String property, Map<String, Object> value) {
        removeValue(subject, property, value, false);
    }

    static void removeValue(Map<String, Object> subject, String property,
            Map<String, Object> value, boolean propertyIsArray) {
        // filter out value
        final List<Object> values = new ArrayList<Object>();
        if (subject.get(property) instanceof List) {
            for (final Object e : ((List) subject.get(property))) {
                if (!(value.equals(e))) {
                    values.add(value);
                }
            }
        } else {
            if (!value.equals(subject.get(property))) {
                values.add(subject.get(property));
            }
        }

        if (values.size() == 0) {
            subject.remove(property);
        } else if (values.size() == 1 && !propertyIsArray) {
            subject.put(property, values.get(0));
        } else {
            subject.put(property, values);
        }
    }

    /**
     * Returns true if the given value is a blank node.
     * 
     * @param v
     *            the value to check.
     * 
     * @return true if the value is a blank node, false if not.
     */
    static boolean isBlankNode(Object v) {
        // Note: A value is a blank node if all of these hold true:
        // 1. It is an Object.
        // 2. If it has an @id key its value begins with '_:'.
        // 3. It has no keys OR is not a @value, @set, or @list.
        if (v instanceof Map) {
            if (((Map) v).containsKey("@id")) {
                return ((String) ((Map) v).get("@id")).startsWith("_:");
            } else {
                return ((Map) v).size() == 0
                        || !(((Map) v).containsKey("@value") || ((Map) v).containsKey("@set") || ((Map) v)
                                .containsKey("@list"));
            }
        }
        return false;
    }

    /**
     * Returns true if the given value is a subject with properties.
     * 
     * @param v
     *            the value to check.
     * 
     * @return true if the value is a subject with properties, false if not.
     */
    static boolean isSubject(Object v) {
        // Note: A value is a subject if all of these hold true:
        // 1. It is an Object.
        // 2. It is not a @value, @set, or @list.
        // 3. It has more than 1 key OR any existing key is not @id.
        if (v instanceof Map
                && !(((Map) v).containsKey("@value") || ((Map) v).containsKey("@set") || ((Map) v)
                        .containsKey("@list"))) {
            return ((Map<String, Object>) v).size() > 1 || !((Map) v).containsKey("@id");
        }
        return false;
    }

    /**
     * Returns true if the given value is a subject reference.
     * 
     * @param v
     *            the value to check.
     * 
     * @return true if the value is a subject reference, false if not.
     */
    static boolean isSubjectReference(Object v) {
        // Note: A value is a subject reference if all of these hold true:
        // 1. It is an Object.
        // 2. It has a single key: @id.
        return (v instanceof Map && ((Map<String, Object>) v).size() == 1 && ((Map<String, Object>) v)
                .containsKey("@id"));
    }

    /**
     * Resolves external @context URLs using the given URL resolver. Each
     * instance of @context in the input that refers to a URL will be replaced
     * with the JSON @context found at that URL.
     * 
     * @param input
     *            the JSON-LD input with possible contexts.
     * @param resolver
     *            (url, callback(err, jsonCtx)) the URL resolver to use.
     * @param callback
     *            (err, input) called once the operation completes.
     * @throws JSONLDProcessingError
     */
    static void resolveContextUrls(Object input) throws JSONLDProcessingError {
        resolve(input, new LinkedHashMap<String, Object>());
    }

    private static void resolve(Object input, Map<String, Object> cycles)
            throws JSONLDProcessingError {
        final Pattern regex = Pattern
                .compile("(http|https)://(\\w+:{0,1}\\w*@)?(\\S+)(:[0-9]+)?(/|/([\\w#!:.?+=&%@!\\-/]))?");

        if (cycles.size() > MAX_CONTEXT_URLS) {
            throw new JSONLDProcessingError("Maximum number of @context URLs exceeded.").setType(
                    JSONLDProcessingError.Error.CONTEXT_URL_ERROR).setDetail("max",
                    MAX_CONTEXT_URLS);
        }

        // for tracking the URLs to resolve
        final Map<String, Object> urls = new LinkedHashMap<String, Object>();

        // find all URLs in the given input
        if (!findContextUrls(input, urls, false)) {
            // finished
            findContextUrls(input, urls, true);
        }

        // queue all unresolved URLs
        final List<String> queue = new ArrayList<String>();
        for (final String url : urls.keySet()) {
            if (Boolean.FALSE.equals(urls.get(url))) {
                // validate URL
                if (!regex.matcher(url).matches()) {
                    throw new JSONLDProcessingError("Malformed URL.").setType(
                            JSONLDProcessingError.Error.INVALID_URL).setDetail("url", url);
                }
                queue.add(url);
            }
        }

        // resolve URLs in queue
        int count = queue.size();
        for (final String url : queue) {
            // check for context URL cycle
            if (cycles.containsKey(url)) {
                throw new JSONLDProcessingError("Cyclical @context URLs detected.").setType(
                        JSONLDProcessingError.Error.CONTEXT_URL_ERROR).setDetail("url", url);
            }
            final Map<String, Object> _cycles = (Map<String, Object>) clone(cycles);
            _cycles.put(url, Boolean.TRUE);

            try {
                Map<String, Object> ctx = (Map<String, Object>) JSONUtils.fromURL(new java.net.URL(
                        url));
                if (!ctx.containsKey("@context")) {
                    ctx = new LinkedHashMap<String, Object>();
                    ctx.put("@context", new LinkedHashMap<String, Object>());
                }
                resolve(ctx, _cycles);
                urls.put(url, ctx.get("@context"));
                count -= 1;
                if (count == 0) {
                    findContextUrls(input, urls, true);
                }
            } catch (final JsonParseException e) {
                throw new JSONLDProcessingError("URL does not resolve to a valid JSON-LD object.")
                        .setType(JSONLDProcessingError.Error.INVALID_URL).setDetail("url", url);
            } catch (final MalformedURLException e) {
                throw new JSONLDProcessingError("Malformed URL.").setType(
                        JSONLDProcessingError.Error.INVALID_URL).setDetail("url", url);
            } catch (final IOException e) {
                throw new JSONLDProcessingError("Unable to open URL.").setType(
                        JSONLDProcessingError.Error.INVALID_URL).setDetail("url", url);
            }
        }

    }

    /**
     * Finds all @context URLs in the given JSON-LD input.
     * 
     * @param input
     *            the JSON-LD input.
     * @param urls
     *            a map of URLs (url => false/@contexts).
     * @param replace
     *            true to replace the URLs in the given input with the
     * @contexts from the urls map, false not to.
     * 
     * @return true if new URLs to resolve were found, false if not.
     */
    private static boolean findContextUrls(Object input, Map<String, Object> urls, Boolean replace) {
        final int count = urls.size();
        if (input instanceof List) {
            for (final Object i : (List) input) {
                findContextUrls(i, urls, replace);
            }
            return count < urls.size();
        } else if (input instanceof Map) {
            for (final String key : ((Map<String, Object>) input).keySet()) {
                if (!"@context".equals(key)) {
                    findContextUrls(((Map) input).get(key), urls, replace);
                    continue;
                }

                // get @context
                final Object ctx = ((Map) input).get(key);

                // array @context
                if (ctx instanceof List) {
                    int length = ((List) ctx).size();
                    for (int i = 0; i < length; i++) {
                        Object _ctx = ((List) ctx).get(i);
                        if (_ctx instanceof String) {
                            // replace w/@context if requested
                            if (replace) {
                                _ctx = urls.get(_ctx);
                                if (_ctx instanceof List) {
                                    // add flattened context
                                    ((List) ctx).remove(i);
                                    ((List) ctx).addAll((Collection) _ctx);
                                    i += ((List) _ctx).size();
                                    length += ((List) _ctx).size();
                                } else {
                                    ((List) ctx).set(i, _ctx);
                                }
                            }
                            // @context URL found
                            else if (!urls.containsKey(_ctx)) {
                                urls.put((String) _ctx, Boolean.FALSE);
                            }
                        }
                    }
                }
                // string @context
                else if (ctx instanceof String) {
                    // replace w/@context if requested
                    if (replace) {
                        ((Map) input).put(key, urls.get(ctx));
                    }
                    // @context URL found
                    else if (!urls.containsKey(ctx)) {
                        urls.put((String) ctx, Boolean.FALSE);
                    }
                }
            }
            return (count < urls.size());
        }
        return false;
    }

    static Object clone(Object value) {// throws
        // CloneNotSupportedException {
        Object rval = null;
        if (value instanceof Cloneable) {
            try {
                rval = value.getClass().getMethod("clone").invoke(value);
            } catch (final Exception e) {
                rval = e;
            }
        }
        if (rval == null || rval instanceof Exception) {
            // the object wasn't cloneable, or an error occured
            if (value == null || value instanceof String || value instanceof Number
                    || value instanceof Boolean) {
                // strings numbers and booleans are immutable
                rval = value;
            } else {
                // TODO: making this throw runtime exception so it doesn't have
                // to be caught
                // because simply it should never fail in the case of JSON-LD
                // and means that
                // the input JSON-LD is invalid
                throw new RuntimeException(new CloneNotSupportedException(
                        (rval instanceof Exception ? ((Exception) rval).getMessage() : "")));
            }
        }
        return rval;
    }

    /**
     * Returns true if the given value is a JSON-LD Array
     * 
     * @param v
     *            the value to check.
     * @return
     */
    static Boolean isArray(Object v) {
        return (v instanceof List);
    }

    /**
     * Returns true if the given value is a JSON-LD List
     * 
     * @param v
     *            the value to check.
     * @return
     */
    static Boolean isList(Object v) {
        return (v instanceof Map && ((Map<String, Object>) v).containsKey("@list"));
    }

    /**
     * Returns true if the given value is a JSON-LD Object
     * 
     * @param v
     *            the value to check.
     * @return
     */
    static Boolean isObject(Object v) {
        return (v instanceof Map);
    }

    /**
     * Returns true if the given value is a JSON-LD value
     * 
     * @param v
     *            the value to check.
     * @return
     */
    static Boolean isValue(Object v) {
        return (v instanceof Map && ((Map<String, Object>) v).containsKey("@value"));
    }

    /**
     * Returns true if the given value is a JSON-LD string
     * 
     * @param v
     *            the value to check.
     * @return
     */
    static Boolean isString(Object v) {
        // TODO: should this return true for arrays of strings as well?
        return (v instanceof String);
    }
}