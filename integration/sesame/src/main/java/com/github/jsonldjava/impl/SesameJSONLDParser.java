/**
 * 
 */
package com.github.jsonldjava.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.helpers.RDFParserBase;

import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.utils.JSONUtils;

/**
 * An {@link RDFParser} that links to {@link SesameTripleCallback}.
 * 
 * @author Peter Ansell p_ansell@yahoo.com
 * 
 */
public class SesameJSONLDParser extends RDFParserBase implements RDFParser {

    /**
     * Default constructor
     */
    public SesameJSONLDParser() {
        super();
    }

    /**
     * Creates a Sesame JSONLD Parser using the given {@link ValueFactory} to
     * create new {@link Value}s.
     * 
     * @param valueFactory
     */
    public SesameJSONLDParser(final ValueFactory valueFactory) {
        super(valueFactory);
    }

    @Override
    public RDFFormat getRDFFormat() {
        return RDFFormat.JSONLD;
    }

    @Override
    public void parse(final InputStream in, final String baseURI) throws IOException,
            RDFParseException, RDFHandlerException {
        final SesameTripleCallback callback = new SesameTripleCallback(getRDFHandler(),
                valueFactory, getParserConfig(), getParseErrorListener());

        try {
            JsonLdProcessor.toRDF(JSONUtils.fromInputStream(in), callback);
        } catch (final JsonLdError e) {
            throw new RDFParseException("Could not parse JSONLD", e);
        } catch (final RuntimeException e) {
            if (e.getCause() != null && e.getCause() instanceof RDFParseException) {
                throw (RDFParseException) e.getCause();
            }
            throw e;
        }
    }

    @Override
    public void parse(final Reader reader, final String baseURI) throws IOException,
            RDFParseException, RDFHandlerException {
        final SesameTripleCallback callback = new SesameTripleCallback(getRDFHandler(),
                valueFactory, getParserConfig(), getParseErrorListener());

        try {
            JsonLdProcessor.toRDF(JSONUtils.fromReader(reader), callback);
        } catch (final JsonLdError e) {
            throw new RDFParseException("Could not parse JSONLD", e);
        } catch (final RuntimeException e) {
            if (e.getCause() != null && e.getCause() instanceof RDFParseException) {
                throw (RDFParseException) e.getCause();
            }
            throw e;
        }
    }

}
