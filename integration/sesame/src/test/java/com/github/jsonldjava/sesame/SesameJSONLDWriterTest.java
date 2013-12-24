/**
 * 
 */
package com.github.jsonldjava.sesame;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Test;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.rio.ParserConfig;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RDFWriterTest;
import org.openrdf.rio.WriterConfig;
import org.openrdf.rio.helpers.BasicParserSettings;
import org.openrdf.rio.helpers.BasicWriterSettings;
import org.openrdf.rio.helpers.StatementCollector;

import com.github.jsonldjava.sesame.SesameJSONLDParserFactory;
import com.github.jsonldjava.sesame.SesameJSONLDWriterFactory;

/**
 * @author Peter Ansell p_ansell@yahoo.com
 */
public class SesameJSONLDWriterTest extends RDFWriterTest {

    public SesameJSONLDWriterTest() {
        super(new SesameJSONLDWriterFactory(), new SesameJSONLDParserFactory());
    }

    @Test
    @Override
    public void testRoundTrip() throws RDFHandlerException, IOException, RDFParseException {
        // Overriding test as it is implemented as an RDF-1.0 test that is not
        // compatible
        // with RDF-1.1 Typed Literals after translating them to have xsd:String
        // and rdf:langString.
        final String ex = "http://example.org/";

        final ValueFactory vf = new ValueFactoryImpl();
        final BNode bnode = vf.createBNode("anon");
        final URI uri1 = vf.createURI(ex, "uri1");
        final URI uri2 = vf.createURI(ex, "uri2");
        final Literal plainLit = vf.createLiteral("plain");
        final Literal dtLit = vf.createLiteral(1);
        final Literal langLit = vf.createLiteral("test", "en");
        final Literal litWithNewline = vf.createLiteral("literal with newline\n");
        final Literal litWithSingleQuotes = vf.createLiteral("'''some single quote text''' - abc");
        final Literal litWithDoubleQuotes = vf
                .createLiteral("\"\"\"some double quote text\"\"\" - abc");

        final Statement st1 = vf.createStatement(bnode, uri1, plainLit);
        final Statement st2 = vf.createStatement(uri1, uri2, langLit, uri2);
        final Statement st3 = vf.createStatement(uri1, uri2, dtLit);
        final Statement st4 = vf.createStatement(uri1, uri2, litWithNewline);
        final Statement st5 = vf.createStatement(uri1, uri2, litWithSingleQuotes);
        final Statement st6 = vf.createStatement(uri1, uri2, litWithDoubleQuotes);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final RDFWriter rdfWriter = rdfWriterFactory.getWriter(out);
        final WriterConfig writerConfig = rdfWriter.getWriterConfig();
        writerConfig.set(BasicWriterSettings.RDF_LANGSTRING_TO_LANG_LITERAL, true);
        writerConfig.set(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL, true);
        rdfWriter.startRDF();
        rdfWriter.handleNamespace("ex", ex);
        rdfWriter.handleStatement(st1);
        rdfWriter.handleStatement(st2);
        rdfWriter.handleStatement(st3);
        rdfWriter.handleStatement(st4);
        rdfWriter.handleStatement(st5);
        rdfWriter.handleStatement(st6);
        rdfWriter.endRDF();

        final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        final RDFParser rdfParser = rdfParserFactory.getParser();
        final ParserConfig config = new ParserConfig();
        config.set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, true);
        config.set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, true);
        rdfParser.setParserConfig(config);
        rdfParser.setValueFactory(vf);
        final Model model = new LinkedHashModel();
        rdfParser.setRDFHandler(new StatementCollector(model));

        rdfParser.parse(in, "foo:bar");
        assertEquals("Unexpected number of namespaces", 1, model.getNamespaces().size());
        assertEquals("Unexpected number of statements", 6, model.size());
        final Model bnodeModel = model.filter(null, uri1,
                vf.createLiteral(plainLit.getLabel(), XMLSchema.STRING));
        assertEquals("Blank node was not round-tripped", 1, bnodeModel.size());
        assertTrue("Blank node was not round-tripped as a blank node", bnodeModel.subjects()
                .iterator().next() instanceof BNode);
        if (rdfParser.getRDFFormat().supportsContexts()) {
            assertTrue(model.contains(st2));
        } else {
            assertTrue(model.contains(vf.createStatement(uri1, uri2, langLit)));
        }
        assertTrue(model.contains(st3));
        assertTrue(
                "missing statement with literal ending on newline",
                model.contains(vf.createStatement(uri1, uri2,
                        vf.createLiteral(litWithNewline.getLabel(), XMLSchema.STRING))));
        assertTrue(
                "missing statement with single quotes",
                model.contains(vf.createStatement(uri1, uri2,
                        vf.createLiteral(litWithSingleQuotes.getLabel(), XMLSchema.STRING))));
        assertTrue(
                "missing statement with single quotes",
                model.contains(vf.createStatement(uri1, uri2,
                        vf.createLiteral(litWithDoubleQuotes.getLabel(), XMLSchema.STRING))));
    }
}
