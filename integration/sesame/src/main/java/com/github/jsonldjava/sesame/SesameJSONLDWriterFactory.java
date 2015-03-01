/**
 *
 */
package com.github.jsonldjava.sesame;

import java.io.OutputStream;
import java.io.Writer;

import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RDFWriterFactory;

/**
 * An {@link RDFWriterFactory} that creates instances of
 * {@link SesameJSONLDWriter}.
 *
 * @author Peter Ansell p_ansell@yahoo.com
 */
public class SesameJSONLDWriterFactory implements RDFWriterFactory {

    @Override
    public RDFFormat getRDFFormat() {
        return RDFFormat.JSONLD;
    }

    @Override
    public RDFWriter getWriter(OutputStream out) {
        return new SesameJSONLDWriter(out);
    }

    @Override
    public RDFWriter getWriter(Writer writer) {
        return new SesameJSONLDWriter(writer);
    }

}
