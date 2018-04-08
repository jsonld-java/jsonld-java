package com.github.jsonldjava.core;

import static com.github.jsonldjava.core.JsonLdUtils.compareShortestLeast;
import static com.github.jsonldjava.utils.Obj.newMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.jsonldjava.core.JsonLdError.Error;
import com.github.jsonldjava.utils.JsonLdUrl;
import com.github.jsonldjava.utils.Obj;

/**
 * A helper class which still stores all the values in a map but gives member
 * variables easily access certain keys
 *
 * @author tristan
 *
 */
public class Context extends LinkedHashMap<String, Object> {

    private static final long serialVersionUID = 2894534897574805571L;

    private JsonLdOptions options;
    private Map<String, Object> termDefinitions;
    public Map<String, Object> inverse = null;

    public Context() {
        this(new JsonLdOptions());
    }

    public Context(JsonLdOptions opts) {
        super();
        init(opts);
    }

    public Context(Map<String, Object> map, JsonLdOptions opts) {
        super(map);
        init(opts);
    }

    public Context(Map<String, Object> map) {
        super(map);
        init(new JsonLdOptions());
    }

    public Context(Object context, JsonLdOptions opts) {
        // TODO: load remote context
        super(context instanceof Map ? (Map<String, Object>) context : null);
        init(opts);
    }

    private void init(JsonLdOptions options) {
        this.options = options;
        if (options.getBase() != null) {
            this.put(JsonLdConsts.BASE, options.getBase());
        }
        this.termDefinitions = newMap();
    }

    /**
     * Value Compaction Algorithm
     *
     * http://json-ld.org/spec/latest/json-ld-api/#value-compaction
     *
     * @param activeProperty
     *            The Active Property
     * @param value
     *            The value to compact
     * @return The compacted value
     */
    public Object compactValue(String activeProperty, Map<String, Object> value) {
        // 1)
        int numberMembers = value.size();
        // 2)
        if (value.containsKey(JsonLdConsts.INDEX)
                && JsonLdConsts.INDEX.equals(this.getContainer(activeProperty))) {
            numberMembers--;
        }
        // 3)
        if (numberMembers > 2) {
            return value;
        }
        // 4)
        final String typeMapping = getTypeMapping(activeProperty);
        final String languageMapping = getLanguageMapping(activeProperty);
        if (value.containsKey(JsonLdConsts.ID)) {
            // 4.1)
            if (numberMembers == 1 && JsonLdConsts.ID.equals(typeMapping)) {
                return compactIri((String) value.get(JsonLdConsts.ID));
            }
            // 4.2)
            if (numberMembers == 1 && JsonLdConsts.VOCAB.equals(typeMapping)) {
                return compactIri((String) value.get(JsonLdConsts.ID), true);
            }
            // 4.3)
            return value;
        }
        final Object valueValue = value.get(JsonLdConsts.VALUE);
        // 5)
        if (value.containsKey(JsonLdConsts.TYPE)
                && Obj.equals(value.get(JsonLdConsts.TYPE), typeMapping)) {
            return valueValue;
        }
        // 6)
        if (value.containsKey(JsonLdConsts.LANGUAGE)) {
            // TODO: SPEC: doesn't specify to check default language as well
            if (Obj.equals(value.get(JsonLdConsts.LANGUAGE), languageMapping) || Obj
                    .equals(value.get(JsonLdConsts.LANGUAGE), this.get(JsonLdConsts.LANGUAGE))) {
                return valueValue;
            }
        }
        // 7)
        if (numberMembers == 1 && (!(valueValue instanceof String)
                || !this.containsKey(JsonLdConsts.LANGUAGE)
                || (termDefinitions.containsKey(activeProperty)
                        && getTermDefinition(activeProperty).containsKey(JsonLdConsts.LANGUAGE)
                        && languageMapping == null))) {
            return valueValue;
        }
        // 8)
        return value;
    }

    /**
     * Context Processing Algorithm
     *
     * http://json-ld.org/spec/latest/json-ld-api/#context-processing-algorithms
     *
     * @param localContext
     *            The Local Context object.
     * @param remoteContexts
     *            The list of Strings denoting the remote Context URLs.
     * @return The parsed and merged Context.
     * @throws JsonLdError
     *             If there is an error parsing the contexts.
     */
    @SuppressWarnings("unchecked")
    public Context parse(Object localContext, List<String> remoteContexts) throws JsonLdError {
        return parse(localContext, remoteContexts, false);
    }

