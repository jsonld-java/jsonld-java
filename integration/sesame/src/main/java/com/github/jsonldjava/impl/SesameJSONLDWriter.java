/**
 * 
 */
package com.github.jsonldjava.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Map;

import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.helpers.BasicParserSettings;
import org.openrdf.rio.helpers.BasicWriterSettings;
import org.openrdf.rio.helpers.JSONLDMode;
import org.openrdf.rio.helpers.JSONLDSettings;
import org.openrdf.rio.helpers.RDFWriterBase;
import org.openrdf.rio.helpers.StatementCollector;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.jsonldjava.core.JSONLD;
import com.github.jsonldjava.core.JSONLDProcessingError;
import com.github.jsonldjava.core.Options;
import com.github.jsonldjava.utils.JSONUtils;

/**
 * @author Peter Ansell p_ansell@yahoo.com
 * 
 */
public class SesameJSONLDWriter extends RDFWriterBase implements RDFWriter {

    private final Model model = new LinkedHashModel();

    private final StatementCollector statementCollector = new StatementCollector(model);

    private final Writer writer;

    /**
     * Create a SesameJSONLDWriter using a java.io.OutputStream
     * 
     * @param outputStream
     */
    public SesameJSONLDWriter(OutputStream outputStream) {
        this(new BufferedWriter(new OutputStreamWriter(outputStream, Charset.forName("UTF-8"))));
    }

    /**
     * Create a SesameJSONLDWriter using a java.io.Writer
     */
    public SesameJSONLDWriter(Writer nextWriter) {
        writer = nextWriter;
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
            Object output = JSONLD.fromRDF(model, serialiser);

            final JSONLDMode mode = getWriterConfig().get(JSONLDSettings.JSONLD_MODE);

            final Options opts = new Options();
            opts.addBlankNodeIDs = getWriterConfig().get(BasicParserSettings.PRESERVE_BNODE_IDS);
            opts.useRdfType = getWriterConfig().get(JSONLDSettings.USE_RDF_TYPE);
            opts.useNativeTypes = getWriterConfig().get(JSONLDSettings.USE_NATIVE_TYPES);
            // opts.optimize = getWriterConfig().get(JSONLDSettings.OPTIMIZE);

            if (mode == JSONLDMode.EXPAND) {
                output = JSONLD.expand(output, opts);
            }
            // TODO: Implement inframe in JSONLDSettings
            final Object inframe = null;
            if (mode == JSONLDMode.FLATTEN) {
                output = JSONLD.frame(output, (Map<String, Object>) inframe, opts);
            }
            if (mode == JSONLDMode.COMPACT) {
                output = JSONLD.simplify(output, opts);
            }
            if (getWriterConfig().get(BasicWriterSettings.PRETTY_PRINT)) {
                JSONUtils.writePrettyPrint(writer, output);
            } else {
                JSONUtils.write(writer, output);
            }

        } catch (final JSONLDProcessingError e) {
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

}
