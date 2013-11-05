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

import static com.github.jsonldjava.jena.JenaJSONLD.JSONLD;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;

import org.apache.jena.riot.RDFDataMgr;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.lib.DatasetLib;
import com.hp.hpl.jena.sparql.sse.SSE;

/** tests : JSONLD->RDF ; JSONLD->RDF->JSONLD */
@Ignore("FIXME")
public class JenaRiotReadWriteTest {

    @BeforeClass
    public static void init() {
        /*
         * Disable this to test that static { } in JenaJSONLD forces init() by
         * accessing the field JenaJSONLD.JSONLD.
         * 
         * It is enabled by default to enable selective test running.
         */
        JenaJSONLD.init();
    }

    @Test
    public void read_g01() {
        graphJ2R("graph1.jsonld", "graph1.ttl");
    }

    @Test
    public void read_ds01() {
        datasetJ2R("graph1.jsonld", "graph1.ttl");
    }

    @Test
    public void read_ds02() {
        datasetJ2R("dataset1.jsonld", "dataset1.trig");
    }

    private void graphJ2R(String inResource, String outResource) {
        final Model model1 = loadModelFromClasspathResource(inResource);
        assertFalse("Failed to load input model from classpath: " + inResource, model1.isEmpty());
        final Model model2 = loadModelFromClasspathResource(outResource);
        assertFalse("Failed to load output model from classpath: " + outResource, model2.isEmpty());
        assertTrue("Input graph " + inResource + " not isomorphic to output dataset" + outResource,
                model1.isIsomorphicWith(model2));
    }

    private void datasetJ2R(String inResource, String outResource) {
        final Dataset ds1 = loadDatasetFromClasspathResource(inResource);
        final Dataset ds2 = loadDatasetFromClasspathResource(outResource);
        assertTrue("Input dataset " + inResource + " not isomorphic to output dataset"
                + outResource, isIsomorphic(ds1, ds2));
    }

    @Test
    public void roundtrip_01() {
        rtRJRg("graph1.ttl");
    }

    @Test
    public void roundtrip_02() {
        rtRJRds("graph1.ttl");
    }

    @Test
    public void roundtrip_03() {
        rtRJRds("dataset1.trig");
    }

    public void rtRJRg(String filename) {
        final Model model = loadModelFromClasspathResource(filename);

        // Write a JSON-LD
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        RDFDataMgr.write(out, model, JSONLD);
        final ByteArrayInputStream r = new ByteArrayInputStream(out.toByteArray());

        // Read as JSON-LD
        final Model model2 = ModelFactory.createDefaultModel();
        RDFDataMgr.read(model2, r, null, JSONLD);

        assertFalse("JSON-LD model was empty", model2.isEmpty());

        // Compare
        if (!model.isIsomorphicWith(model2)) {
            System.out.println("## ---- DIFFERENT");
        }
    }

    private Model loadModelFromClasspathResource(String resource) {
        final URL url = getResource(resource);
        return RDFDataMgr.loadModel(url.toExternalForm());
    }

    private URL getResource(String resource) {
        final URL url = getClass().getResource(resource);
        assertNotNull("Could not find resource on classpath: " + resource, url);
        return url;
    }

    public void rtRJRds(String resource) {
        final Dataset ds1 = loadDatasetFromClasspathResource(resource);

        // Write a JSON-LD
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        RDFDataMgr.write(out, ds1, JSONLD);
        final ByteArrayInputStream r = new ByteArrayInputStream(out.toByteArray());

        // Read as JSON-LD
        final Dataset ds2 = DatasetFactory.createMem();
        RDFDataMgr.read(ds2, r, null, JSONLD);

        if (!isIsomorphic(ds1, ds2)) {
            SSE.write(ds1);
            SSE.write(ds2);
        }

        assertTrue(isIsomorphic(ds1, ds2));
    }

    private Dataset loadDatasetFromClasspathResource(String resource) {
        final URL url = getResource(resource);
        return RDFDataMgr.loadDataset(url.toExternalForm());
    }

    private static boolean isIsomorphic(Dataset ds1, Dataset ds2) {
        return DatasetLib.isomorphic(ds1, ds2);
    }
}
