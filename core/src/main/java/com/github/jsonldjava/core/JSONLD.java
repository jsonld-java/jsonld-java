package com.github.jsonldjava.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.jsonldjava.core.JSONLDUtils.*;

public class JSONLD {
	
	/**
	 * Performs JSON-LD compaction.
	 *
	 * @param input the JSON-LD input to compact.
	 * @param ctx the context to compact with.
	 * @param [options] options to use:
	 *          [base] the base IRI to use.
	 *          [strict] use strict mode (default: true).
	 *          [compactArrays] true to compact arrays to single values when
	 *            appropriate, false not to (default: true).
	 *          [graph] true to always output a top-level graph (default: false).
	 *          [skipExpansion] true to assume the input is expanded and skip
	 *            expansion, false not to, defaults to false.
	 *          [loadContext(url, callback(err, url, result))] the context loader.
	 * @param callback(err, compacted, ctx) called once the operation completes.
	 */
    public static Object compact(Object input, Object ctx, Options opts) throws JSONLDProcessingError {
    	// nothing to compact
        if (input == null) {
            return null;
        }
        
        // NOTE: javascript does this check before input check
        if (ctx == null) {
        	throw new JSONLDProcessingError("The compaction context must not be null.")
    		.setType(JSONLDProcessingError.Error.COMPACT_ERROR);
        }
        
        // set default options
        if (opts.base == null) {
        	opts.base = "";
        }
        if (opts.strict == null) {
            opts.strict = true;
        }
        if (opts.compactArrays == null) {
        	opts.compactArrays = true;
        }
        if (opts.graph == null) {
            opts.graph = false;
        }
        if (opts.skipExpansion == null) {
            opts.skipExpansion = false;
        }
        //JSONLDProcessor p = new JSONLDProcessor(opts);

        // expand input then do compaction
        Object expanded;
        try {
        	if (opts.skipExpansion) {
        		expanded = input;
        	} else {
        		expanded = JSONLD.expand(input, opts);
        	}
        } catch (JSONLDProcessingError e) {
        	throw new JSONLDProcessingError("Could not expand input before compaction.")
        		.setType(JSONLDProcessingError.Error.COMPACT_ERROR)
        		.setDetail("cause", e);
        }

        // process context
        ActiveContext activeCtx = new ActiveContext(opts);
        try {
            activeCtx = JSONLD.processContext(activeCtx, ctx, opts);
        } catch (JSONLDProcessingError e) {
            throw new JSONLDProcessingError("Could not process context before compaction.")
            	.setType(JSONLDProcessingError.Error.COMPACT_ERROR)
        		.setDetail("cause", e);
        }

        // do compaction
        Object compacted = new JSONLDProcessor(opts).compact(activeCtx, null, expanded);

        // cleanup
        if (opts.compactArrays && !opts.graph && isArray(compacted)) {
        	// simplify to a single item
        	if (((List<Object>)compacted).size() == 1) {
        		compacted = ((List<Object>) compacted).get(0);
        	}
        	// simplify to an empty object
        	else if (((List<Object>)compacted).size() == 0) {
        		compacted = new HashMap<String, Object>();
        	}
        }
        // always use array if graph option is on
        else if (opts.graph && isObject(compacted)) {
            List<Object> tmp = new ArrayList<Object>();
            tmp.add(compacted);
            compacted = tmp;
        }

        // follow @context key
        if (isObject(ctx) && ((Map<String, Object>) ctx).containsKey("@context")) {
            ctx = ((Map<String, Object>) ctx).get("@context");
        }

        // build output context
        ctx = JSONLDUtils.clone(ctx);
        if (!isArray(ctx)) {
            List<Object> lctx = new ArrayList<Object>();
            lctx.add(ctx);
            ctx = lctx;
        }
        
        // remove empty contexts
        List<Object> tmp = (List<Object>) ctx;
        ctx = new ArrayList<Object>();
        for (Object i : tmp) {
            if (!isObject(i) || ((Map) i).size() > 0) {
                ((List<Object>) ctx).add(i);
            }
        }

        // remove array if only one context
        boolean hasContext = ((List) ctx).size() > 0;
        if (((List) ctx).size() == 1) {
            ctx = ((List) ctx).get(0);
        }

        // add context and/or @graph
        if (isArray(compacted)) {
            String kwgraph = compactIri(activeCtx, "@graph");
            Object graph = compacted;
            compacted = new HashMap<String, Object>();
            if (hasContext) {
                ((Map<String, Object>) compacted).put("@context", ctx);
            }
            ((Map<String, Object>) compacted).put(kwgraph, graph);
        } else if (isObject(compacted) && hasContext) {
        	// reorder keys so @context is first
            Map<String, Object> graph = (Map<String, Object>) compacted;
            compacted = new HashMap<String, Object>();
            ((Map) compacted).put("@context", ctx);
            for (String key : graph.keySet()) {
                ((Map<String, Object>) compacted).put(key, graph.get(key));
            }
        }

        return compacted;
    }

