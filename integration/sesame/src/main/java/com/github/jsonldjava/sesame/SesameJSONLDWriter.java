/**
 *
 */
package com.github.jsonldjava.sesame;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.Model;
import org.openrdf.model.Namespace;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.helpers.BasicWriterSettings;
import org.openrdf.rio.helpers.JSONLDMode;
import org.openrdf.rio.helpers.JSONLDSettings;
import org.openrdf.rio.helpers.RDFWriterBase;
import org.openrdf.rio.helpers.StatementCollector;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

/**
 * @author Peter Ansell p_ansell@yahoo.com
 *
 */
public class SesameJSONLDWriter extends RDFWriterBase implements RDFWriter {

    private final Model model = new LinkedHashModel();

    private final StatementCollector statementCollector = new StatementCollector(model);

    private final Writer writer;

    /**
     * Create a SesameJSONLDWriter using a {@link java.io.OutputStream}
     *
     * @param outputStream
     *            The OutputStream to write to.
     */
    public SesameJSONLDWriter(OutputStream outputStream) {
        this(new BufferedWriter(new OutputStreamWriter(outputStream, Charset.forName("UTF-8"))));
    }

    /**
     * Create a SesameJSONLDWriter using a {@link java.io.Writer}
     *
     * @param writer
     *            The Writer to write to.
     */
    public SesameJSONLDWriter(Writer writer) {
        this.writer = writer;
    }

    @Override
    public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
        model.setNamespace(prefix, uri);
    }

    @Override
    public void startRDF() throws RDFHandlerException {
        statementCollector.clear();
        model.clear();
    }

    @Override
    public void endRDF() throws RDFHandlerException {
        final SesameRDFParser serialiser = new SesameRDFParser();
        try {
            Object output = JsonLdProcessor.fromRDF(model, serialiser);

            final JSONLDMode mode = getWriterConfig().get(JSONLDSettings.JSONLD_MODE);

            final JsonLdOptions opts = new JsonLdOptions();
            // opts.addBlankNodeIDs =
            // getWriterConfig().get(BasicParserSettings.PRESERVE_BNODE_IDS);
            opts.setUseRdfType(getWriterConfig().get(JSONLDSettings.USE_RDF_TYPE));
            opts.setUseNativeTypes(getWriterConfig().get(JSONLDSettings.USE_NATIVE_TYPES));
            // opts.optimize = getWriterConfig().get(JSONLDSettings.OPTIMIZE);

            if (mode == JSONLDMode.EXPAND) {
                output = JsonLdProcessor.expand(output, opts);
            }
            // TODO: Implement inframe in JSONLDSettings
            final Object inframe = null;
            if (mode == JSONLDMode.FLATTEN) {
                output = JsonLdProcessor.flatten(output, inframe, opts);
            }
            if (mode == JSONLDMode.COMPACT) {
                final Map<String, Object> ctx = new LinkedHashMap<String, Object>();
                addPrefixes(ctx, model.getNamespaces());
                final Map<String, Object> localCtx = new HashMap<String, Object>();
                localCtx.put("@context", ctx);

                output = JsonLdProcessor.compact(output, localCtx, opts);
            }
            if (getWriterConfig().get(BasicWriterSettings.PRETTY_PRINT)) {
                JsonUtils.writePrettyPrint(writer, output);
            } else {
                JsonUtils.write(writer, output);
            }

        } catch (final JsonLdError e) {
            throw new RDFHandlerException("Could not render JSONLD", e);
        } catch (final JsonGenerationException e) {
            throw new RDFHandlerException("Could not render JSONLD", e);
        } catch (final JsonMappingException e) {
            throw new RDFHandlerException("Could not render JSONLD", e);
        } catch (final IOException e) {
            throw new RDFHandlerException("Could not render JSONLD", e);
        }
    }

    @Override
    public void handleStatement(Statement st) throws RDFHandlerException {
        statementCollector.handleStatement(st);
    }

    @Override
    public void handleComment(String comment) throws RDFHandlerException {
    }

    @Override
    public RDFFormat getRDFFormat() {
        return RDFFormat.JSONLD;
    }

    private static void addPrefixes(Map<String, Object> ctx, Set<Namespace> namespaces) {
        for (final Namespace ns : namespaces) {
            ctx.put(ns.getPrefix(), ns.getName());
        }

    }
}
