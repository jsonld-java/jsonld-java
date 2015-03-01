package com.github.jsonldjava.sesame;

import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Locale;

import org.junit.Test;
import org.openrdf.model.Model;
import org.openrdf.model.util.ModelUtil;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.Rio;

/**
 * Test for locale-insensitive numeric representations that match the XML Schema
 * Datatype specification.
 *
 * @author Peter Ansell p_ansell@yahoo.com
 * @see <a href="https://github.com/jsonld-java/jsonld-java/issues/131">Github
 *      issue #133</a>
 */
public class SesameLocaleNumericTest {

    @Test
    public void testLocaleUS() throws Exception {
        final Locale oldDefault = Locale.getDefault();

        try {
            Locale.setDefault(Locale.US);
            final String input = getTestString();
            final Model parse = Rio.parse(new StringReader(input), "", RDFFormat.JSONLD);

            final StringWriter output = new StringWriter();
            Rio.write(parse, output, RDFFormat.JSONLD);

            System.out.println(output);

            final Model reparse = Rio.parse(new StringReader(output.toString()), "",
                    RDFFormat.JSONLD);

            assertTrue(ModelUtil.equals(parse, reparse));
        } finally {
            Locale.setDefault(oldDefault);
        }
    }

    @Test
    public void testLocaleFrench() throws Exception {
        final Locale oldDefault = Locale.getDefault();

        try {
            Locale.setDefault(Locale.FRANCE);
            final String input = getTestString();
            final Model parse = Rio.parse(new StringReader(input), "", RDFFormat.JSONLD);

            final StringWriter output = new StringWriter();
            Rio.write(parse, output, RDFFormat.JSONLD);

            System.out.println(output);

            final Model reparse = Rio.parse(new StringReader(output.toString()), "",
                    RDFFormat.JSONLD);

            assertTrue(ModelUtil.equals(parse, reparse));
        } finally {
            Locale.setDefault(oldDefault);
        }
    }

    private String getTestString() {
        return "{" + "\"@id\": \"http://www.ex.com/product\"," + "\"http://schema.org/price\": {"
                + "\"@value\": 100.00" + "}}}";
    }
}