    public static Object compact(Object input, Map<String, Object> ctx) throws JSONLDProcessingError {
        return compact(input, ctx, new Options("", true));
    }
	
    /**
     * Performs JSON-LD expansion.
     *
     * @param input the JSON-LD input to expand.
     * @param [options] the options to use:
     *          [base] the base IRI to use.
     *          [keepFreeFloatingNodes] true to keep free-floating nodes,
     *            false not to, defaults to false.
     * @return the expanded result as a list
     */
    public static List<Object> expand(Object input, Options opts) throws JSONLDProcessingError {
    	if (opts.base == null) {
    		opts.base = "";
    	}
    	
    	if (opts.keepFreeFloatingNodes == null) {
    		opts.keepFreeFloatingNodes = false;
    	}
    	
    	// resolve all @context URLs in the input
    	input = JSONLDUtils.clone(input);
    	JSONLDUtils.resolveContextUrls(input);
    	
    	// do expansion
    	JSONLDProcessor p = new JSONLDProcessor(opts);
        Object expanded = p.expand(new ActiveContext(opts), null, input, false);

        // optimize away @graph with no other properties
        if (isObject(expanded) && ((Map) expanded).containsKey("@graph") && ((Map) expanded).size() == 1) {
            expanded = ((Map<String, Object>) expanded).get("@graph");
        } else if (expanded == null) {
        	expanded = new ArrayList<Object>();
        }

        // normalize to an array
        if (!isArray(expanded)) {
            List<Object> tmp = new ArrayList<Object>();
            tmp.add(expanded);
            expanded = tmp;
        }
        return (List<Object>) expanded;
    }

