package com.github.jsonldjava.core;

import static com.github.jsonldjava.core.JSONLDUtils.compareShortestLeast;
import static com.github.jsonldjava.core.JSONLDUtils.isArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.jsonldjava.utils.URL;

/**
 * A helper class which still stores all the values in a map but gives member
 * variables easily access certain keys
 * 
 * @author tristan
 * 
 */
public class ActiveContext extends LinkedHashMap<String, Object> {
    public ActiveContext() {
        this(new Options());
    }

    public ActiveContext(Options options) {
        super();
        init(options);
    }

    public ActiveContext(Map<String, Object> map, Options options) {
        super(map);
        init(options);
    }

    public ActiveContext(Map<String, Object> map) {
        super(map);
        init(new Options());
    }

    private void init(Options options) {
        final Object base = URL.parse(options.base);

        this.put("mappings", new LinkedHashMap<String, Object>());

        this.put("@base", base);
        mappings = (Map<String, Object>) this.get("mappings");

    }

    public Object getContextValue(String key, String type) {

        // return null for invalid key
        if (key == null) {
            return null;
        }

        Object rval = null;

        // get default language
        if ("@language".equals(type) && this.containsKey(type)) {
            rval = this.get(type);
        }

        // get specific entry information
        if (this.mappings.containsKey(key)) {
            final Map<String, Object> entry = (Map<String, Object>) this.mappings.get(key);

            if (type == null) {
                rval = entry;
            } else if (entry.containsKey(type)) {
                rval = entry.get(type);
            }
        }

        return rval;
    }

    @Override
    public ActiveContext clone() {
        return (ActiveContext) super.clone();
    }

    public Map<String, Object> mappings;
    public Map<String, Object> inverse = null;

    // TODO: remove this when it's not needed by old code
    public Map<String, List<String>> keywords;

    /**
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

        inverse = new LinkedHashMap<String, Object>();

        // handle default language
        String defaultLanguage = (String) this.get("@language");
        if (defaultLanguage == null) {
            defaultLanguage = "@none";
        }
        // create term selections for each mapping in the context, ordererd by
        // shortest and then lexicographically least
        final List<String> terms = new ArrayList<String>(mappings.keySet());
        Collections.sort(terms, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return compareShortestLeast(a, b);
            }
        });

        for (final String term : terms) {
            final Map<String, Object> mapping = (Map<String, Object>) mappings.get(term);
            if (mappings.containsKey(term) && mapping == null) {
                continue;
            }

            String container = (String) mapping.get("@container");
            if (container == null) {
                container = "@none";
            }

            // iterate over every IRI in the mapping
            List<String> ids;
            if (!isArray(mapping.get("@id"))) {
                ids = new ArrayList<String>();
                ids.add((String) mapping.get("@id"));
            } else {
                ids = (List<String>) mapping.get("@id");
            }
            for (final String iri : ids) {
                Map<String, Object> entry = (Map<String, Object>) inverse.get(iri);

                // initialize entry
                if (entry == null) {
                    entry = new LinkedHashMap<String, Object>();
                    inverse.put(iri, entry);
                }

                // add new entry
                if (!entry.containsKey(container) || entry.get(container) == null) {
                    entry.put(container, new LinkedHashMap<String, Object>() {
                        {
                            put("@language", new LinkedHashMap<String, Object>());
                            put("@type", new LinkedHashMap<String, Object>());
                        }
                    });
                }
                entry = (Map<String, Object>) entry.get(container);

                // term is preferred for values using @reverse
                if (mapping.containsKey("reverse") && Boolean.TRUE.equals(mapping.get("reverse"))) {
                    addPreferredTerm(mapping, term, entry.get("@type"), "@reverse");
                }
                // term is preferred for values using specific type
                else if (mapping.containsKey("@type")) {
                    addPreferredTerm(mapping, term, entry.get("@type"), mapping.get("@type"));
                }
                // term is preferred for values using specific language
                else if (mapping.containsKey("@language")) {
                    String language = (String) mapping.get("@language");
                    if (language == null) {
                        language = "@null";
                    }
                    addPreferredTerm(mapping, term, entry.get("@language"), language);
                }
                // term is preferred for values w/default language or no type
                // and no language
                else {
                    // add an entry for the default language
                    addPreferredTerm(mapping, term, entry.get("@type"), defaultLanguage);

                    // add entries for no type and no language
                    addPreferredTerm(mapping, term, entry.get("@type"), "@none");
                    addPreferredTerm(mapping, term, entry.get("@language"), "@none");
                }
            }
        }

        return inverse;
    }

    /**
     * Adds the term for the given entry if not already added.
     * 
     * @param mapping
     *            the term mapping.
     * @param term
     *            the term to add.
     * @param entry
     *            the inverse context typeOrLanguage entry to add to.
     * @param typeOrLanguageValue
     *            the key in the entry to add to.
     * 
     *            NOTE: variables are left as Object to make it look a bit nicer
     *            in the calling function
     */
    private void addPreferredTerm(Map<String, Object> mapping, String term, Object entry,
            Object typeOrLanguageValue) {
        if (!((Map<String, Object>) entry).containsKey(typeOrLanguageValue)) {
            ((Map<String, Object>) entry).put((String) typeOrLanguageValue, term);
        }
    }
}