    /**
     * Helper method used to work around logic errors related to the recursive
     * nature of the JSONLD-API Context Processing Algorithm.
     *
     * @param localContext
     *            The Local Context object.
     * @param remoteContexts
     *            The list of Strings denoting the remote Context URLs.
     * @param parsingARemoteContext
     *            True if localContext represents a remote context that has been
     *            parsed and sent into this method and false otherwise. This
     *            must be set to know whether to propagate the @code{@base} key
     *            from the context to the result.
     * @return The parsed and merged Context.
     * @throws JsonLdError
     *             If there is an error parsing the contexts.
     */
    private Context parse(Object localContext, List<String> remoteContexts,
            boolean parsingARemoteContext) throws JsonLdError {
        if (remoteContexts == null) {
            remoteContexts = new ArrayList<String>();
        }
        // 1. Initialize result to the result of cloning active context.
        Context result = this.clone(); // TODO: clone?
        // 2)
        if (!(localContext instanceof List)) {
            final Object temp = localContext;
            localContext = new ArrayList<Object>();
            ((List<Object>) localContext).add(temp);
        }
        // 3)
        for (final Object context : ((List<Object>) localContext)) {
            // 3.1)
            if (context == null) {
                result = new Context(this.options);
                continue;
            } else if (context instanceof Context) {
                result = ((Context) context).clone();
            }
            // 3.2)
            else if (context instanceof String) {
                String uri = (String) result.get(JsonLdConsts.BASE);
                uri = JsonLdUrl.resolve(uri, (String) context);
                // 3.2.2
                if (remoteContexts.contains(uri)) {
                    throw new JsonLdError(Error.RECURSIVE_CONTEXT_INCLUSION, uri);
                }
                remoteContexts.add(uri);

                // 3.2.3: Dereference context
                final RemoteDocument rd = this.options.getDocumentLoader().loadDocument(uri);
                final Object remoteContext = rd.getDocument();
                if (!(remoteContext instanceof Map) || !((Map<String, Object>) remoteContext)
                        .containsKey(JsonLdConsts.CONTEXT)) {
                    // If the dereferenced document has no top-level JSON object
                    // with an @context member
                    throw new JsonLdError(Error.INVALID_REMOTE_CONTEXT, context);
                }
                final Object tempContext = ((Map<String, Object>) remoteContext)
                        .get(JsonLdConsts.CONTEXT);

                // 3.2.4
                result = result.parse(tempContext, remoteContexts, true);
                // 3.2.5
                continue;
            } else if (!(context instanceof Map)) {
                // 3.3
                throw new JsonLdError(Error.INVALID_LOCAL_CONTEXT, context);
            }

            // 3.4
            if (!parsingARemoteContext
                    && ((Map<String, Object>) context).containsKey(JsonLdConsts.BASE)) {
                // 3.4.1
                final Object value = ((Map<String, Object>) context).get(JsonLdConsts.BASE);
                // 3.4.2
                if (value == null) {
                    result.remove(JsonLdConsts.BASE);
                } else if (value instanceof String) {
                    // 3.4.3
                    if (JsonLdUtils.isAbsoluteIri((String) value)) {
                        result.put(JsonLdConsts.BASE, value);
                    } else {
                        // 3.4.4
                        final String baseUri = (String) result.get(JsonLdConsts.BASE);
                        if (!JsonLdUtils.isAbsoluteIri(baseUri)) {
                            throw new JsonLdError(Error.INVALID_BASE_IRI, baseUri);
                        }
                        result.put(JsonLdConsts.BASE, JsonLdUrl.resolve(baseUri, (String) value));
                    }
                } else {
                    // 3.4.5
                    throw new JsonLdError(JsonLdError.Error.INVALID_BASE_IRI,
                            "@base must be a string");
                }
            }

            // 3.5
            if (((Map<String, Object>) context).containsKey(JsonLdConsts.VOCAB)) {
                final Object value = ((Map<String, Object>) context).get(JsonLdConsts.VOCAB);
                if (value == null) {
                    result.remove(JsonLdConsts.VOCAB);
                } else if (value instanceof String) {
                    if (JsonLdUtils.isAbsoluteIri((String) value)) {
                        result.put(JsonLdConsts.VOCAB, value);
                    } else {
                        throw new JsonLdError(Error.INVALID_VOCAB_MAPPING,
                                "@value must be an absolute IRI");
                    }
                } else {
                    throw new JsonLdError(Error.INVALID_VOCAB_MAPPING,
                            "@vocab must be a string or null");
                }
            }

            // 3.6
            if (((Map<String, Object>) context).containsKey(JsonLdConsts.LANGUAGE)) {
                final Object value = ((Map<String, Object>) context).get(JsonLdConsts.LANGUAGE);
                if (value == null) {
                    result.remove(JsonLdConsts.LANGUAGE);
                } else if (value instanceof String) {
                    result.put(JsonLdConsts.LANGUAGE, ((String) value).toLowerCase());
                } else {
                    throw new JsonLdError(Error.INVALID_DEFAULT_LANGUAGE, value);
                }
            }

            // 3.7
            final Map<String, Boolean> defined = new LinkedHashMap<String, Boolean>();
            for (final String key : ((Map<String, Object>) context).keySet()) {
                if (JsonLdConsts.BASE.equals(key) || JsonLdConsts.VOCAB.equals(key)
                        || JsonLdConsts.LANGUAGE.equals(key)) {
                    continue;
                }
                result.createTermDefinition((Map<String, Object>) context, key, defined);
            }
        }
        return result;
    }

    public Context parse(Object localContext) throws JsonLdError {
        return this.parse(localContext, new ArrayList<String>());
    }

