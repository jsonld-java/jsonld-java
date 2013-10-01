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

import com.hp.hpl.jena.rdf.model.impl.IO_Ctl;
import com.hp.hpl.jena.rdf.model.impl.RDFReaderFImpl;
import com.hp.hpl.jena.rdf.model.impl.RDFWriterFImpl;

public class JenaJSONLD {
    // public static

    public static Lang JSONLD = LangBuilder
            .create("JSON-LD", "application/ld+json")
            // .addAltNames("RDF/JSON-LD")
            .addFileExtensions("jsonld").build();
    public static RDFFormat JSONLD_FORMAT_PRETTY = new RDFFormat(JSONLD, RDFFormat.PRETTY);
    public static RDFFormat JSONLD_FORMAT_FLAT = new RDFFormat(JSONLD, RDFFormat.FLAT);

    public static void init() {
        IO_Ctl.init();
        registerReader();
        registerWriter();
    }

    static {
        init();
    }

    public static class JSONLDWriterGraphRIOTFactory implements
            WriterGraphRIOTFactory {
        @Override
        public WriterGraphRIOT create(RDFFormat syntaxForm) {
            return RiotLib.adapter(new JsonLDWriter(syntaxForm));
        }
    }

    public static class JSONLDWriterDatasetRIOTFactory implements
            WriterDatasetRIOTFactory {
        @Override
        public WriterDatasetRIOT create(RDFFormat syntaxForm) {
            return new JsonLDWriter(syntaxForm);
        }
    }

    public static final class JsonLDReaderRIOTFactory implements
            ReaderRIOTFactory {
        @Override
        public ReaderRIOT create(Lang language) {
            return new JsonLDReader();
        }
    }

    // Classic RDFReader. Must be a subclass as registration is done by class
    public static class RDFReaderRIOT_RDFJSONLD extends RDFReaderRIOT {
        public RDFReaderRIOT_RDFJSONLD() {
            super(JSONLD.getName());
        }
    }

    protected static void registerReader() {
        // This just registers the name, not the parser.        
        RDFLanguages.register(JSONLD);

        // Register the parser factory.
        JsonLDReaderRIOTFactory rfactory = new JsonLDReaderRIOTFactory();
        RDFParserRegistry.registerLangTriples(JSONLD, rfactory);
        RDFParserRegistry.registerLangQuads(JSONLD, rfactory);

        // Register for Model.read (old world)
        IO_Jena.registerForModelRead(JSONLD.getName(),
                RDFReaderRIOT_RDFJSONLD.class);
    }

    protected static void unregisterReader() {
        RDFLanguages.unregister(JSONLD);
        RDFParserRegistry.removeRegistration(JSONLD);
        
        
        //IO_Jena.registerForModelRead(JSONLD.getName(), null);
        // FIXME: IO_Jena can't handle null above - so we'll cheat:
        RDFReaderFImpl.setBaseReaderClassName(JSONLD.getName(), null);
    }
    
    // ---- Writer
    public static class RDFWriterRIOT_RDFJSONLD extends RDFWriterRIOT {
        public RDFWriterRIOT_RDFJSONLD() {
            super(JSONLD.getName());
        }
    }

    protected static void registerWriter() {


        // Register the default format for the language.
        RDFWriterRegistry.register(JSONLD, JSONLD_FORMAT_PRETTY);

        // For datasets
        WriterDatasetRIOTFactory wfactory = new JSONLDWriterDatasetRIOTFactory();
        // Uses the same code for each form.
        RDFWriterRegistry.register(JSONLD_FORMAT_PRETTY, wfactory);
        RDFWriterRegistry.register(JSONLD_FORMAT_FLAT, wfactory);

        // For graphs
        WriterGraphRIOTFactory wfactory2 = new JSONLDWriterGraphRIOTFactory();
        RDFWriterRegistry.register(JSONLD_FORMAT_PRETTY, wfactory2);
        RDFWriterRegistry.register(JSONLD_FORMAT_FLAT, wfactory2);

        // Register for use with Model.write (old world)
        IO_Jena.registerForModelWrite(JSONLD.getName(),
                RDFWriterRIOT_RDFJSONLD.class);
    }
    
    protected static void unregisterWriter() {
        RDFWriterRegistry.register(JSONLD, null);
        RDFWriterRegistry.register(JSONLD_FORMAT_PRETTY, (WriterGraphRIOTFactory)null);
        RDFWriterRegistry.register(JSONLD_FORMAT_FLAT, (WriterGraphRIOTFactory)null);
        RDFWriterRegistry.register(JSONLD_FORMAT_PRETTY, (WriterDatasetRIOTFactory)null);
        RDFWriterRegistry.register(JSONLD_FORMAT_FLAT, (WriterGraphRIOTFactory)null);
        
        IO_Jena.registerForModelWrite(JSONLD.getName(), null);
        // FIXME: IO_Jena can't handle null above - so we'll cheat:
        RDFWriterFImpl.setBaseWriterClassName(JSONLD.getName(), null);
    }

}