    public static Object expand(Object input) throws JSONLDProcessingError {
        return expand(input, new Options(""));
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
	 * @throws JSONLDProcessingError 
     */
    public static Object frame(Object input, Object frame, Options opts) throws JSONLDProcessingError {
    	if (input == null) {
            return null;
        }
    	if (opts.embed == null) {
    		opts.embed  = true;
    	}
    	//if (opts.optimize == null) {
    	//	opts.optimize = false;
    	//}
    	if (opts.explicit == null) {
    		opts.explicit = false;
    	}
    	if (opts.omitDefault == null) {
    		opts.omitDefault = false;
    	}
    	
    	
    	JSONLDProcessor p = new JSONLDProcessor(opts);
    	
    	// preserve frame context
    	ActiveContext ctx = new ActiveContext();
    	Map<String, Object> fctx;
    	if (frame instanceof Map && ((Map<String, Object>) frame).containsKey("@context")) {
    		fctx = (Map<String, Object>) ((Map<String, Object>) frame).get("@context");
    		ctx = JSONLDProcessor.processContext(ctx, fctx, opts);
    	} else {
    		fctx = new HashMap<String, Object>();
    	}
    	
    	// expand input
    	Object _input = JSONLD.expand(input, opts);
    	Object _frame = JSONLD.expand(frame, opts);
    	
    	Object framed = p.frame(_input, _frame);
    	
    	opts.graph = true;
    	
    	Map<String,Object> compacted = (Map<String, Object>) JSONLD.compact(framed, fctx, opts);
    	//String graph = JSONLDProcessor.compactIri(ctx, "@graph");
    	//compacted.put(graph, p.removePreserve(ctx, compacted.get(graph)));
        return compacted;
    }
    
    public static Object frame(Object input, Object frame) throws JSONLDProcessingError {
    	return frame(input, frame, new Options(""));
    }
    
    /**
     * Processes a local context, resolving any URLs as necessary, and returns a
     * new active context in its callback.
     *
     * @param activeCtx the current active context.
     * @param localCtx the local context to process.
     * @param [options] the options to use:
     *          [loadContext(url, callback(err, url, result))] the context loader.
     * @param callback(err, ctx) called once the operation completes.
     */
    private static ActiveContext processContext(ActiveContext activeCtx, Object localCtx, Options opts) throws JSONLDProcessingError {
    	// set default options
    	if (opts.base == null) {
    		opts.base = "";
    	}
    	
    	// return initial context early for null context
    	if (localCtx == null) {
    		return new ActiveContext(opts);
    	}
    	
    	// retrieve URLs in localCtx
    	localCtx = JSONLDUtils.clone(localCtx);
    	if (isString(localCtx) || (isObject(localCtx) && !((HashMap<String, Object>) localCtx).containsKey("@context"))) {
    		Map<String,Object> tmp = new HashMap<String, Object>();
    		tmp.put("@context", localCtx);
    		localCtx = tmp;
    	}
    	
    	resolveContextUrls(localCtx);
    	
    	return new JSONLDProcessor(opts).processContext(activeCtx, localCtx);
    }
    
    /**
     * Performs RDF normalization on the given JSON-LD input. The output is
     * a sorted array of RDF statements unless the 'format' option is used.
     *
     * @param input the JSON-LD input to normalize.
     * @param [options] the options to use:
     *          [base] the base IRI to use.
     * @param [options] the options to use:
     *          [format] the format if output is a string:
     *            'application/nquads' for N-Quads.
     *          [resolver(url, callback(err, jsonCtx))] the URL resolver to use.
     * @param callback(err, normalized) called once the operation completes.
     */
    public static Object normalize(Object input, Options opts) throws JSONLDProcessingError {
    	if (opts.base == null) {
    		opts.base = "";
    	}
    	
    	Object expanded = JSONLD.expand(input, opts);
    	return new JSONLDProcessor(opts).normalize(expanded);
    }
    
    /**
     * Outputs the RDF statements found in the given JSON-LD object.
     *
     * @param input the JSON-LD input.
     * @param [options] the options to use:
     *          [base] the base IRI to use.
     *          [format] the format to use to output a string:
     *            'application/nquads' for N-Quads (default).
     *          [collate] true to output all statements at once (in an array
     *            or as a formatted string), false to output one statement at
     *            a time (default).
     *          [resolver(url, callback(err, jsonCtx))] the URL resolver to use.
     * @param callback(err, statement) called when a statement is output, with the
     *          last statement as null.
     */
    public static void toRDF(Object input, Options opts, JSONLDTripleCallback callback) throws JSONLDProcessingError {
    	if (opts.base == null) {
    		opts.base = "";
    	}
    	if (opts.collate == null) {
    		opts.collate = false;
    	}
    	
    	if (opts.collate) {
    		// TODO:
    	}
    	
    	Object expanded = JSONLD.expand(input, opts);
    	JSONLDProcessor p = new JSONLDProcessor(opts);
    	UniqueNamer namer = new UniqueNamer("_:t");
    	p.toRDF(expanded, namer, null, null, null, callback);
    }
    
    public static void toRDF(Object input, JSONLDTripleCallback callback) throws JSONLDProcessingError {
    	toRDF(input, new Options(""), callback);
    }
    
    public static Object fromRDF(Object input, Options opts, JSONLDSerializer serializer) throws JSONLDProcessingError {
    	if (opts.useRdfType == null) {
    		opts.useRdfType = false;
    	}
    	if (opts.useNativeTypes == null) {
    		opts.useNativeTypes = true;
    	}
    	serializer.parse(input);
    	Object rval = new JSONLDProcessor(opts).fromRDF(serializer.getStatements());
    	rval = serializer.finalize(rval);
    	return rval;
    }
    
    public static Object fromRDF(Object input, JSONLDSerializer serializer) throws JSONLDProcessingError {
    	return fromRDF(input, new Options(""), serializer);
    }

	public static Object simplify(Object input, Options opts) throws JSONLDProcessingError {
		// TODO Auto-generated method stub
		if (opts.base == null) {
    		opts.base = "";
    	}
		return new JSONLDProcessor(opts).simplify(input);
	}
	
	public static Object simplify(Object input) throws JSONLDProcessingError {
		return simplify(input, new Options(""));
	}
}