    /**
     * Create Term Definition Algorithm
     *
     * http://json-ld.org/spec/latest/json-ld-api/#create-term-definition
     *
     * @param result
     * @param context
     * @param key
     * @param defined
     * @throws JsonLdError
     */
    private void createTermDefinition(Map<String, Object> context, String term,
            Map<String, Boolean> defined) throws JsonLdError {
        if (defined.containsKey(term)) {
            if (Boolean.TRUE.equals(defined.get(term))) {
                return;
            }
            throw new JsonLdError(Error.CYCLIC_IRI_MAPPING, term);
        }

        defined.put(term, false);

        if (JsonLdUtils.isKeyword(term)) {
            throw new JsonLdError(Error.KEYWORD_REDEFINITION, term);
        }

        this.termDefinitions.remove(term);
        Object value = context.get(term);
        if (value == null || (value instanceof Map
                && ((Map<String, Object>) value).containsKey(JsonLdConsts.ID)
                && ((Map<String, Object>) value).get(JsonLdConsts.ID) == null)) {
            this.termDefinitions.put(term, null);
            defined.put(term, true);
            return;
        }

        if (value instanceof String) {
            value = newMap(JsonLdConsts.ID, value);
        }

        if (!(value instanceof Map)) {
            throw new JsonLdError(Error.INVALID_TERM_DEFINITION, value);
        }

        // casting the value so it doesn't have to be done below everytime
        final Map<String, Object> val = (Map<String, Object>) value;

        // 9) create a new term definition
        final Map<String, Object> definition = newMap();

        // 10)
        if (val.containsKey(JsonLdConsts.TYPE)) {
            if (!(val.get(JsonLdConsts.TYPE) instanceof String)) {
                throw new JsonLdError(Error.INVALID_TYPE_MAPPING, val.get(JsonLdConsts.TYPE));
            }
            String type = (String) val.get(JsonLdConsts.TYPE);
            try {
                type = this.expandIri((String) val.get(JsonLdConsts.TYPE), false, true, context,
                        defined);
            } catch (final JsonLdError error) {
                if (error.getType() != Error.INVALID_IRI_MAPPING) {
                    throw error;
                }
                throw new JsonLdError(Error.INVALID_TYPE_MAPPING, type, error);
            }
            // TODO: fix check for absoluteIri (blank nodes shouldn't count, at
            // least not here!)
            if (JsonLdConsts.ID.equals(type) || JsonLdConsts.VOCAB.equals(type)
                    || (!type.startsWith(JsonLdConsts.BLANK_NODE_PREFIX)
                            && JsonLdUtils.isAbsoluteIri(type))) {
                definition.put(JsonLdConsts.TYPE, type);
            } else {
                throw new JsonLdError(Error.INVALID_TYPE_MAPPING, type);
            }
        }

        // 11)
        if (val.containsKey(JsonLdConsts.REVERSE)) {
            if (val.containsKey(JsonLdConsts.ID)) {
                throw new JsonLdError(Error.INVALID_REVERSE_PROPERTY, val);
            }
            if (!(val.get(JsonLdConsts.REVERSE) instanceof String)) {
                throw new JsonLdError(Error.INVALID_IRI_MAPPING,
                        "Expected String for @reverse value. got "
                                + (val.get(JsonLdConsts.REVERSE) == null ? "null"
                                        : val.get(JsonLdConsts.REVERSE).getClass()));
            }
            final String reverse = this.expandIri((String) val.get(JsonLdConsts.REVERSE), false,
                    true, context, defined);
            if (!JsonLdUtils.isAbsoluteIri(reverse)) {
                throw new JsonLdError(Error.INVALID_IRI_MAPPING,
                        "Non-absolute @reverse IRI: " + reverse);
            }
            definition.put(JsonLdConsts.ID, reverse);
            if (val.containsKey(JsonLdConsts.CONTAINER)) {
                final String container = (String) val.get(JsonLdConsts.CONTAINER);
                if (container == null || JsonLdConsts.SET.equals(container)
                        || JsonLdConsts.INDEX.equals(container)) {
                    definition.put(JsonLdConsts.CONTAINER, container);
                } else {
                    throw new JsonLdError(Error.INVALID_REVERSE_PROPERTY,
                            "reverse properties only support set- and index-containers");
                }
            }
            definition.put(JsonLdConsts.REVERSE, true);
            this.termDefinitions.put(term, definition);
            defined.put(term, true);
            return;
        }

        // 12)
        definition.put(JsonLdConsts.REVERSE, false);

        // 13)
        if (val.get(JsonLdConsts.ID) != null && !term.equals(val.get(JsonLdConsts.ID))) {
            if (!(val.get(JsonLdConsts.ID) instanceof String)) {
                throw new JsonLdError(Error.INVALID_IRI_MAPPING,
                        "expected value of @id to be a string");
            }

            final String res = this.expandIri((String) val.get(JsonLdConsts.ID), false, true,
                    context, defined);
            if (JsonLdUtils.isKeyword(res) || JsonLdUtils.isAbsoluteIri(res)) {
                if (JsonLdConsts.CONTEXT.equals(res)) {
                    throw new JsonLdError(Error.INVALID_KEYWORD_ALIAS, "cannot alias @context");
                }
                definition.put(JsonLdConsts.ID, res);
            } else {
                throw new JsonLdError(Error.INVALID_IRI_MAPPING,
                        "resulting IRI mapping should be a keyword, absolute IRI or blank node");
            }
        }

        // 14)
        else if (term.indexOf(":") >= 0) {
            final int colIndex = term.indexOf(":");
            final String prefix = term.substring(0, colIndex);
            final String suffix = term.substring(colIndex + 1);
            if (context.containsKey(prefix)) {
                this.createTermDefinition(context, prefix, defined);
            }
            if (termDefinitions.containsKey(prefix)) {
                definition.put(JsonLdConsts.ID,
                        ((Map<String, Object>) termDefinitions.get(prefix)).get(JsonLdConsts.ID)
                                + suffix);
            } else {
                definition.put(JsonLdConsts.ID, term);
            }
            // 15)
        } else if (this.containsKey(JsonLdConsts.VOCAB)) {
            definition.put(JsonLdConsts.ID, this.get(JsonLdConsts.VOCAB) + term);
        } else {
            throw new JsonLdError(Error.INVALID_IRI_MAPPING,
                    "relative term definition without vocab mapping");
        }

        // 16)
        if (val.containsKey(JsonLdConsts.CONTAINER)) {
            final String container = (String) val.get(JsonLdConsts.CONTAINER);
            if (!JsonLdConsts.LIST.equals(container) && !JsonLdConsts.SET.equals(container)
                    && !JsonLdConsts.INDEX.equals(container)
                    && !JsonLdConsts.LANGUAGE.equals(container)) {
                throw new JsonLdError(Error.INVALID_CONTAINER_MAPPING,
                        "@container must be either @list, @set, @index, or @language");
            }
            definition.put(JsonLdConsts.CONTAINER, container);
        }

        // 17)
        if (val.containsKey(JsonLdConsts.LANGUAGE) && !val.containsKey(JsonLdConsts.TYPE)) {
            if (val.get(JsonLdConsts.LANGUAGE) == null
                    || val.get(JsonLdConsts.LANGUAGE) instanceof String) {
                final String language = (String) val.get(JsonLdConsts.LANGUAGE);
                definition.put(JsonLdConsts.LANGUAGE,
                        language != null ? language.toLowerCase() : null);
            } else {
                throw new JsonLdError(Error.INVALID_LANGUAGE_MAPPING,
                        "@language must be a string or null");
            }
        }

        // 18)
        this.termDefinitions.put(term, definition);
        defined.put(term, true);
    }

