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

public class JenaJSONLD {
    // public static

    public static Lang JSONLD = LangBuilder
            .create("JSON-LD", "application/ld+json")
            // .addAltNames("RDF/JSON-LD")
            .addFileExtensions("jsonld").build();

    public static void init() {
    }

    static {
        // Temp
        IO_Ctl.init();
        initReader();
        initWriter();
    }

    // ---- Reader
    public static class RDFReaderRIOT_RDFJSONLD extends RDFReaderRIOT {
        public RDFReaderRIOT_RDFJSONLD() {
            super(JSONLD.getName());
        }
    }

    private static void initReader() {
        // This just registers the name, not the parser.
        RDFLanguages.register(JSONLD);

        // Register the parser factory.
        ReaderRIOTFactory rfactory = new ReaderRIOTFactory() {
            @Override
            public ReaderRIOT create(Lang language) {
                return new JsonLDReader();
            }
        };

        RDFParserRegistry.registerLangTriples(JSONLD, rfactory);
        RDFParserRegistry.registerLangQuads(JSONLD, rfactory);

        // Register for Model.read (old world)
        IO_Jena.registerForModelRead(JSONLD.getName(),
                RDFReaderRIOT_RDFJSONLD.class);
    }

    // ---- Writer
    public static class RDFWriterRIOT_RDFJSONLD extends RDFWriterRIOT {
        public RDFWriterRIOT_RDFJSONLD() {
            super(JSONLD.getName());
        }
    }

    private static void initWriter() {
        RDFFormat format1 = new RDFFormat(JSONLD, RDFFormat.PRETTY);
        RDFFormat format2 = new RDFFormat(JSONLD, RDFFormat.FLAT);

        // Register the default format for the language.
        RDFWriterRegistry.register(JSONLD, format1);

        // For datasets
        WriterDatasetRIOTFactory wfactory = new WriterDatasetRIOTFactory() {
            @Override
            public WriterDatasetRIOT create(RDFFormat syntaxForm) {
                return new JsonLDWriter(syntaxForm);
            }
        };
        // Uses the same code for each form.
        RDFWriterRegistry.register(format1, wfactory);
        RDFWriterRegistry.register(format2, wfactory);

        // For graphs
        WriterGraphRIOTFactory wfactory2 = new WriterGraphRIOTFactory() {
            @Override
            public WriterGraphRIOT create(RDFFormat syntaxForm) {
                return RiotLib.adapter(new JsonLDWriter(syntaxForm));
            }
        };
        RDFWriterRegistry.register(format1, wfactory2);
        RDFWriterRegistry.register(format2, wfactory2);

        // Register for use with Model.write (old world)
        IO_Jena.registerForModelWrite(JSONLD.getName(),
                RDFWriterRIOT_RDFJSONLD.class);
    }

}
