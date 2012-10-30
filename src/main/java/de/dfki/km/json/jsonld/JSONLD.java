package de.dfki.km.json.jsonld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.dfki.km.json.jsonld.JSONLDProcessor.ActiveContext;
import de.dfki.km.json.jsonld.JSONLDProcessor.Options;
import de.dfki.km.json.jsonld.JSONLDProcessor.UniqueNamer;

public class JSONLD {
	
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
     * @throws JSONLDProcessingError 
     */
    public static Object compact(Object input, Object ctx, Options opts) throws JSONLDProcessingError {
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
                String kwgraph = JSONLDProcessor.compactIri(activeCtx, "@graph");
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

    public static Object compact(Object input, Map<String, Object> ctx) throws JSONLDProcessingError {
        return compact(input, ctx, new Options("", true));
    }
	
	/**
     * Performs JSON-LD expansion.
     *
     * @param input the JSON-LD input to expand.
     * @throws JSONLDProcessingError 
     */
    public static Object expand(Object input, Options opts) throws JSONLDProcessingError {
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
    	if (opts.optimize == null) {
    		opts.optimize = false;
    	}
    	if (opts.explicit == null) {
    		opts.explicit = false;
    	}
    	if (opts.omitDefault == null) {
    		opts.omitDefault = false;
    	}
    	
    	
    	JSONLDProcessor p = new JSONLDProcessor(opts);
    	
    	// preserve frame context
    	ActiveContext ctx = p.new ActiveContext();
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
    	String graph = JSONLDProcessor.compactIri(ctx, "@graph");
    	compacted.put(graph, JSONLDProcessor.removePreserve(ctx, compacted.get(graph)));
        return compacted;
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
    	UniqueNamer namer = p.new UniqueNamer("_:t");
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
    	return new JSONLDProcessor(opts).fromRDF(serializer.getStatements());
    }
    
    public static Object fromRDF(Object input, JSONLDSerializer serializer) throws JSONLDProcessingError {
    	return fromRDF(input, new Options(""), serializer);
    }
}