    /**
     * IRI Expansion Algorithm
     *
     * http://json-ld.org/spec/latest/json-ld-api/#iri-expansion
     *
     * @param value
     * @param relative
     * @param vocab
     * @param context
     * @param defined
     * @return
     * @throws JsonLdError
     */
    String expandIri(String value, boolean relative, boolean vocab, Map<String, Object> context,
            Map<String, Boolean> defined) throws JsonLdError {
        // 1)
        if (value == null || JsonLdUtils.isKeyword(value)) {
            return value;
        }
        // 2)
        if (context != null && context.containsKey(value)
                && !Boolean.TRUE.equals(defined.get(value))) {
            this.createTermDefinition(context, value, defined);
        }
        // 3)
        if (vocab && this.termDefinitions.containsKey(value)) {
            final Map<String, Object> td = (Map<String, Object>) this.termDefinitions.get(value);
            if (td != null) {
                return (String) td.get(JsonLdConsts.ID);
            } else {
                return null;
            }
        }
        // 4)
        final int colIndex = value.indexOf(":");
        if (colIndex >= 0) {
            // 4.1)
            final String prefix = value.substring(0, colIndex);
            final String suffix = value.substring(colIndex + 1);
            // 4.2)
            if ("_".equals(prefix) || suffix.startsWith("//")) {
                return value;
            }
            // 4.3)
            if (context != null && context.containsKey(prefix)
                    && (!defined.containsKey(prefix) || defined.get(prefix) == false)) {
                this.createTermDefinition(context, prefix, defined);
            }
            // 4.4)
            if (this.termDefinitions.containsKey(prefix)) {
                return (String) ((Map<String, Object>) this.termDefinitions.get(prefix))
                        .get(JsonLdConsts.ID) + suffix;
            }
            // 4.5)
            return value;
        }
        // 5)
        if (vocab && this.containsKey(JsonLdConsts.VOCAB)) {
            return this.get(JsonLdConsts.VOCAB) + value;
        }
        // 6)
        else if (relative) {
            return JsonLdUrl.resolve((String) this.get(JsonLdConsts.BASE), value);
        } else if (context != null && JsonLdUtils.isRelativeIri(value)) {
            throw new JsonLdError(Error.INVALID_IRI_MAPPING, "not an absolute IRI: " + value);
        }
        // 7)
        return value;
    }

