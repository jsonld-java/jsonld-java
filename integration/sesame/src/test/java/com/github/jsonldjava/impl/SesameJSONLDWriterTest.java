/**
 * 
 */
package com.github.jsonldjava.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Ignore;
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

import com.github.jsonldjava.impl.SesameJSONLDParserFactory;
import com.github.jsonldjava.impl.SesameJSONLDWriterFactory;

/**
 * @author Peter Ansell p_ansell@yahoo.com
 */
public class SesameJSONLDWriterTest extends RDFWriterTest {

	public SesameJSONLDWriterTest() {
		super(new SesameJSONLDWriterFactory(), new SesameJSONLDParserFactory());
	}

	@Test
	@Override
	public void testRoundTrip() throws RDFHandlerException, IOException,
			RDFParseException {
		// Overriding test as it is implemented as an RDF-1.0 test that is not
		// compatible
		// with RDF-1.1 Typed Literals after translating them to have xsd:String
		// and rdf:langString.
		String ex = "http://example.org/";

		ValueFactory vf = new ValueFactoryImpl();
		BNode bnode = vf.createBNode("anon");
		URI uri1 = vf.createURI(ex, "uri1");
		URI uri2 = vf.createURI(ex, "uri2");
		Literal plainLit = vf.createLiteral("plain");
		Literal dtLit = vf.createLiteral(1);
		Literal langLit = vf.createLiteral("test", "en");
		Literal litWithNewline = vf.createLiteral("literal with newline\n");
		Literal litWithSingleQuotes = vf
				.createLiteral("'''some single quote text''' - abc");
		Literal litWithDoubleQuotes = vf
				.createLiteral("\"\"\"some double quote text\"\"\" - abc");

		Statement st1 = vf.createStatement(bnode, uri1, plainLit);
		Statement st2 = vf.createStatement(uri1, uri2, langLit, uri2);
		Statement st3 = vf.createStatement(uri1, uri2, dtLit);
		Statement st4 = vf.createStatement(uri1, uri2, litWithNewline);
		Statement st5 = vf.createStatement(uri1, uri2, litWithSingleQuotes);
		Statement st6 = vf.createStatement(uri1, uri2, litWithDoubleQuotes);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		RDFWriter rdfWriter = rdfWriterFactory.getWriter(out);
		WriterConfig writerConfig = rdfWriter.getWriterConfig();
		writerConfig.set(BasicWriterSettings.RDF_LANGSTRING_TO_LANG_LITERAL,
				true);
		writerConfig.set(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL, true);
		rdfWriter.handleNamespace("ex", ex);
		rdfWriter.startRDF();
		rdfWriter.handleStatement(st1);
		rdfWriter.handleStatement(st2);
		rdfWriter.handleStatement(st3);
		rdfWriter.handleStatement(st4);
		rdfWriter.handleStatement(st5);
		rdfWriter.handleStatement(st6);
		rdfWriter.endRDF();

		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		RDFParser rdfParser = rdfParserFactory.getParser();
		ParserConfig config = new ParserConfig();
		config.set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, true);
		config.set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, true);
		rdfParser.setParserConfig(config);
		rdfParser.setValueFactory(vf);
		Model model = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(model));

		rdfParser.parse(in, "foo:bar");

		assertEquals("Unexpected number of statements", 6, model.size());
		Model bnodeModel = model.filter(null, uri1,
				vf.createLiteral(plainLit.getLabel(), XMLSchema.STRING));
		assertEquals("Blank node was not round-tripped", 1, bnodeModel.size());
		assertTrue("Blank node was not round-tripped as a blank node",
				bnodeModel.subjects().iterator().next() instanceof BNode);
		if (rdfParser.getRDFFormat().supportsContexts()) {
			assertTrue(model.contains(st2));
		} else {
			assertTrue(model.contains(vf.createStatement(uri1, uri2, langLit)));
		}
		assertTrue(model.contains(st3));
		assertTrue("missing statement with literal ending on newline",
				model.contains(vf.createStatement(uri1, uri2, vf.createLiteral(
						litWithNewline.getLabel(), XMLSchema.STRING))));
		assertTrue("missing statement with single quotes", model.contains(vf
				.createStatement(uri1, uri2, vf.createLiteral(
						litWithSingleQuotes.getLabel(), XMLSchema.STRING))));
		assertTrue("missing statement with single quotes", model.contains(vf
				.createStatement(uri1, uri2, vf.createLiteral(
						litWithDoubleQuotes.getLabel(), XMLSchema.STRING))));
	}
}
