/**
 * 
 */
package de.dfki.km.json.jsonld.impl;

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

import de.dfki.km.json.JSONUtils;
import de.dfki.km.json.jsonld.JSONLD;
import de.dfki.km.json.jsonld.JSONLDProcessingError;

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
    public void parse(final InputStream in, final String baseURI)
	    throws IOException, RDFParseException, RDFHandlerException {
	SesameTripleCallback callback = new SesameTripleCallback(
		getRDFHandler(), valueFactory);

	try {
	    JSONLD.toRDF(JSONUtils.fromInputStream(in), callback);
	} catch (JSONLDProcessingError e) {
	    throw new RDFParseException("Could not parse JSONLD", e);
	}
    }

    @Override
    public void parse(final Reader reader, final String baseURI)
	    throws IOException, RDFParseException, RDFHandlerException {
	SesameTripleCallback callback = new SesameTripleCallback(
		getRDFHandler(), valueFactory);

	try {
	    JSONLD.toRDF(JSONUtils.fromReader(reader), callback);
	} catch (JSONLDProcessingError e) {
	    throw new RDFParseException("Could not parse JSONLD", e);
	}
    }

}
