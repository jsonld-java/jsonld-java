package com.github.jsonldjava.core;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.jsonldjava.core.JsonLdError.Error;
import com.github.jsonldjava.impl.NQuadRDFParser;
import com.github.jsonldjava.impl.NQuadTripleCallback;
import com.github.jsonldjava.impl.TurtleRDFParser;
import com.github.jsonldjava.impl.TurtleTripleCallback;

/** 
 * http://json-ld.org/spec/latest/json-ld-api/#the-jsonldprocessor-interface
 * 
 * @author tristan
 *
 */
public class JsonLdProcessor {

	public static Map<String,Object> compact(Object input, Object context, JsonLdOptions opts) throws JsonLdError {
		// 1)
		// TODO: look into java futures/promises
		
		// 2-6) NOTE: these are all the same steps as in expand
    	Object expanded = expand(input, opts);
    	// 7)
    	if (context instanceof Map && ((Map<String,Object>) context).containsKey("@context")) {
    		context = ((Map<String,Object>) context).get("@context");
    	}
    	Context activeCtx = new Context(opts);
    	activeCtx = activeCtx.parse(context);
    	// 8)
    	Object compacted = new JsonLdApi(opts).compact(activeCtx, null, expanded, opts.getCompactArrays());
    	
    	// final step of Compaction Algorithm
    	// TODO: SPEC:  the result result is a NON EMPTY array, 
    	if (compacted instanceof List) {
    		if (((List<Object>) compacted).isEmpty()) {
    			compacted = new LinkedHashMap<String, Object>();
    		} else {
    			Map<String,Object> tmp = new LinkedHashMap<String, Object>();
    			// TODO: SPEC: doesn't specify to use vocab = true here
    			tmp.put(activeCtx.compactIri("@graph", true), compacted);
    			compacted = tmp;
    		}
    	}
    	if (compacted != null && context != null) {
    		// TODO: figure out if we can make "@context" appear at the start of the keySet
    		if ((context instanceof Map && !((Map<String,Object>) context).isEmpty()) || (context instanceof List && !((List<Object>) context).isEmpty())) {
    			((Map<String,Object>)compacted).put("@context", context);
    		}
    	}
    	
    	// 9)
    	return (Map<String,Object>)compacted;    	
	}
	
	public static List<Object> expand(Object input, JsonLdOptions opts) throws JsonLdError {
    	// 1) 
    	// TODO: look into java futures/promises
    	
    	// 2) TODO: better verification of DOMString IRI
    	if (input instanceof String && ((String)input).contains(":")) {
    		try {
    			RemoteDocument tmp = opts.documentLoader.loadDocument((String)input);
    			input = tmp.document;
    			// TODO: figure out how to deal with remote context
    		} catch (Exception e) {
    			throw new JsonLdError(Error.LOADING_DOCUMENT_FAILED, e.getMessage());
    		}
    		// if set the base in options should override the base iri in the active context
    		// thus only set this as the base iri if it's not already set in options
    		if (opts.getBase() == null) { 
    			opts.setBase((String)input); 
    		}
    	}
    	
    	// 3)
    	Context activeCtx = new Context(opts);
    	// 4)
    	if (opts.getExpandContext() != null) {
    		Object exCtx = opts.getExpandContext();
    		if (exCtx instanceof Map && ((Map<String,Object>) exCtx).containsKey("@context")) {
    			exCtx = ((Map<String,Object>) exCtx).get("@context");
    		}
    		activeCtx = activeCtx.parse(exCtx);
    	}
    	
    	// 5)
    	// TODO: add support for getting a context from HTTP when content-type is set to a jsonld compatable format
    	
    	// 6)
    	Object expanded = new JsonLdApi(opts).expand(activeCtx, input);
    	
    	// final step of Expansion Algorithm
        if (expanded instanceof Map && ((Map) expanded).containsKey("@graph")
                && ((Map) expanded).size() == 1) {
            expanded = ((Map<String, Object>) expanded).get("@graph");
        } else if (expanded == null) {
            expanded = new ArrayList<Object>();
        }

        // normalize to an array
        if (!(expanded instanceof List)) {
            final List<Object> tmp = new ArrayList<Object>();
            tmp.add(expanded);
            expanded = tmp;
        }
        return (List<Object>) expanded;
    }
    
    public static List<Object> expand(Object input) throws JsonLdError {
        return expand(input, new JsonLdOptions(""));
    }

