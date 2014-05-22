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
import java.io.File;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

import org.apache.jena.riot.RDFDataMgr;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.jsonldjava.utils.TestUtils;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.lib.DatasetLib;
import com.hp.hpl.jena.sparql.sse.SSE;

/** tests : JSONLD->RDF ; JSONLD->RDF->JSONLD */
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

    private static boolean isIsomorphic(Dataset ds1, Dataset ds2) {
        return DatasetLib.isomorphic(ds1, ds2);
    }

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    private File testDir;

    @Before
    public void setUp() throws Exception {
        testDir = tempDir.newFolder("jenarioreadwritetest");
    }

    @Test
    public void read_ds01() throws Exception {
        datasetJ2R("graph1.jsonld", "graph1.ttl");
    }

    @Test
    public void read_ds02() throws Exception {
        datasetJ2R("dataset1.jsonld", "dataset1.trig");
    }

    @Test
    public void read_g01() throws Exception {
        graphJ2R("graph1.jsonld", "graph1.ttl");
    }

    @Test
    public void roundtrip_01() throws Exception {
        rtRJRg("graph1.ttl");
    }

    @Test
    public void roundtrip_02() throws Exception {
        rtRJRds("graph1.ttl");
    }

    @Test
    public void roundtrip_03() throws Exception {
        rtRJRds("dataset1.trig");
    }

    private void datasetJ2R(String inResource, String outResource) throws Exception {
        final Dataset ds1 = loadDatasetFromClasspathResource("/com/github/jsonldjava/jena/"
                + inResource);
        final Dataset ds2 = loadDatasetFromClasspathResource("/com/github/jsonldjava/jena/"
                + outResource);
        assertTrue("Input dataset " + inResource + " not isomorphic to output dataset"
                + outResource, isIsomorphic(ds1, ds2));
    }

    private void graphJ2R(String inResource, String outResource) throws Exception {
        final Model model1 = loadModelFromClasspathResource("/com/github/jsonldjava/jena/"
                + inResource);
        assertFalse("Failed to load input model from classpath: " + inResource, model1.isEmpty());
        final Model model2 = loadModelFromClasspathResource("/com/github/jsonldjava/jena/"
                + outResource);
        assertFalse("Failed to load output model from classpath: " + outResource, model2.isEmpty());
        assertTrue("Input graph " + inResource + " not isomorphic to output dataset" + outResource,
                model1.isIsomorphicWith(model2));
    }

    private Dataset loadDatasetFromClasspathResource(String resource) throws Exception {
        final InputStream url = this.getClass().getResourceAsStream(resource);
        assertNotNull("Could not find resource on classpath: " + resource, url);
        return RDFDataMgr.loadDataset(TestUtils.copyResourceToFile(testDir, resource));
    }

    private Model loadModelFromClasspathResource(String resource) throws Exception {
        final InputStream url = this.getClass().getResourceAsStream(resource);
        assertNotNull("Could not find resource on classpath: " + resource, url);
        return RDFDataMgr.loadModel(TestUtils.copyResourceToFile(testDir, resource));
    }
    
    private void rtRJRds(String resource) throws Exception {
        final Dataset ds1 = loadDatasetFromClasspathResource("/com/github/jsonldjava/jena/"
                + resource);

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

        assertTrue("Input dataset " + resource + " not isomorphic with roundtrip dataset",
                isIsomorphic(ds1, ds2));
        
        // Check namespaces in the parsed dataset match those in the original data
    	checkNamespaces(ds2.getDefaultModel(), ds1.getDefaultModel().getNsPrefixMap());
    	Iterator<String> graphNames = ds2.listNames();
    	while (graphNames.hasNext()) {
    		String gn = graphNames.next();
    		checkNamespaces(ds2.getNamedModel(gn), ds1.getNamedModel(gn).getNsPrefixMap());
    	}
    }
    
    private void checkNamespaces(Model m, Map<String, String> namespaces) {
    	if (namespaces == null) return;
    	
    	for (String prefix : namespaces.keySet()) {
    		Assert.assertEquals("Model does contain expected namespace " + prefix + ": <" + namespaces.get(prefix) + ">", namespaces.get(prefix), m.getNsPrefixURI(prefix));
    	}
    }

    private void rtRJRg(String filename) throws Exception {
        final Model model = loadModelFromClasspathResource("/com/github/jsonldjava/jena/"
                + filename);

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
        
        // Check namespaces in parsed graph match the original data
        checkNamespaces(model2, model.getNsPrefixMap());
    }
}