    /**
     * IRI Compaction Algorithm
     *
     * http://json-ld.org/spec/latest/json-ld-api/#iri-compaction
     *
     * Compacts an IRI or keyword into a term or prefix if it can be. If the IRI
     * has an associated value it may be passed.
     *
     * @param iri
     *            the IRI to compact.
     * @param value
     *            the value to check or null.
     * @param relativeTo
     *            options for how to compact IRIs: vocab: true to split
     *            after @vocab, false not to.
     * @param reverse
     *            true if a reverse property is being compacted, false if not.
     *
     * @return the compacted term, prefix, keyword alias, or the original IRI.
     */
    String compactIri(String iri, Object value, boolean relativeToVocab, boolean reverse) {
        // 1)
        if (iri == null) {
            return null;
        }

        // 2)
        if (relativeToVocab && getInverse().containsKey(iri)) {
            // 2.1)
            String defaultLanguage = (String) this.get(JsonLdConsts.LANGUAGE);
            if (defaultLanguage == null) {
                defaultLanguage = JsonLdConsts.NONE;
            }

            // 2.2)
            final List<String> containers = new ArrayList<String>();
            // 2.3)
            String typeLanguage = JsonLdConsts.LANGUAGE;
            String typeLanguageValue = JsonLdConsts.NULL;

            // 2.4)
            if (value instanceof Map
                    && ((Map<String, Object>) value).containsKey(JsonLdConsts.INDEX)) {
                containers.add(JsonLdConsts.INDEX);
            }

            // 2.5)
            if (reverse) {
                typeLanguage = JsonLdConsts.TYPE;
                typeLanguageValue = JsonLdConsts.REVERSE;
                containers.add(JsonLdConsts.SET);
            }
            // 2.6)
            else if (value instanceof Map
                    && ((Map<String, Object>) value).containsKey(JsonLdConsts.LIST)) {
                // 2.6.1)
                if (!((Map<String, Object>) value).containsKey(JsonLdConsts.INDEX)) {
                    containers.add(JsonLdConsts.LIST);
                }
                // 2.6.2)
                final List<Object> list = (List<Object>) ((Map<String, Object>) value)
                        .get(JsonLdConsts.LIST);
                // 2.6.3)
                String commonLanguage = (list.size() == 0) ? defaultLanguage : null;
                String commonType = null;
                // 2.6.4)
                for (final Object item : list) {
                    // 2.6.4.1)
                    String itemLanguage = JsonLdConsts.NONE;
                    String itemType = JsonLdConsts.NONE;
                    // 2.6.4.2)
                    if (JsonLdUtils.isValue(item)) {
                        // 2.6.4.2.1)
                        if (((Map<String, Object>) item).containsKey(JsonLdConsts.LANGUAGE)) {
                            itemLanguage = (String) ((Map<String, Object>) item)
                                    .get(JsonLdConsts.LANGUAGE);
                        }
                        // 2.6.4.2.2)
                        else if (((Map<String, Object>) item).containsKey(JsonLdConsts.TYPE)) {
                            itemType = (String) ((Map<String, Object>) item).get(JsonLdConsts.TYPE);
                        }
                        // 2.6.4.2.3)
                        else {
                            itemLanguage = JsonLdConsts.NULL;
                        }
                    }
                    // 2.6.4.3)
                    else {
                        itemType = JsonLdConsts.ID;
                    }
                    // 2.6.4.4)
                    if (commonLanguage == null) {
                        commonLanguage = itemLanguage;
                    }
                    // 2.6.4.5)
                    else if (!commonLanguage.equals(itemLanguage) && JsonLdUtils.isValue(item)) {
                        commonLanguage = JsonLdConsts.NONE;
                    }
                    // 2.6.4.6)
                    if (commonType == null) {
                        commonType = itemType;
                    }
                    // 2.6.4.7)
                    else if (!commonType.equals(itemType)) {
                        commonType = JsonLdConsts.NONE;
                    }
                    // 2.6.4.8)
                    if (JsonLdConsts.NONE.equals(commonLanguage)
                            && JsonLdConsts.NONE.equals(commonType)) {
                        break;
                    }
                }
                // 2.6.5)
                commonLanguage = (commonLanguage != null) ? commonLanguage : JsonLdConsts.NONE;
                // 2.6.6)
                commonType = (commonType != null) ? commonType : JsonLdConsts.NONE;
                // 2.6.7)
                if (!JsonLdConsts.NONE.equals(commonType)) {
                    typeLanguage = JsonLdConsts.TYPE;
                    typeLanguageValue = commonType;
                }
                // 2.6.8)
                else {
                    typeLanguageValue = commonLanguage;
                }
            }
            // 2.7)
            else {
                // 2.7.1)
                if (value instanceof Map
                        && ((Map<String, Object>) value).containsKey(JsonLdConsts.VALUE)) {
                    // 2.7.1.1)
                    if (((Map<String, Object>) value).containsKey(JsonLdConsts.LANGUAGE)
                            && !((Map<String, Object>) value).containsKey(JsonLdConsts.INDEX)) {
                        containers.add(JsonLdConsts.LANGUAGE);
                        typeLanguageValue = (String) ((Map<String, Object>) value)
                                .get(JsonLdConsts.LANGUAGE);
                    }
                    // 2.7.1.2)
                    else if (((Map<String, Object>) value).containsKey(JsonLdConsts.TYPE)) {
                        typeLanguage = JsonLdConsts.TYPE;
                        typeLanguageValue = (String) ((Map<String, Object>) value)
                                .get(JsonLdConsts.TYPE);
                    }
                }
                // 2.7.2)
                else {
                    typeLanguage = JsonLdConsts.TYPE;
                    typeLanguageValue = JsonLdConsts.ID;
                }
                // 2.7.3)
                containers.add(JsonLdConsts.SET);
            }

            // 2.8)
            containers.add(JsonLdConsts.NONE);
            // 2.9)
            if (typeLanguageValue == null) {
                typeLanguageValue = JsonLdConsts.NULL;
            }
            // 2.10)
            final List<String> preferredValues = new ArrayList<String>();
            // 2.11)
            if (JsonLdConsts.REVERSE.equals(typeLanguageValue)) {
                preferredValues.add(JsonLdConsts.REVERSE);
            }
            // 2.12)
            if ((JsonLdConsts.REVERSE.equals(typeLanguageValue)
                    || JsonLdConsts.ID.equals(typeLanguageValue)) && (value instanceof Map)
                    && ((Map<String, Object>) value).containsKey(JsonLdConsts.ID)) {
                // 2.12.1)
                final String result = this.compactIri(
                        (String) ((Map<String, Object>) value).get(JsonLdConsts.ID), null, true,
                        true);
                if (termDefinitions.containsKey(result)
                        && ((Map<String, Object>) termDefinitions.get(result))
                                .containsKey(JsonLdConsts.ID)
                        && ((Map<String, Object>) value).get(JsonLdConsts.ID)
                                .equals(((Map<String, Object>) termDefinitions.get(result))
                                        .get(JsonLdConsts.ID))) {
                    preferredValues.add(JsonLdConsts.VOCAB);
                    preferredValues.add(JsonLdConsts.ID);
                }
                // 2.12.2)
                else {
                    preferredValues.add(JsonLdConsts.ID);
                    preferredValues.add(JsonLdConsts.VOCAB);
                }
            }
            // 2.13)
            else {
                preferredValues.add(typeLanguageValue);
            }
            preferredValues.add(JsonLdConsts.NONE);

            // 2.14)
            final String term = selectTerm(iri, containers, typeLanguage, preferredValues);
            // 2.15)
            if (term != null) {
                return term;
            }
        }

        // 3)
        if (relativeToVocab && this.containsKey(JsonLdConsts.VOCAB)) {
            // determine if vocab is a prefix of the iri
            final String vocab = (String) this.get(JsonLdConsts.VOCAB);
            // 3.1)
            if (iri.indexOf(vocab) == 0 && !iri.equals(vocab)) {
                // use suffix as relative iri if it is not a term in the
                // active context
                final String suffix = iri.substring(vocab.length());
                if (!termDefinitions.containsKey(suffix)) {
                    return suffix;
                }
            }
        }

        // 4)
        String compactIRI = null;
        // 5)
        for (final String term : termDefinitions.keySet()) {
            final Map<String, Object> termDefinition = (Map<String, Object>) termDefinitions
                    .get(term);
            // 5.1)
            if (term.contains(":")) {
                continue;
            }
            // 5.2)
            if (termDefinition == null || iri.equals(termDefinition.get(JsonLdConsts.ID))
                    || !iri.startsWith((String) termDefinition.get(JsonLdConsts.ID))) {
                continue;
            }

            // 5.3)
            final String candidate = term + ":"
                    + iri.substring(((String) termDefinition.get(JsonLdConsts.ID)).length());
            // 5.4)
            compactIRI = _iriCompactionStep5point4(iri, value, compactIRI, candidate,
                    termDefinitions);
        }

        // 6)
        if (compactIRI != null) {
            return compactIRI;
        }

        // 7)
        if (!relativeToVocab) {
            return JsonLdUrl.removeBase(this.get(JsonLdConsts.BASE), iri);
        }

        // 8)
        return iri;
    }

