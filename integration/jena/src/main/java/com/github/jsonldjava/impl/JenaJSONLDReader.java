package com.github.jsonldjava.impl;

import java.io.Reader;

import com.github.jsonldjava.core.JSONLD;
import com.github.jsonldjava.core.Options;
import com.github.jsonldjava.utils.JSONUtils;
import com.hp.hpl.jena.n3.JenaReaderBase;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFReader;

public class JenaJSONLDReader extends JenaReaderBase implements RDFReader {

    public static final String JSON_LD = "JSON-LD";

    public static void registerWithJena() {
        // Strange Jena hack to register reader
        ModelFactory.createDefaultModel().setReaderClassName(JSON_LD, JenaJSONLDReader.class.getCanonicalName());
        // Note: The above will only work on a flat classpath, not within OSGi
    }
    
    @Override
    protected void readWorker(Model model, Reader reader, String base)
            throws Exception {
        Object input = JSONUtils.fromReader(reader);
        JenaTripleCallback callback = new JenaTripleCallback();
        callback.setJenaModel(model);
        JSONLD.toRDF(input, callback, new Options(base));
    }
}
