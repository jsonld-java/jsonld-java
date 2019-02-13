package com.github.jsonldjava.core;

import static com.github.jsonldjava.utils.Obj.newMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.jsonldjava.core.JsonLdError.Error;
import com.github.jsonldjava.impl.NQuadRDFParser;
import com.github.jsonldjava.impl.NQuadTripleCallback;

/**
 * This class implements the <a href=
 * "http://json-ld.org/spec/latest/json-ld-api/#the-jsonldprocessor-interface" >
 * JsonLdProcessor interface</a>, except that it does not currently support
 * asynchronous processing, and hence does not return Promises, instead directly
 * returning the results.
 *
 * @author tristan
 *
 */
public class JsonLdProcessor {

    /**
     * Compacts the given input using the context according to the steps in the
     * <a href="http://www.w3.org/TR/json-ld-api/#compaction-algorithm">
     * Compaction algorithm</a>.
     *
     * @param input
     *            The input JSON-LD object.
     * @param context
     *            The context object to use for the compaction algorithm.
     * @param opts
     *            The {@link JsonLdOptions} that are to be sent to the
     *            compaction algorithm.
     * @return The compacted JSON-LD document
     * @throws JsonLdError
     *             If there is an error while compacting.
     */
    public static Map<String, Object> compact(Object input, Object context, JsonLdOptions opts)
            throws JsonLdError {
        // 1)
        // TODO: look into java futures/promises

        // 2-6) NOTE: these are all the same steps as in expand
        final Object expanded = expand(input, opts);
        // 7)
        if (context instanceof Map
                && ((Map<String, Object>) context).containsKey(JsonLdConsts.CONTEXT)) {
            context = ((Map<String, Object>) context).get(JsonLdConsts.CONTEXT);
        }
        Context activeCtx = new Context(opts);
        activeCtx = activeCtx.parse(context);
        // 8)
        Object compacted = new JsonLdApi(opts).compact(activeCtx, null, expanded,
                opts.getCompactArrays());

        // final step of Compaction Algorithm
        // TODO: SPEC: the result result is a NON EMPTY array,
        if (compacted instanceof List) {
            if (((List<Object>) compacted).isEmpty()) {
                compacted = newMap();
            } else {
                final Map<String, Object> tmp = newMap();
                // TODO: SPEC: doesn't specify to use vocab = true here
                tmp.put(activeCtx.compactIri(JsonLdConsts.GRAPH, true), compacted);
                compacted = tmp;
            }
        }
        if (compacted != null && context != null) {
            // TODO: figure out if we can make "@context" appear at the start of
            // the keySet
            if ((context instanceof Map && !((Map<String, Object>) context).isEmpty())
                    || (context instanceof List && !((List<Object>) context).isEmpty())) {

                if (context instanceof List && ((List<Object>) context).size() == 1
                        && opts.getCompactArrays()) {
                    ((Map<String, Object>) compacted).put(JsonLdConsts.CONTEXT,
                            ((List<Object>) context).get(0));
                } else {
                    ((Map<String, Object>) compacted).put(JsonLdConsts.CONTEXT, context);
                }
            }
        }

        // 9)
        return (Map<String, Object>) compacted;
    }