    /*
     * This method is only visible for testing.
     */
    public static String _iriCompactionStep5point4(String iri, Object value, String compactIRI,
            final String candidate, Map<String, Object> termDefinitions) {

        final boolean condition1 = (compactIRI == null
                || compareShortestLeast(candidate, compactIRI) < 0);

        final boolean condition2 = (!termDefinitions.containsKey(candidate) || (iri
                .equals(((Map<String, Object>) termDefinitions.get(candidate)).get(JsonLdConsts.ID))
                && value == null));

        if (condition1 && condition2) {
            compactIRI = candidate;
        }
        return compactIRI;
    }

    /**
     * Return a map of potential RDF prefixes based on the JSON-LD Term
     * Definitions in this context.
     * <p>
     * No guarantees of the prefixes are given, beyond that it will not contain
     * ":".
     *
     * @param onlyCommonPrefixes
     *            If <code>true</code>, the result will not include "not so
     *            useful" prefixes, such as "term1": "http://example.com/term1",
     *            e.g. all IRIs will end with "/" or "#". If <code>false</code>,
     *            all potential prefixes are returned.
     *
     * @return A map from prefix string to IRI string
     */
    public Map<String, String> getPrefixes(boolean onlyCommonPrefixes) {
        final Map<String, String> prefixes = new LinkedHashMap<String, String>();
        for (final String term : termDefinitions.keySet()) {
            if (term.contains(":")) {
                continue;
            }
            final Map<String, Object> termDefinition = (Map<String, Object>) termDefinitions
                    .get(term);
            if (termDefinition == null) {
                continue;
            }
            final String id = (String) termDefinition.get(JsonLdConsts.ID);
            if (id == null) {
                continue;
            }
            if (term.startsWith("@") || id.startsWith("@")) {
                continue;
            }
            if (!onlyCommonPrefixes || id.endsWith("/") || id.endsWith("#")) {
                prefixes.put(term, id);
            }
        }
        return prefixes;
    }

    String compactIri(String iri, boolean relativeToVocab) {
        return compactIri(iri, null, relativeToVocab, false);
    }

    String compactIri(String iri) {
        return compactIri(iri, null, false, false);
    }

    @Override
    public Context clone() {
        final Context rval = (Context) super.clone();
        // TODO: is this shallow copy enough? probably not, but it passes all
        // the tests!
        rval.termDefinitions = new LinkedHashMap<String, Object>(this.termDefinitions);
        return rval;
    }

