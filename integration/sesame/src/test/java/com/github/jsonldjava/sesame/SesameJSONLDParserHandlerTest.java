/**
 *
 */
package com.github.jsonldjava.sesame;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;

import org.openrdf.model.Model;
import org.openrdf.model.Namespace;
import org.openrdf.model.Statement;
import org.openrdf.rio.AbstractParserHandlingTest;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFWriter;

/**
 * Unit tests for {@link SesameJSONLDParser} related to handling of datatypes
 * and languages.
 *
 * @author Peter Ansell p_ansell@yahoo.com
 */
public class SesameJSONLDParserHandlerTest extends AbstractParserHandlingTest {

    @Override
    protected InputStream getUnknownDatatypeStream(Model unknownDatatypeStatements)
            throws Exception {
        return writeJSONLD(unknownDatatypeStatements);
    }

    @Override
    protected InputStream getKnownDatatypeStream(Model knownDatatypeStatements) throws Exception {
        return writeJSONLD(knownDatatypeStatements);
    }

    @Override
    protected InputStream getUnknownLanguageStream(Model unknownLanguageStatements)
            throws Exception {
        return writeJSONLD(unknownLanguageStatements);
    }

    @Override
    protected InputStream getKnownLanguageStream(Model knownLanguageStatements) throws Exception {
        return writeJSONLD(knownLanguageStatements);
    }

    @Override
    protected RDFParser getParser() {
        return new SesameJSONLDParser();
    }

    /**
     * Helper method to write the given model to JSON-LD and return an
     * InputStream containing the results.
     *
     * @param statements
     * @return An {@link InputStream} containing the results.
     * @throws RDFHandlerException
     */
    private InputStream writeJSONLD(Model statements) throws RDFHandlerException {
        final StringWriter writer = new StringWriter();

        final RDFWriter jsonldWriter = new SesameJSONLDWriter(writer);
        jsonldWriter.startRDF();
        for (final Namespace prefix : statements.getNamespaces()) {
            jsonldWriter.handleNamespace(prefix.getPrefix(), prefix.getName());
        }
        for (final Statement nextStatement : statements) {
            jsonldWriter.handleStatement(nextStatement);
        }
        jsonldWriter.endRDF();

        // System.out.println(writer.toString());

        return new ByteArrayInputStream(writer.toString().getBytes(Charset.forName("UTF-8")));
    }

}
