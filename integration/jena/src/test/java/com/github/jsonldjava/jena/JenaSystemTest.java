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

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParserRegistry;
import org.apache.jena.riot.RDFWriterRegistry;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

// Test system integration / registration
public class JenaSystemTest extends Assert {

    @BeforeClass
    public static void init() {
        JenaJSONLD.init();
    }

    private static RDFFormat jsonldFmt1 = new RDFFormat(JenaJSONLD.JSONLD,
            RDFFormat.PRETTY);
    private static RDFFormat jsonldFmt2 = new RDFFormat(JenaJSONLD.JSONLD,
            RDFFormat.FLAT);

    @Test
    public void jenaSystem_basic_1() {
        assertEquals("name", "JSON-LD", JenaJSONLD.JSONLD.getName());
        assertEquals("content-type", "application/ld+json", JenaJSONLD.JSONLD
                .getContentType().getContentType());
    }

    @Test
    public void jenaSystem_read_1() {
        assertTrue(RDFLanguages.isRegistered(JenaJSONLD.JSONLD));
        assertTrue(RDFLanguages.isTriples(JenaJSONLD.JSONLD));
        assertTrue(RDFLanguages.isQuads(JenaJSONLD.JSONLD));
    }

    @Test
    public void jenaSystem_read_2() {
        assertNotNull(RDFParserRegistry.getFactory(JenaJSONLD.JSONLD));
    }

    @Test
    public void jenaSystem_write_1() {
        assertTrue(RDFWriterRegistry.contains(JenaJSONLD.JSONLD));
    }

    @Test
    public void jenaSystem_write_2() {
        assertNotNull(RDFWriterRegistry
                .getWriterGraphFactory(JenaJSONLD.JSONLD));
        assertNotNull(RDFWriterRegistry
                .getWriterDatasetFactory(JenaJSONLD.JSONLD));
        assertNotNull(RDFWriterRegistry.defaultSerialization(JenaJSONLD.JSONLD));
    }

    @Test
    public void jenaSystem_write_3() {

        assertEquals(jsonldFmt1,
                RDFWriterRegistry.defaultSerialization(JenaJSONLD.JSONLD));

        assertNotNull(RDFWriterRegistry.getWriterGraphFactory(jsonldFmt1));
        assertNotNull(RDFWriterRegistry.getWriterGraphFactory(jsonldFmt2));

        assertTrue(RDFWriterRegistry.registeredGraphFormats().contains(
                jsonldFmt1));
        assertTrue(RDFWriterRegistry.registeredGraphFormats().contains(
                jsonldFmt2));

        assertNotNull(RDFWriterRegistry.getWriterDatasetFactory(jsonldFmt1));
        assertNotNull(RDFWriterRegistry.getWriterDatasetFactory(jsonldFmt2));

        assertTrue(RDFWriterRegistry.registeredDatasetFormats().contains(
                jsonldFmt1));
        assertTrue(RDFWriterRegistry.registeredDatasetFormats().contains(
                jsonldFmt2));
    }

    @Test
    public void jenaSystem_write_4() {
        assertNotNull(RDFDataMgr.createGraphWriter(jsonldFmt1));
        assertNotNull(RDFDataMgr.createGraphWriter(jsonldFmt2));
        assertNotNull(RDFDataMgr.createDatasetWriter(jsonldFmt1));
        assertNotNull(RDFDataMgr.createDatasetWriter(jsonldFmt2));
    }
}