    /**
     * Inverse Context Creation
     *
     * http://json-ld.org/spec/latest/json-ld-api/#inverse-context-creation
     *
     * Generates an inverse context for use in the compaction algorithm, if not
     * already generated for the given active context.
     *
     * @return the inverse context.
     */
    public Map<String, Object> getInverse() {

        // lazily create inverse
        if (inverse != null) {
            return inverse;
        }

        // 1)
        inverse = newMap();

        // 2)
        String defaultLanguage = (String) this.get(JsonLdConsts.LANGUAGE);
        if (defaultLanguage == null) {
            defaultLanguage = JsonLdConsts.NONE;
        }

        // create term selections for each mapping in the context, ordererd by
        // shortest and then lexicographically least
        final List<String> terms = new ArrayList<String>(termDefinitions.keySet());
        Collections.sort(terms, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return compareShortestLeast(a, b);
            }
        });

        for (final String term : terms) {
            final Map<String, Object> definition = (Map<String, Object>) termDefinitions.get(term);
            // 3.1)
            if (definition == null) {
                continue;
            }

            // 3.2)
            String container = (String) definition.get(JsonLdConsts.CONTAINER);
            if (container == null) {
                container = JsonLdConsts.NONE;
            }

            // 3.3)
            final String iri = (String) definition.get(JsonLdConsts.ID);

            // 3.4 + 3.5)
            Map<String, Object> containerMap = (Map<String, Object>) inverse.get(iri);
            if (containerMap == null) {
                containerMap = newMap();
                inverse.put(iri, containerMap);
            }

            // 3.6 + 3.7)
            Map<String, Object> typeLanguageMap = (Map<String, Object>) containerMap.get(container);
            if (typeLanguageMap == null) {
                typeLanguageMap = newMap();
                typeLanguageMap.put(JsonLdConsts.LANGUAGE, newMap());
                typeLanguageMap.put(JsonLdConsts.TYPE, newMap());
                containerMap.put(container, typeLanguageMap);
            }

            // 3.8)
            if (Boolean.TRUE.equals(definition.get(JsonLdConsts.REVERSE))) {
                final Map<String, Object> typeMap = (Map<String, Object>) typeLanguageMap
                        .get(JsonLdConsts.TYPE);
                if (!typeMap.containsKey(JsonLdConsts.REVERSE)) {
                    typeMap.put(JsonLdConsts.REVERSE, term);
                }
                // 3.9)
            } else if (definition.containsKey(JsonLdConsts.TYPE)) {
                final Map<String, Object> typeMap = (Map<String, Object>) typeLanguageMap
                        .get(JsonLdConsts.TYPE);
                if (!typeMap.containsKey(definition.get(JsonLdConsts.TYPE))) {
                    typeMap.put((String) definition.get(JsonLdConsts.TYPE), term);
                }
                // 3.10)
            } else if (definition.containsKey(JsonLdConsts.LANGUAGE)) {
                final Map<String, Object> languageMap = (Map<String, Object>) typeLanguageMap
                        .get(JsonLdConsts.LANGUAGE);
                String language = (String) definition.get(JsonLdConsts.LANGUAGE);
                if (language == null) {
                    language = JsonLdConsts.NULL;
                }
                if (!languageMap.containsKey(language)) {
                    languageMap.put(language, term);
                }
                // 3.11)
            } else {
                // 3.11.1)
                final Map<String, Object> languageMap = (Map<String, Object>) typeLanguageMap
                        .get(JsonLdConsts.LANGUAGE);
                // 3.11.2)
                if (!languageMap.containsKey(JsonLdConsts.LANGUAGE)) {
                    languageMap.put(JsonLdConsts.LANGUAGE, term);
                }
                // 3.11.3)
                if (!languageMap.containsKey(JsonLdConsts.NONE)) {
                    languageMap.put(JsonLdConsts.NONE, term);
                }
                // 3.11.4)
                final Map<String, Object> typeMap = (Map<String, Object>) typeLanguageMap
                        .get(JsonLdConsts.TYPE);
                // 3.11.5)
                if (!typeMap.containsKey(JsonLdConsts.NONE)) {
                    typeMap.put(JsonLdConsts.NONE, term);
                }
            }
        }
        // 4)
        return inverse;
    }

    /**
     * Term Selection
     *
     * http://json-ld.org/spec/latest/json-ld-api/#term-selection
     *
     * This algorithm, invoked via the IRI Compaction algorithm, makes use of an
     * active context's inverse context to find the term that is best used to
     * compact an IRI. Other information about a value associated with the IRI
     * is given, including which container mappings and which type mapping or
     * language mapping would be best used to express the value.
     *
     * @return the selected term.
     */
    private String selectTerm(String iri, List<String> containers, String typeLanguage,
            List<String> preferredValues) {
        final Map<String, Object> inv = getInverse();
        // 1)
        final Map<String, Object> containerMap = (Map<String, Object>) inv.get(iri);
        // 2)
        for (final String container : containers) {
            // 2.1)
            if (!containerMap.containsKey(container)) {
                continue;
            }
            // 2.2)
            final Map<String, Object> typeLanguageMap = (Map<String, Object>) containerMap
                    .get(container);
            // 2.3)
            final Map<String, Object> valueMap = (Map<String, Object>) typeLanguageMap
                    .get(typeLanguage);
            // 2.4 )
            for (final String item : preferredValues) {
                // 2.4.1
                if (!valueMap.containsKey(item)) {
                    continue;
                }
                // 2.4.2
                return (String) valueMap.get(item);
            }
        }
        // 3)
        return null;
    }

    /**
     * Retrieve container mapping.
     *
     * @param property
     *            The Property to get a container mapping for.
     * @return The container mapping
     */
    public String getContainer(String property) {
        if (JsonLdConsts.GRAPH.equals(property)) {
            return JsonLdConsts.SET;
        }
        if (JsonLdUtils.isKeyword(property)) {
            return property;
        }
        final Map<String, Object> td = (Map<String, Object>) termDefinitions.get(property);
        if (td == null) {
            return null;
        }
        return (String) td.get(JsonLdConsts.CONTAINER);
    }

    public Boolean isReverseProperty(String property) {
        final Map<String, Object> td = (Map<String, Object>) termDefinitions.get(property);
        if (td == null) {
            return false;
        }
        final Object reverse = td.get(JsonLdConsts.REVERSE);
        return reverse != null && (Boolean) reverse;
    }

    private String getTypeMapping(String property) {
        final Map<String, Object> td = (Map<String, Object>) termDefinitions.get(property);
        if (td == null) {
            return null;
        }
        return (String) td.get(JsonLdConsts.TYPE);
    }

    private String getLanguageMapping(String property) {
        final Map<String, Object> td = (Map<String, Object>) termDefinitions.get(property);
        if (td == null) {
            return null;
        }
        return (String) td.get(JsonLdConsts.LANGUAGE);
    }

    Map<String, Object> getTermDefinition(String key) {
        return ((Map<String, Object>) termDefinitions.get(key));
    }

    public Object expandValue(String activeProperty, Object value) throws JsonLdError {
        final Map<String, Object> rval = newMap();
        final Map<String, Object> td = getTermDefinition(activeProperty);
        // 1)
        if (td != null && JsonLdConsts.ID.equals(td.get(JsonLdConsts.TYPE))) {
            // TODO: i'm pretty sure value should be a string if the @type is
            // @id
            rval.put(JsonLdConsts.ID, expandIri(value.toString(), true, false, null, null));
            return rval;
        }
        // 2)
        if (td != null && JsonLdConsts.VOCAB.equals(td.get(JsonLdConsts.TYPE))) {
            // TODO: same as above
            rval.put(JsonLdConsts.ID, expandIri(value.toString(), true, true, null, null));
            return rval;
        }
        // 3)
        rval.put(JsonLdConsts.VALUE, value);
        // 4)
        if (td != null && td.containsKey(JsonLdConsts.TYPE)) {
            rval.put(JsonLdConsts.TYPE, td.get(JsonLdConsts.TYPE));
        }
        // 5)
        else if (value instanceof String) {
            // 5.1)
            if (td != null && td.containsKey(JsonLdConsts.LANGUAGE)) {
                final String lang = (String) td.get(JsonLdConsts.LANGUAGE);
                if (lang != null) {
                    rval.put(JsonLdConsts.LANGUAGE, lang);
                }
            }
            // 5.2)
            else if (this.get(JsonLdConsts.LANGUAGE) != null) {
                rval.put(JsonLdConsts.LANGUAGE, this.get(JsonLdConsts.LANGUAGE));
            }
        }
        return rval;
    }

    public Map<String, Object> serialize() {
        final Map<String, Object> ctx = newMap();
        if (this.get(JsonLdConsts.BASE) != null
                && !this.get(JsonLdConsts.BASE).equals(options.getBase())) {
            ctx.put(JsonLdConsts.BASE, this.get(JsonLdConsts.BASE));
        }
        if (this.get(JsonLdConsts.LANGUAGE) != null) {
            ctx.put(JsonLdConsts.LANGUAGE, this.get(JsonLdConsts.LANGUAGE));
        }
        if (this.get(JsonLdConsts.VOCAB) != null) {
            ctx.put(JsonLdConsts.VOCAB, this.get(JsonLdConsts.VOCAB));
        }
        for (final String term : termDefinitions.keySet()) {
            final Map<String, Object> definition = (Map<String, Object>) termDefinitions.get(term);
            if (definition.get(JsonLdConsts.LANGUAGE) == null
                    && definition.get(JsonLdConsts.CONTAINER) == null
                    && definition.get(JsonLdConsts.TYPE) == null
                    && (definition.get(JsonLdConsts.REVERSE) == null
                            || Boolean.FALSE.equals(definition.get(JsonLdConsts.REVERSE)))) {
                final String cid = this.compactIri((String) definition.get(JsonLdConsts.ID));
                ctx.put(term, term.equals(cid) ? definition.get(JsonLdConsts.ID) : cid);
            } else {
                final Map<String, Object> defn = newMap();
                final String cid = this.compactIri((String) definition.get(JsonLdConsts.ID));
                final Boolean reverseProperty = Boolean.TRUE
                        .equals(definition.get(JsonLdConsts.REVERSE));
                if (!(term.equals(cid) && !reverseProperty)) {
                    defn.put(reverseProperty ? JsonLdConsts.REVERSE : JsonLdConsts.ID, cid);
                }
                final String typeMapping = (String) definition.get(JsonLdConsts.TYPE);
                if (typeMapping != null) {
                    defn.put(JsonLdConsts.TYPE, JsonLdUtils.isKeyword(typeMapping) ? typeMapping
                            : compactIri(typeMapping, true));
                }
                if (definition.get(JsonLdConsts.CONTAINER) != null) {
                    defn.put(JsonLdConsts.CONTAINER, definition.get(JsonLdConsts.CONTAINER));
                }
                final Object lang = definition.get(JsonLdConsts.LANGUAGE);
                if (definition.get(JsonLdConsts.LANGUAGE) != null) {
                    defn.put(JsonLdConsts.LANGUAGE, Boolean.FALSE.equals(lang) ? null : lang);
                }
                ctx.put(term, defn);
            }
        }

        final Map<String, Object> rval = newMap();
        if (!(ctx == null || ctx.isEmpty())) {
            rval.put(JsonLdConsts.CONTEXT, ctx);
        }
        return rval;
    }

}