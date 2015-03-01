/**
 *
 */
package com.github.jsonldjava.sesame;

import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParserFactory;

/**
 * An {@link RDFParserFactory} that creates instances of
 * {@link SesameJSONLDParser}.
 *
 * @author Peter Ansell p_ansell@yahoo.com
 */
public class SesameJSONLDParserFactory implements RDFParserFactory {

    @Override
    public RDFFormat getRDFFormat() {
        return RDFFormat.JSONLD;
    }

    @Override
    public RDFParser getParser() {
        return new SesameJSONLDParser();
    }

}
