package com.github.jsonldjava.impl;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import com.github.jsonldjava.core.JSONLD;
import com.github.jsonldjava.core.Options;
import com.github.jsonldjava.utils.JSONUtils;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFErrorHandler;
import com.hp.hpl.jena.rdf.model.RDFWriter;

public class JenaJSONLDWriter implements RDFWriter {

    public static final String PROP_OPTIONS = "options";
    public static final String PROP_PRETTY = "pretty";

    private static final boolean DEFAULT_PRETTY = true;
    private static final Charset UTF8 = Charset.forName("utf-8");

    public static void registerWithJena() {
        // Strange Jena hack to register writer
        ModelFactory.createDefaultModel().setWriterClassName(
                JenaJSONLDReader.JSON_LD,
                JenaJSONLDWriter.class.getCanonicalName());
        // Note: The above will only work on a flat classpath, not within OSGi
    }

    protected Options defaultOptions;
    protected RDFErrorHandler errHandler;

    private boolean pretty = DEFAULT_PRETTY;

    @Override
    public RDFErrorHandler setErrorHandler(RDFErrorHandler errHandler) {
        RDFErrorHandler old = this.errHandler;
        this.errHandler = errHandler;
        return old;
    }

    @Override
    public Object setProperty(String propName, Object propValue) {
        if (propName.equals(PROP_OPTIONS)) {
            Options old = defaultOptions;
            defaultOptions = (Options) propValue;
            return old;
        }
        if (propName.equals(PROP_PRETTY)) {
            pretty = toBoolean(propValue, DEFAULT_PRETTY);
        }

        return null;
    }

    private boolean toBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) (value));
        }
        throw new IllegalArgumentException("Not a boolean: " + value);
    }

    @Override
    public void write(Model model, OutputStream out, String base) {
        write(model, new OutputStreamWriter(out, UTF8), base);
    }

    @Override
    public void write(Model model, Writer out, String base) {
        if (base == null) {
            base = ""; // JSONLD Options expect a string
        }
        Options options;
        if (defaultOptions != null) {
            options = defaultOptions.clone();
            options.base = base;
        } else {
            options = new Options(base);
        }

        JenaRDFParser parser = new JenaRDFParser();
        try {
            Object json = JSONLD.fromRDF(model, options, parser);
            if (pretty) {
                JSONUtils.writePrettyPrint(out, json);
            } else {
                JSONUtils.write(out, json);
            }
        } catch (Exception e) {
            if (errHandler == null) {
                throw new RuntimeException("Can't parse JSON-LD", e);
            }
            errHandler.error(e);
        }
    }

}
