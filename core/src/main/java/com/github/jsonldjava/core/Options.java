package com.github.jsonldjava.core;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Options {
    public Options() {
        this.base = "";
        this.strict = true;
    }

    public Options(String base) {
        this.base = base;
        this.strict = true;
    }

    public Options(String base, Boolean strict) {
        this.base = base;
        this.strict = strict;
    }

    public String base = null;
    public Boolean strict = null;
    public Boolean graph = null;
    // public Boolean optimize = null;
    public Map<String, Object> optimizeCtx = null;
    public Boolean embed = null;
    public Boolean explicit = null;
    public Boolean omitDefault = null;
    public Boolean collate = null;
    public Boolean useRdfType = null;
    public Boolean useNativeTypes = null;
    public Boolean produceGeneralizedRdf = null;

    private final Set<String> ignoredKeys = new HashSet<String>();

    // custom option to give to expand and compact which will generate @id's for
    // elements that don't
    // have a specific @id
    public Boolean addBlankNodeIDs = false;

    public Boolean keepFreeFloatingNodes = false;
    public Boolean compactArrays = null;
    public Boolean skipExpansion = null;
    public ActiveContext compactResultsActiveCtx = null;
    public String format = null;
    public String outputForm = null;
    public Boolean useNamespaces = false;

    /**
     * Tells the processor to skip over the key specified by "key" any time it
     * encounters it. Objects under this key will not be manipulated by any of
     * the processor functions and no triples will be created using it.
     * 
     * @param key
     *            The name of the key this processor should ignore.
     */
    public Options ignoreKey(String key) {
        ignoredKeys.add(key);
        return this;
    }

    public Boolean isIgnored(String key) {
        return ignoredKeys.contains(key);
    }

    @Override
    public Options clone() {
        final Options rval = new Options(base);
        rval.strict = strict;
        rval.graph = graph;
        rval.optimizeCtx = (Map<String, Object>) JSONLDUtils.clone(optimizeCtx);
        rval.embed = embed;
        rval.explicit = explicit;
        rval.omitDefault = omitDefault;
        rval.collate = collate;
        rval.useNativeTypes = useNativeTypes;
        rval.useRdfType = useRdfType;
        rval.produceGeneralizedRdf = produceGeneralizedRdf;
        rval.addBlankNodeIDs = addBlankNodeIDs;
        rval.keepFreeFloatingNodes = keepFreeFloatingNodes;
        rval.compactArrays = compactArrays;
        rval.skipExpansion = skipExpansion;
        rval.compactResultsActiveCtx = compactResultsActiveCtx != null ? compactResultsActiveCtx
                .clone() : null;
        rval.format = format;
        rval.outputForm = outputForm;
        rval.useNamespaces = useNamespaces;
        for (final String key : ignoredKeys) {
            rval.ignoreKey(key);
        }
        return rval;
    }
}