    /**
     * Expands the given input according to the steps in the
     * <a href="http://www.w3.org/TR/json-ld-api/#expansion-algorithm">Expansion
     * algorithm</a>.
     *
     * @param input
     *            The input JSON-LD object.
     * @param opts
     *            The {@link JsonLdOptions} that are to be sent to the expansion
     *            algorithm.
     * @return The expanded JSON-LD document
     * @throws JsonLdError
     *             If there is an error while expanding.
     */
    public static List<Object> expand(Object input, JsonLdOptions opts) throws JsonLdError {
        // 1)
        // TODO: look into java futures/promises

        // 2) TODO: better verification of DOMString IRI
        if (input instanceof String && ((String) input).contains(":")) {
            try {
                final RemoteDocument tmp = opts.getDocumentLoader().loadDocument((String) input);
                input = tmp.getDocument();
                // TODO: figure out how to deal with remote context
            } catch (final Exception e) {
                throw new JsonLdError(Error.LOADING_DOCUMENT_FAILED, e);
            }
            // if set the base in options should override the base iri in the
            // active context
            // thus only set this as the base iri if it's not already set in
            // options
            if (opts.getBase() == null) {
                opts.setBase((String) input);
            }
        }

        // 3)
        Context activeCtx = new Context(opts);
        // 4)
        if (opts.getExpandContext() != null) {
            Object exCtx = opts.getExpandContext();
            if (exCtx instanceof Map
                    && ((Map<String, Object>) exCtx).containsKey(JsonLdConsts.CONTEXT)) {
                exCtx = ((Map<String, Object>) exCtx).get(JsonLdConsts.CONTEXT);
            }
            activeCtx = activeCtx.parse(exCtx);
        }

        // 5)
        // TODO: add support for getting a context from HTTP when content-type
        // is set to a jsonld compatable format

        // 6)
        Object expanded = new JsonLdApi(opts).expand(activeCtx, input);

        // final step of Expansion Algorithm
        if (expanded instanceof Map && ((Map) expanded).containsKey(JsonLdConsts.GRAPH)
                && ((Map) expanded).size() == 1) {
            expanded = ((Map<String, Object>) expanded).get(JsonLdConsts.GRAPH);
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

    /**
     * Expands the given input according to the steps in the
     * <a href="http://www.w3.org/TR/json-ld-api/#expansion-algorithm">Expansion
     * algorithm</a>, using the default {@link JsonLdOptions}.
     *
     * @param input
     *            The input JSON-LD object.
     * @return The expanded JSON-LD document
     * @throws JsonLdError
     *             If there is an error while expanding.
     */
    public static List<Object> expand(Object input) throws JsonLdError {
        return expand(input, new JsonLdOptions(""));
    }

    public static Object flatten(Object input, Object context, JsonLdOptions opts)
            throws JsonLdError {
        // 2-6) NOTE: these are all the same steps as in expand
        final Object expanded = expand(input, opts);
        // 7)
        if (context instanceof Map
                && ((Map<String, Object>) context).containsKey(JsonLdConsts.CONTEXT)) {
            context = ((Map<String, Object>) context).get(JsonLdConsts.CONTEXT);
        }
        // 8) NOTE: blank node generation variables are members of JsonLdApi
        // 9) NOTE: the next block is the Flattening Algorithm described in
        // http://json-ld.org/spec/latest/json-ld-api/#flattening-algorithm

        // 1)
        final Map<String, Object> nodeMap = newMap();
        nodeMap.put(JsonLdConsts.DEFAULT, newMap());
        // 2)
        new JsonLdApi(opts).generateNodeMap(expanded, nodeMap);
        // 3)
        final Map<String, Object> defaultGraph = (Map<String, Object>) nodeMap
                .remove(JsonLdConsts.DEFAULT);
        // 4)
        for (final String graphName : nodeMap.keySet()) {
            final Map<String, Object> graph = (Map<String, Object>) nodeMap.get(graphName);
            // 4.1+4.2)
            Map<String, Object> entry;
            if (!defaultGraph.containsKey(graphName)) {
                entry = newMap();
                entry.put(JsonLdConsts.ID, graphName);
                defaultGraph.put(graphName, entry);
            } else {
                entry = (Map<String, Object>) defaultGraph.get(graphName);
            }
            // 4.3)
            // TODO: SPEC doesn't specify that this should only be added if it
            // doesn't exists
            if (!entry.containsKey(JsonLdConsts.GRAPH)) {
                entry.put(JsonLdConsts.GRAPH, new ArrayList<Object>());
            }
            final List<String> keys = new ArrayList<String>(graph.keySet());
            Collections.sort(keys);
            for (final String id : keys) {
                final Map<String, Object> node = (Map<String, Object>) graph.get(id);
                if (!(node.containsKey(JsonLdConsts.ID) && node.size() == 1)) {
                    ((List<Object>) entry.get(JsonLdConsts.GRAPH)).add(node);
                }
            }

        }
        // 5)
        final List<Object> flattened = new ArrayList<Object>();
        // 6)
        final List<String> keys = new ArrayList<String>(defaultGraph.keySet());
        Collections.sort(keys);
        for (final String id : keys) {
            final Map<String, Object> node = (Map<String, Object>) defaultGraph.get(id);
            if (!(node.containsKey(JsonLdConsts.ID) && node.size() == 1)) {
                flattened.add(node);
            }
        }
        // 8)
        if (context != null && !flattened.isEmpty()) {
            Context activeCtx = new Context(opts);
            activeCtx = activeCtx.parse(context);
            // TODO: only instantiate one jsonldapi
            Object compacted = new JsonLdApi(opts).compact(activeCtx, null, flattened,
                    opts.getCompactArrays());
            if (!(compacted instanceof List)) {
                final List<Object> tmp = new ArrayList<Object>();
                tmp.add(compacted);
                compacted = tmp;
            }
            final String alias = activeCtx.compactIri(JsonLdConsts.GRAPH);
            final Map<String, Object> rval = activeCtx.serialize();
            rval.put(alias, compacted);
            return rval;
        }
        return flattened;
    }

    /**
     * Flattens the given input and compacts it using the passed context
     * according to the steps in the
     * <a href="http://www.w3.org/TR/json-ld-api/#flattening-algorithm">
     * Flattening algorithm</a>:
     *
     * @param input
     *            The input JSON-LD object.
     * @param opts
     *            The {@link JsonLdOptions} that are to be sent to the
     *            flattening algorithm.
     * @return The flattened JSON-LD document
     * @throws JsonLdError
     *             If there is an error while flattening.
     */
    public static Object flatten(Object input, JsonLdOptions opts) throws JsonLdError {
        return flatten(input, null, opts);
    }

    /**
     * Frames the given input using the frame according to the steps in the
     * <a href=
     * "http://json-ld.org/spec/latest/json-ld-framing/#framing-algorithm">
     * Framing Algorithm</a>.
     *
     * @param input
     *            The input JSON-LD object.
     * @param frame
     *            The frame to use when re-arranging the data of input; either
     *            in the form of an JSON object or as IRI.
     * @param opts
     *            The {@link JsonLdOptions} that are to be sent to the framing
     *            algorithm.
     * @return The framed JSON-LD document
     * @throws JsonLdError
     *             If there is an error while framing.
     */
    public static Map<String, Object> frame(Object input, Object frame, JsonLdOptions opts)
            throws JsonLdError {

        if (frame instanceof Map) {
            frame = JsonLdUtils.clone(frame);
        }
        // TODO string/IO input

        // 2. Set expanded input to the result of using the expand method using
        // input and options.
        final Object expandedInput = expand(input, opts);

        // 3. Set expanded frame to the result of using the expand method using
        // frame and options with expandContext set to null and the
        // frameExpansion option set to true.
        final Object savedExpandedContext = opts.getExpandContext();
        opts.setExpandContext(null);
        opts.setFrameExpansion(true);
        final List<Object> expandedFrame = expand(frame, opts);
        opts.setExpandContext(savedExpandedContext);

        // 4. Set context to the value of @context from frame, if it exists, or
        // to a new empty
        // context, otherwise.
        final JsonLdApi api = new JsonLdApi(expandedInput, opts);
        final Context activeCtx = api.context
                .parse(((Map<String, Object>) frame).get(JsonLdConsts.CONTEXT));
        final List<Object> framed = api.frame(expandedInput, expandedFrame);
        if (opts.getPruneBlankNodeIdentifiers()) {
            JsonLdUtils.pruneBlankNodes(framed);
        }
        Object compacted = api.compact(activeCtx, null, framed, opts.getCompactArrays());
        final Map<String, Object> rval = activeCtx.serialize();
        final boolean addGraph = ((!(compacted instanceof List)) && !opts.getOmitGraph());
        if (addGraph && !(compacted instanceof List)) {
            final List<Object> tmp = new ArrayList<Object>();
            tmp.add(compacted);
            compacted = tmp;
        }
        if (addGraph || (compacted instanceof List)) {
            final String alias = activeCtx.compactIri(JsonLdConsts.GRAPH);
            rval.put(alias, compacted);
        } else if (!addGraph && (compacted instanceof Map)) {
            rval.putAll((Map) compacted);
        }
        JsonLdUtils.removePreserve(activeCtx, rval, opts);
        return rval;
    }

    /**
     * A registry for RDF Parsers (in this case, JSONLDSerializers) used by
     * fromRDF if no specific serializer is specified and options.format is set.
     *
     * TODO: this would fit better in the document loader class
     */
    private static Map<String, RDFParser> rdfParsers = new LinkedHashMap<String, RDFParser>() {
        {
            // automatically register nquad serializer
            put(JsonLdConsts.APPLICATION_NQUADS, new NQuadRDFParser());
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
     * @param options
     *            the options to use: [format] the format if input is not an
     *            array: 'application/nquads' for N-Quads (default).
     *            [useRdfType] true to use rdf:type, false to use @type
     *            (default: false). [useNativeTypes] true to convert XSD types
     *            into native types (boolean, integer, double), false not to
     *            (default: true).
     * @return A JSON-LD object.
     * @throws JsonLdError
     *             If there is an error converting the dataset to JSON-LD.
     */
    public static Object fromRDF(Object dataset, JsonLdOptions options) throws JsonLdError {
        // handle non specified serializer case

        RDFParser parser = null;

        if (options.format == null && dataset instanceof String) {
            // attempt to parse the input as nquads
            options.format = JsonLdConsts.APPLICATION_NQUADS;
        }

        if (rdfParsers.containsKey(options.format)) {
            parser = rdfParsers.get(options.format);
        } else {
            throw new JsonLdError(JsonLdError.Error.UNKNOWN_FORMAT, options.format);
        }

        // convert from RDF
        return fromRDF(dataset, options, parser);
    }

    /**
     * Converts an RDF dataset to JSON-LD, using the default
     * {@link JsonLdOptions}.
     *
     * @param dataset
     *            a serialized string of RDF in a format specified by the format
     *            option or an RDF dataset to convert.
     * @return The JSON-LD object represented by the given RDF dataset
     * @throws JsonLdError
     *             If there was an error converting from RDF to JSON-LD
     */
    public static Object fromRDF(Object dataset) throws JsonLdError {
        return fromRDF(dataset, new JsonLdOptions(""));
    }

    /**
     * Converts an RDF dataset to JSON-LD, using a specific instance of
     * {@link RDFParser}.
     *
     * @param input
     *            a serialized string of RDF in a format specified by the format
     *            option or an RDF dataset to convert.
     * @param options
     *            the options to use: [format] the format if input is not an
     *            array: 'application/nquads' for N-Quads (default).
     *            [useRdfType] true to use rdf:type, false to use @type
     *            (default: false). [useNativeTypes] true to convert XSD types
     *            into native types (boolean, integer, double), false not to
     *            (default: true).
     * @param parser
     *            A specific instance of {@link RDFParser} to use for the
     *            conversion.
     * @return A JSON-LD object.
     * @throws JsonLdError
     *             If there is an error converting the dataset to JSON-LD.
     */
    public static Object fromRDF(Object input, JsonLdOptions options, RDFParser parser)
            throws JsonLdError {

        final RDFDataset dataset = parser.parse(input);

        // convert from RDF
        final Object rval = new JsonLdApi(options).fromRDF(dataset);

        // re-process using the generated context if outputForm is set
        if (options.outputForm != null) {
            if (JsonLdConsts.EXPANDED.equals(options.outputForm)) {
                return rval;
            } else if (JsonLdConsts.COMPACTED.equals(options.outputForm)) {
                return compact(rval, dataset.getContext(), options);
            } else if (JsonLdConsts.FLATTENED.equals(options.outputForm)) {
                return flatten(rval, dataset.getContext(), options);
            } else {
                throw new JsonLdError(JsonLdError.Error.UNKNOWN_ERROR,
                        "Output form was unknown: " + options.outputForm);
            }
        }
        return rval;
    }

    /**
     * Converts an RDF dataset to JSON-LD, using a specific instance of
     * {@link RDFParser}, and the default {@link JsonLdOptions}.
     *
     * @param input
     *            a serialized string of RDF in a format specified by the format
     *            option or an RDF dataset to convert.
     * @param parser
     *            A specific instance of {@link RDFParser} to use for the
     *            conversion.
     * @return A JSON-LD object.
     * @throws JsonLdError
     *             If there is an error converting the dataset to JSON-LD.
     */
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
     * @param options
     *            the options to use: [base] the base IRI to use. [format] the
     *            format to use to output a string: 'application/nquads' for
     *            N-Quads (default). [loadContext(url, callback(err, url,
     *            result))] the context loader.
     * @return The result of executing
     *         {@link JsonLdTripleCallback#call(RDFDataset)} on the results, or
     *         if {@link JsonLdOptions#format} is not null, a result in that
     *         format if it is found, or otherwise the raw {@link RDFDataset}.
     * @throws JsonLdError
     *             If there is an error converting the dataset to JSON-LD.
     */
    public static Object toRDF(Object input, JsonLdTripleCallback callback, JsonLdOptions options)
            throws JsonLdError {

        final Object expandedInput = expand(input, options);

        final JsonLdApi api = new JsonLdApi(expandedInput, options);
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
                if (e.containsKey(JsonLdConsts.CONTEXT)) {
                    dataset.parseContext(e.get(JsonLdConsts.CONTEXT));
                }
            }
        }

        if (callback != null) {
            return callback.call(dataset);
        }

        if (options.format != null) {
            if (JsonLdConsts.APPLICATION_NQUADS.equals(options.format)) {
                return new NQuadTripleCallback().call(dataset);
            } else {
                throw new JsonLdError(JsonLdError.Error.UNKNOWN_FORMAT, options.format);
            }
        }
        return dataset;
    }

    /**
     * Outputs the RDF dataset found in the given JSON-LD object.
     *
     * @param input
     *            the JSON-LD input.
     * @param options
     *            the options to use: [base] the base IRI to use. [format] the
     *            format to use to output a string: 'application/nquads' for
     *            N-Quads (default). [loadContext(url, callback(err, url,
     *            result))] the context loader.
     * @return A JSON-LD object.
     * @throws JsonLdError
     *             If there is an error converting the dataset to JSON-LD.
     */
    public static Object toRDF(Object input, JsonLdOptions options) throws JsonLdError {
        return toRDF(input, null, options);
    }

    /**
     * Outputs the RDF dataset found in the given JSON-LD object, using the
     * default {@link JsonLdOptions}.
     *
     * @param input
     *            the JSON-LD input.
     * @param callback
     *            A callback that is called when the input has been converted to
     *            Quads (null to use options.format instead).
     * @return A JSON-LD object.
     * @throws JsonLdError
     *             If there is an error converting the dataset to JSON-LD.
     */
    public static Object toRDF(Object input, JsonLdTripleCallback callback) throws JsonLdError {
        return toRDF(input, callback, new JsonLdOptions(""));
    }

    /**
     * Outputs the RDF dataset found in the given JSON-LD object, using the
     * default {@link JsonLdOptions}.
     *
     * @param input
     *            the JSON-LD input.
     * @return A JSON-LD object.
     * @throws JsonLdError
     *             If there is an error converting the dataset to JSON-LD.
     */
    public static Object toRDF(Object input) throws JsonLdError {
        return toRDF(input, new JsonLdOptions(""));
    }

    /**
     * Performs RDF dataset normalization on the given JSON-LD input. The output
     * is an RDF dataset unless the 'format' option is used.
     *
     * @param input
     *            the JSON-LD input to normalize.
     * @param options
     *            the options to use: [base] the base IRI to use. [format] the
     *            format if output is a string: 'application/nquads' for
     *            N-Quads. [loadContext(url, callback(err, url, result))] the
     *            context loader.
     * @return The JSON-LD object
     * @throws JsonLdError
     *             If there is an error normalizing the dataset.
     */
    public static Object normalize(Object input, JsonLdOptions options) throws JsonLdError {

        final JsonLdOptions opts = new JsonLdOptions(options.getBase());
        opts.format = null;
        final RDFDataset dataset = (RDFDataset) toRDF(input, opts);

        return new JsonLdApi(options).normalize(dataset);
    }

    /**
     * Performs RDF dataset normalization on the given JSON-LD input. The output
     * is an RDF dataset unless the 'format' option is used. Uses the default
     * {@link JsonLdOptions}.
     *
     * @param input
     *            the JSON-LD input to normalize.
     * @return The JSON-LD object
     * @throws JsonLdError
     *             If there is an error normalizing the dataset.
     */
    public static Object normalize(Object input) throws JsonLdError {
        return normalize(input, new JsonLdOptions(""));
    }

}
