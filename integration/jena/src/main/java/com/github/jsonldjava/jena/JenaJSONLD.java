/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.jsonldjava.jena;

import org.apache.jena.riot.IO_Jena;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.LangBuilder;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParserRegistry;
import org.apache.jena.riot.RDFWriterRegistry;
import org.apache.jena.riot.ReaderRIOT;
import org.apache.jena.riot.ReaderRIOTFactory;
import org.apache.jena.riot.WriterDatasetRIOT;
import org.apache.jena.riot.WriterDatasetRIOTFactory;
import org.apache.jena.riot.WriterGraphRIOT;
import org.apache.jena.riot.WriterGraphRIOTFactory;
import org.apache.jena.riot.adapters.RDFReaderRIOT;
import org.apache.jena.riot.adapters.RDFWriterRIOT;
import org.apache.jena.riot.system.RiotLib;

import com.github.jsonldjava.core.JsonLdProcessor;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.impl.IO_Ctl;

/**
 * Jena binding for JSON-LD.
 * <p>
 * The bindings must be initialized by calling {@link JenaJSONLD#init()}. After
 * initialization, the language {@link JSONLD} can be used with
 * {@link RDFDataMgr} for read/write of JSONLD. The file extension ".jsonld" and
 * media type "application/ld+json" is also recognized. The classic
 * {@link Model} can also read/write JSON-LD using the language "JSON-LD".
 * 
 * 
 * @author Andy Seaborne
 * @author Stian Soiland-Reyes
 * 
 */
public class JenaJSONLD {

    /**
     * Factory for JSONLD RIOT graph reader.
     * 
     */
    public static final class JsonLDReaderRIOTFactory implements ReaderRIOTFactory {
        @Override
        public ReaderRIOT create(Lang language) {
            return new JsonLDReader();
        }
    }

    /**
     * Factory for JSONLD RIOT dataset writer.
     * 
     */
    public static class JsonLDWriterDatasetRIOTFactory implements WriterDatasetRIOTFactory {
        @Override
        public WriterDatasetRIOT create(RDFFormat syntaxForm) {
            return new JsonLDWriter(syntaxForm);
        }
    }

    /**
     * Factory for JSONLD RIOT graph writer.
     * 
     */
    public static class JsonLDWriterGraphRIOTFactory implements WriterGraphRIOTFactory {
        @Override
        public WriterGraphRIOT create(RDFFormat syntaxForm) {
            return RiotLib.adapter(new JsonLDWriter(syntaxForm));
        }
    }

    /**
     * Classic RDFReader for JSONLD. Must be a subclass as registration is done
     * by class.
     */
    public static class JsonLDRDFReader extends RDFReaderRIOT {
        public JsonLDRDFReader() {
            super(JSONLD.getName());
        }
    }

    /**
     * Classic RDFWriter for JSONLD. Must be a subclass as registration is done
     * by class.
     */
    public static class JsonLDRDFWriter extends RDFWriterRIOT {
        public JsonLDRDFWriter() {
            super(JSONLD.getName());
        }
    }

    public static Lang JSONLD = LangBuilder.create("JSON-LD", "application/ld+json")
    // .addAltNames("RDF/JSON-LD")
            .addFileExtensions("jsonld").build();

    public static RDFFormat JSONLD_FORMAT_FLAT = new RDFFormat(JSONLD, RDFFormat.FLAT);

    public static RDFFormat JSONLD_FORMAT_PRETTY = new RDFFormat(JSONLD, RDFFormat.PRETTY);

    static {
        init();
    }

    /**
     * Initialize JSONLD readers and writers with Jena.
     * <p>
     * After initialization, the language {@link JSONLD} can be used with
     * {@link RDFDataMgr} for read/write. Additionally the classic {@link Model}
     * (as "JSON-LD")
     * 
     * This method is safe to call multiple times.
     * 
     */
    public static void init() {
        IO_Ctl.init();
        registerReader();
        registerWriter();
        registerWithJsonLD();
    }

    protected static void registerWithJsonLD() {
        JsonLdProcessor.registerRDFParser(JSONLD.getContentType().getContentType(),
                new JenaRDFParser());
    }

    protected static void registerReader() {
        // This just registers the name, not the parser.
        RDFLanguages.register(JSONLD);

        // Register the parser factory.
        final JsonLDReaderRIOTFactory rfactory = new JsonLDReaderRIOTFactory();
        RDFParserRegistry.registerLangTriples(JSONLD, rfactory);
        RDFParserRegistry.registerLangQuads(JSONLD, rfactory);

        // Register for Model.read (old world)
        IO_Jena.registerForModelRead(JSONLD.getName(), JsonLDRDFReader.class);
    }

    protected static void registerWriter() {

        // Register the default format for the language.
        RDFWriterRegistry.register(JSONLD, JSONLD_FORMAT_PRETTY);

        // For datasets
        final WriterDatasetRIOTFactory wfactory = new JsonLDWriterDatasetRIOTFactory();
        // Uses the same code for each form.
        RDFWriterRegistry.register(JSONLD_FORMAT_PRETTY, wfactory);
        RDFWriterRegistry.register(JSONLD_FORMAT_FLAT, wfactory);

        // For graphs
        final WriterGraphRIOTFactory wfactory2 = new JsonLDWriterGraphRIOTFactory();
        RDFWriterRegistry.register(JSONLD_FORMAT_PRETTY, wfactory2);
        RDFWriterRegistry.register(JSONLD_FORMAT_FLAT, wfactory2);

        // Register for use with Model.write (old world)
        IO_Jena.registerForModelWrite(JSONLD.getName(), JsonLDRDFWriter.class);
    }

}