    public static Object flatten(Object input, Object context, JsonLdOptions opts) throws JsonLdError {
    	// 2-6) NOTE: these are all the same steps as in expand
    	Object expanded = expand(input, opts);
    	// 7)
    	if (context instanceof Map && ((Map<String,Object>) context).containsKey("@context")) {
    		context = ((Map<String,Object>) context).get("@context");
    	}
    	// 8) NOTE: blank node generation variables are members of JsonLdApi
    	// 9) NOTE: the next block is the Flattening Algorithm described in
    	// http://json-ld.org/spec/latest/json-ld-api/#flattening-algorithm
    	
    	// 1)
    	Map<String,Object> nodeMap = new LinkedHashMap<String, Object>();
    	nodeMap.put("@default", new LinkedHashMap<String, Object>());
    	// 2)
    	new JsonLdApi(opts).generateNodeMap(expanded, nodeMap);
    	// 3)
    	Map<String,Object> defaultGraph = (Map<String, Object>) nodeMap.remove("@default");
    	// 4)
    	for (String graphName : nodeMap.keySet()) {
    		Map<String,Object> graph = (Map<String, Object>) nodeMap.get(graphName);
    		// 4.1+4.2)
    		Map<String,Object> entry;
    		if (!defaultGraph.containsKey(graphName)) {
    			entry = new LinkedHashMap<String, Object>();
    			entry.put("@id", graphName);
    			defaultGraph.put(graphName, entry);
    		} else {
    			entry = (Map<String, Object>) defaultGraph.get(graphName);
    		}
    		// 4.3)
    		// TODO: SPEC doesn't specify that this should only be added if it doesn't exists
    		if (!entry.containsKey("@graph")) {
    			entry.put("@graph", new ArrayList<Object>());
    		}
    		List<String> keys = new ArrayList<String>(graph.keySet());
    		Collections.sort(keys);
    		for (String id : keys) {
    			Map<String,Object> node = (Map<String, Object>) graph.get(id);
    			if (!(node.containsKey("@id") && node.size() == 1)) {
    				((List<Object>) entry.get("@graph")).add(node);
    			}
    		}
    		
    	}
    	// 5)
    	List<Object> flattened = new ArrayList<Object>();
    	// 6)
    	List<String> keys = new ArrayList<String>(defaultGraph.keySet());
		Collections.sort(keys);
		for (String id : keys) {
			Map<String,Object> node = (Map<String, Object>) defaultGraph.get(id);
			if (!(node.containsKey("@id") && node.size() == 1)) {
				flattened.add(node);
			}
		}
		// 8)
		if (context != null && !flattened.isEmpty()) {
			Context activeCtx = new Context(opts);
			activeCtx = activeCtx.parse(context);
			// TODO: only instantiate one jsonldapi
			Object compacted = new JsonLdApi(opts).compact(activeCtx, null, flattened, opts.getCompactArrays());
			if (!(compacted instanceof List)) {
				List<Object> tmp = new ArrayList<Object>();
				tmp.add(compacted);
				compacted = tmp;
			}
			String alias = activeCtx.compactIri("@graph");
			Map<String,Object> rval = activeCtx.serialize();
			rval.put(alias, compacted);
			return rval;
		}
    	return flattened;
    }
    
    public static Object flatten(Object input, JsonLdOptions opts) throws JsonLdError {
    	return flatten(input, null, opts);
    }
    
	public static Map<String, Object> frame(Object input, Object frame, JsonLdOptions options) throws JsonLdError {
		
		if (frame instanceof Map) {
			frame = JsonLdUtils.clone(frame);
		}
		// TODO string/IO input
		
		Object expandedInput = expand(input, options); 
		List<Object> expandedFrame = expand(frame, options);
		
		JsonLdApi api = new JsonLdApi(expandedInput, options);
		List<Object> framed = api.frame(expandedInput, expandedFrame);
		Context activeCtx = api.context.parse(((Map<String,Object>) frame).get("@context"));
		
		Object compacted = api.compact(activeCtx, null, framed);
		if (!(compacted instanceof List)) {
			List<Object> tmp = new ArrayList<Object>();
			tmp.add(compacted);
			compacted = tmp;
		}
		String alias = activeCtx.compactIri("@graph");
		Map<String,Object> rval = activeCtx.serialize();
		rval.put(alias, compacted);
		JsonLdUtils.removePreserve(activeCtx, rval, options);
		return rval;
	}
	
	 /**
     * a registry for RDF Parsers (in this case, JSONLDSerializers) used by
     * fromRDF if no specific serializer is specified and options.format is set.
     * 
     * TODO: this would fit better in the document loader class
     */
    private static Map<String, RDFParser> rdfParsers = new LinkedHashMap<String, RDFParser>() {
        {
            // automatically register nquad serializer
            put("application/nquads", new NQuadRDFParser());
            put("text/turtle", new TurtleRDFParser());
        }
    };

    public static void registerRDFParser(String format, RDFParser parser) {
        rdfParsers.put(format, parser);
    }

    public static void removeRDFParser(String format) {
        rdfParsers.remove(format);
    }

