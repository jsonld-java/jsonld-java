package com.github.jsonldjava.sesame;

import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;

import org.junit.Test;
import org.openrdf.model.Model;
import org.openrdf.model.util.ModelUtil;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.Rio;
import org.openrdf.rio.WriterConfig;
import org.openrdf.rio.helpers.JSONLDMode;
import org.openrdf.rio.helpers.JSONLDSettings;

public class SesameEmptyPrefixTest {

    @Test
    public void testEmptyPrefixDefault() throws Exception {
        final String input = "@prefix :      <http://www.example.com/resource/100/v1#> ."
                + "@prefix dc:    <http://purl.org/dc/elements/1.1/> ."
                + " :G { "
                + "  <http://www.example.com/archive/100/v1> dc:isVersionOf <http://www.example.com/resource/100>    .        }";
        final Model parse = Rio.parse(new StringReader(input), "", RDFFormat.TRIG);

        final StringWriter output = new StringWriter();
        Rio.write(parse, output, RDFFormat.JSONLD);

        System.out.println(output);

        final Model reparse = Rio.parse(new StringReader(output.toString()), "", RDFFormat.JSONLD);

        assertTrue(ModelUtil.equals(parse, reparse));
    }

    @Test
    public void testEmptyPrefixCompact() throws Exception {
        final String input = "@prefix :      <http://www.example.com/resource/100/v1#> ."
                + "@prefix dc:    <http://purl.org/dc/elements/1.1/> ."
                + " :G { "
                + "  <http://www.example.com/archive/100/v1> dc:isVersionOf <http://www.example.com/resource/100>    .        }";
        final Model parse = Rio.parse(new StringReader(input), "", RDFFormat.TRIG);

        final WriterConfig config = new WriterConfig();
        config.set(JSONLDSettings.JSONLD_MODE, JSONLDMode.COMPACT);

        final StringWriter output = new StringWriter();
        Rio.write(parse, output, RDFFormat.JSONLD, config);

        System.out.println(output);

        final Model reparse = Rio.parse(new StringReader(output.toString()), "", RDFFormat.JSONLD);

        assertTrue(ModelUtil.equals(parse, reparse));
    }
}