	 /**
     * Converts an RDF dataset to JSON-LD.
     * 
     * @param dataset
     *            a serialized string of RDF in a format specified by the format
     *            option or an RDF dataset to convert.
     * @param [options] the options to use: [format] the format if input is not
     *        an array: 'application/nquads' for N-Quads (default). [useRdfType]
     *        true to use rdf:type, false to use @type (default: false).
     *        [useNativeTypes] true to convert XSD types into native types
     *        (boolean, integer, double), false not to (default: true).
     * 
     * @param callback
     *            (err, output) called once the operation completes.
     */
    public static Object fromRDF(Object dataset, JsonLdOptions options) throws JsonLdError {
        // handle non specified serializer case

        RDFParser parser = null;

        if (options.format == null && dataset instanceof String) {
            // attempt to parse the input as nquads
            options.format = "application/nquads";
        }

        if (rdfParsers.containsKey(options.format)) {
            parser = rdfParsers.get(options.format);
        } else {
        	throw new JsonLdError(JsonLdError.Error.UNKNOWN_FORMAT, options.format);
        }

        // convert from RDF
        return fromRDF(dataset, options, parser);
    }

    public static Object fromRDF(Object dataset) throws JsonLdError {
        return fromRDF(dataset, new JsonLdOptions(""));
    }

    /**
     * Uses a specific serializer.
     * 
     */
    public static Object fromRDF(Object input, JsonLdOptions options, RDFParser parser)
            throws JsonLdError {

        final RDFDataset dataset = parser.parse(input);

        // convert from RDF
        final Object rval = new JsonLdApi(options).fromRDF(dataset);

        // re-process using the generated context if outputForm is set
        if (options.outputForm != null) {
            if ("expanded".equals(options.outputForm)) {
                return rval;
            } else if ("compacted".equals(options.outputForm)) {
                return compact(rval, dataset.getContext(), options);
            } else if ("flattened".equals(options.outputForm)) {
                return flatten(rval, dataset.getContext(), options);
            } else {
                throw new JsonLdError(JsonLdError.Error.UNKNOWN_ERROR);
            }
        }
        return rval;
    }

    public static Object fromRDF(Object input, RDFParser parser) throws JsonLdError {
        return fromRDF(input, new JsonLdOptions(""), parser);
    }
    
    /**
     * Outputs the RDF dataset found in the given JSON-LD object.
     * 
     * @param input
     *            the JSON-LD input.
     * @param callback
     *            A callback that is called when the input has been converted to
     *            Quads (null to use options.format instead).
     * @param [options] the options to use: [base] the base IRI to use. [format]
     *        the format to use to output a string: 'application/nquads' for
     *        N-Quads (default). [loadContext(url, callback(err, url, result))]
     *        the context loader.
     * @param callback
     *            (err, dataset) called once the operation completes.
     */
    public static Object toRDF(Object input, JSONLDTripleCallback callback, JsonLdOptions options)
            throws JsonLdError {

        Object expandedInput = expand(input, options);

        JsonLdApi api = new JsonLdApi(expandedInput, options);
        final RDFDataset dataset = api.toRDF();
        
        
        // generate namespaces from context
        if (options.useNamespaces) {
            List<Map<String, Object>> _input;
            if (input instanceof List) {
                _input = (List<Map<String, Object>>) input;
            } else {
                _input = new ArrayList<Map<String, Object>>();
                _input.add((Map<String, Object>) input);
            }
            for (final Map<String, Object> e : _input) {
                if (e.containsKey("@context")) {
                    dataset.parseContext((Map<String, Object>) e.get("@context"));
                }
            }
        }

        if (callback != null) {
            return callback.call(dataset);
        }

        if (options.format != null) {
            if ("application/nquads".equals(options.format)) {
                return new NQuadTripleCallback().call(dataset);
            } else if ("text/turtle".equals(options.format)) {
                return new TurtleTripleCallback().call(dataset);
            } else {
                throw new JsonLdError(JsonLdError.Error.UNKNOWN_FORMAT, options.format);
            }
        }
        return dataset;
    }

    public static Object toRDF(Object input, JsonLdOptions options) throws JsonLdError {
        return toRDF(input, null, options);
    }

    public static Object toRDF(Object input, JSONLDTripleCallback callback)
            throws JsonLdError {
        return toRDF(input, callback, new JsonLdOptions(""));
    }

    public static Object toRDF(Object input) throws JsonLdError {
        return toRDF(input, new JsonLdOptions(""));
    }
    
    /**
     * Performs RDF dataset normalization on the given JSON-LD input. The output
     * is an RDF dataset unless the 'format' option is used.
     * 
     * @param input
     *            the JSON-LD input to normalize.
     * @param [options] the options to use: [base] the base IRI to use. [format]
     *        the format if output is a string: 'application/nquads' for
     *        N-Quads. [loadContext(url, callback(err, url, result))] the
     *        context loader.
     * @param callback
     *            (err, normalized) called once the operation completes.
     * @throws JSONLDProcessingError
     */
    public static Object normalize(Object input, JsonLdOptions options) throws JsonLdError {
        
        final JsonLdOptions opts = options.clone();
        opts.format = null;
        RDFDataset dataset = (RDFDataset) toRDF(input, opts);
        
        return new JsonLdApi(options).normalize(dataset);
    }

    public static Object normalize(Object input) throws JsonLdError {
        return normalize(input, new JsonLdOptions(""));
    }
    
}
