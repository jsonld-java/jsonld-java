package com.github.jsonldjava.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.jsonldjava.core.JSONLD;
import com.github.jsonldjava.core.JSONLDProcessingError;
import com.github.jsonldjava.core.JSONLDUtils;
import com.github.jsonldjava.impl.NQuadRDFParser;
import com.github.jsonldjava.impl.NQuadTripleCallback;
import com.github.jsonldjava.impl.TurtleTripleCallback;
import com.github.jsonldjava.utils.JSONUtils;


@RunWith(Parameterized.class)
public class JSONLDProcessorTest {

    private static final String TEST_DIR = "json-ld.org";
    
    private static Map<String,Object> REPORT;
    private static List<Object> REPORT_GRAPH;
    private static String ASSERTOR = "http://tristan.github.com/foaf#me";
    
    @BeforeClass
    public static void prepareReportFrame() {
	    REPORT = new LinkedHashMap<String,Object>() {{
	    	// context
	    	put("@context", new LinkedHashMap<String,Object>() {{
	    		put("@vocab", "http://www.w3.org/ns/earl#");
	    		put("foaf", "http://xmlns.com/foaf/0.1/");
	    		put("earl", "http://www.w3.org/ns/earl#");
	    		put("doap", "http://usefulinc.com/ns/doap#");
	    		put("dc", "http://purl.org/dc/terms/");
	    		put("xsd", "http://www.w3.org/2001/XMLSchema#");
	    		put("foaf:homepage", new LinkedHashMap<String, Object>() {{ 
	    			put("@type", "@id");
	    		}});
	    		put("doap:homepage", new LinkedHashMap<String, Object>() {{ 
	    			put("@type", "@id");
	    		}});
	    	}});
	    	put("@graph", new ArrayList<Object>() {{
	    		// asserter
	    		add(new LinkedHashMap<String, Object>() {{
	    			put("@id", "http://tristan.github.com/foaf#me");
	    			put("@type", new ArrayList<Object>() {{
	    				add("foaf:Person");
	    				add("earl:Assertor");
	    			}});
	    			put("foaf:name", "Tristan King");
	    			put("foaf:title", "Implementor");
	    			put("foaf:homepage", "http://tristan.github.com");
	    		}});
	    		
	    		// project
	    		add(new LinkedHashMap<String, Object>() {{ 
	    			put("@id", "http://github.com/jsonld-java/jsonld-java");
	    			put("@type", new ArrayList<Object>() {{
	    				add("doap:Project");
	    				add("earl:TestSubject");
	    				add("earl:Software");
	    			}});
	    			put("doap:name", "JSONLD-Java");
	    			put("doap:homepage", "http://github.com/jsonld-java/jsonld-java");
	    			put("doap:description", new LinkedHashMap<String, Object>() {{ 
	    				put("@value", "An Implementation of the JSON-LD Specification for Java");
	    				put("@language", "en");
	    			}});
	    			put("doap:programming-language", "Java");
	    			put("doap:developer", new ArrayList<Object>() {{ 
	    				add(new LinkedHashMap<String, Object>() {{
	    					put("@id", "http://tristan.github.com/foaf#me");
	    				}});
	    				add(new LinkedHashMap<String, Object>() {{
	    					put("@id", "https://github.com/ansell/foaf#me");
	    					put("foaf:name", "Peter Ansell");
	    					put("foaf:title", "Contributor");
	    				}});
	    			}});
	    			put("doap:title", "JSONLD-Java");
	    			put("dc:date", new LinkedHashMap<String,Object>() {{ 
		    			put("@type", "xsd:date");
		    			put("@value", "2013-05-16");
		    		}});
	    			put("dc:creator", new LinkedHashMap<String, Object>() {{
						put("@id", "http://tristan.github.com/foaf#me");
					}});
	    		}});
	    	}});
	    }};
	    REPORT_GRAPH = (List<Object>) REPORT.get("@graph");
    }
    
    private static final String reportOutputFile = "reports/report";
    
    @AfterClass
    public static void writeReport() throws JsonGenerationException, JsonMappingException, IOException, JSONLDProcessingError {
    	
    	// Only write reports if "-Dreport.format=..." is set
    	String reportFormat = System.getProperty("report.format");
    	if (reportFormat != null) {
    		reportFormat = reportFormat.toLowerCase();
    	}
    	
    	if ("application/ld+json".equals(reportFormat) || "jsonld".equals(reportFormat) || "*".equals(reportFormat)) {
    		System.out.println("Generating JSON-LD Report");
    		JSONUtils.writePrettyPrint(new OutputStreamWriter(new FileOutputStream(reportOutputFile + ".jsonld")), REPORT);
    	}
    	if ("text/plain".equals(reportFormat) || "nquads".equals(reportFormat) || "nq".equals(reportFormat) || "nt".equals(reportFormat) || "ntriples".equals(reportFormat) || "*".equals(reportFormat)) {
    		System.out.println("Generating Nquads Report");
    		Options options = new Options("") {{
    			this.format = "application/nquads";
    		}};
	    	String rdf = (String)JSONLD.toRDF(REPORT, options);
	    	OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(reportOutputFile + ".nq"));
	    	writer.write(rdf);
	    	writer.close();
    	}
    	if ("text/turtle".equals(reportFormat) || "turtle".equals(reportFormat) || "ttl".equals(reportFormat) || "*".equals(reportFormat)) { // write turtle
    		System.out.println("Generating Turtle Report");
	    	Options options = new Options("") {{
	    		this.format = "text/turtle";
	    	}};
	    	String rdf = (String) JSONLD.toRDF(REPORT, new TurtleTripleCallback((Map<String, Object>) REPORT.get("@context")), options);
	    	OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(reportOutputFile + ".ttl"));
	    	writer.write((String)rdf);
	    	writer.close();
    	}
    }

    @Parameters(name="{0}{1}")
    public static Collection<Object[]> data() throws URISyntaxException, IOException {

        // TODO: look into getting the test data from github, which will help more
        // with keeping up to date with the spec.
        // perhaps use http://develop.github.com/p/object.html
        // to pull info from https://github.com/json-ld/json-ld.org/tree/master/test-suite/tests

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        File f = new File(cl.getResource(TEST_DIR).toURI());
        List<File> manifestfiles = Arrays.asList(f.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if (name.contains("manifest") && name.endsWith(".jsonld")) {
                    //System.out.println("Using manifest: " + dir + " " + name);
                    return true;
                }
                return false;
            }
        }));

        Collection<Object[]> rdata = new ArrayList<Object[]>();
        int count = 0;
        for (File in : manifestfiles) {
            //System.out.println("Reading: " + in.getCanonicalPath());
            FileInputStream manifestfile = new FileInputStream(in);

            Map<String, Object> manifest = (Map<String, Object>) JSONUtils.fromInputStream(manifestfile);

            for (Map<String, Object> test : (List<Map<String, Object>>) manifest.get("sequence")) {
                List<String> testType = (List<String>) test.get("@type");
                if (
                //"#t0001".equals(test.get("@id")) &&
                testType.contains("jld:ExpandTest") 
                || testType.contains("jld:CompactTest")
                || testType.contains("jld:FlattenTest")
                || testType.contains("jld:FrameTest")
                || testType.contains("jld:ToRDFTest")
                || testType.contains("jld:NormalizeTest")
                || testType.contains("jld:FromRDFTest")
                ) {
                    //System.out.println("Adding test: " + test.get("name"));
                    rdata.add(new Object[] { (String)manifest.get("baseIri") + (String)manifest.get("name") + "-manifest.jsonld", test.get("@id"), test });
                } else {
                    // TODO: many disabled while implementation is incomplete
                    //System.out.println("Skipping test: " + test.get("name"));
                }

                // Uncomment this to print a list of all the tests (may make debugging easier
                // System.out.println("Added Test[" + count++ + "]: " + testgroup.get("group") + " " + test.get("name") + "..."
                // + " (" + test.get("input") + "," + test.get("expect") + ")"
                // );
            }
        }
        return rdata;
    }

    private String group;
    private Map<String, Object> test;

    public JSONLDProcessorTest(final String group, final String id, final Map<String, Object> test) {
        this.group = group;
        this.test = test;
    }

    public static String join(Collection<String> list, String delim) {
        StringBuilder builder = new StringBuilder();
        Iterator<String> iter = list.iterator();
        while (iter.hasNext()) {
            builder.append(iter.next());
            if (!iter.hasNext()) {
                break;
            }
            builder.append(delim);
        }
        return builder.toString();
    }

    @Test
    public void runTest() throws URISyntaxException, IOException, JSONLDProcessingError {
        //System.out.println("running test: " + group + test.get("@id") + " :: " + test.get("name"));
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        List<String> testType = (List<String>) test.get("@type");
        
        String inputFile = (String) test.get("input");
        InputStream inputStream = cl.getResourceAsStream(TEST_DIR + "/" + inputFile);
        assertNotNull("unable to find input file: " + test.get("input"), inputStream);
        String inputType = inputFile.substring(inputFile.lastIndexOf(".") + 1);

        Object input = null;
        if (inputType.equals("jsonld")) {
        	input = JSONUtils.fromInputStream(inputStream);
        } else if (inputType.equals("nt") || inputType.equals("nq")) {
        	List<String> inputLines = new ArrayList<String>();
            BufferedReader buf = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String line;
            while ((line = buf.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0 || line.charAt(0) == '#') {
                    continue;
                }
                inputLines.add(line);
            }
            //Collections.sort(inputLines);
            input = join(inputLines, "\n");
        }
        Object expect = null;
        String sparql = null;
        Boolean failure_expected = false;
        String expectFile = (String) test.get("expect");
        String sparqlFile = (String) test.get("sparql");
        if (expectFile != null) {
            InputStream expectStream = cl.getResourceAsStream(TEST_DIR + "/" + expectFile);
            if (expectStream == null && testType.contains("jld:NegativeEvaluationTest")) {
            	// in the case of negative evaluation tests the expect field can be a description of what should happen
            	expect = expectFile;
            	failure_expected = true;
            } else if (expectStream == null) {
            	assertFalse("Unable to find expect file: " + expectFile, true);
            } else {
	            String expectType = expectFile.substring(expectFile.lastIndexOf(".") + 1);
	            if (expectType.equals("jsonld")) {
	                expect = JSONUtils.fromInputStream(expectStream);
	            } else if (expectType.equals("nt") || expectType.equals("nq")) {
	                List<String> expectLines = new ArrayList<String>();
	                BufferedReader buf = new BufferedReader(new InputStreamReader(expectStream, "UTF-8"));
	                String line;
	                while ((line = buf.readLine()) != null) {
	                    line = line.trim();
	                    if (line.length() == 0 || line.charAt(0) == '#') {
	                        continue;
	                    }
	                    expectLines.add(line);
	                }
	                Collections.sort(expectLines);
	                expect = join(expectLines, "\n");
	            } else {
	                expect = "";
	                assertFalse("Unknown expect type: " + expectType, true);
	            }
            }
        } else if (sparqlFile != null) {
            InputStream sparqlStream = cl.getResourceAsStream(TEST_DIR + "/" + sparqlFile);
            assertNotNull("unable to find expect file: " + sparqlFile, sparqlStream);
            BufferedReader buf = new BufferedReader(new InputStreamReader(sparqlStream, "UTF-8"));
            String buffer = null;
            while ((buffer = buf.readLine()) != null)
                sparql += buffer + "\n";
        } else if (testType.contains("jld:NegativeEvaluationTest")) {
        	// TODO: this is hacky, but works for the limited number of negative evaluation tests that are currently present
        	failure_expected = true;
        } else {
            assertFalse("Nothing to expect from this test, thus nothing to test if it works", true);
        }

        Object result = null;

        Options options = new Options("http://json-ld.org/test-suite/tests/" + test.get("input"));
        try {
	        if (testType.contains("jld:NormalizeTest")) {
	        	options.format = "application/nquads";
	            result = JSONLD.normalize(input, options);
	            result = ((String)result).trim();
	        } else if (testType.contains("jld:ExpandTest")) {
	        	result = JSONLD.expand(input, options);
	        } else if (testType.contains("jld:CompactTest")) {
	            InputStream contextStream = cl.getResourceAsStream(TEST_DIR + "/" + test.get("context"));
	            Object contextJson = JSONUtils.fromInputStream(contextStream);
	            result = JSONLD.compact(input, (Map<String, Object>) contextJson, options);
	        } else if (testType.contains("jld:FlattenTest")) {
	        	if (test.containsKey("context")) {
	        		InputStream contextStream = cl.getResourceAsStream(TEST_DIR + "/" + test.get("context"));
		            Object contextJson = JSONUtils.fromInputStream(contextStream);
		            result = JSONLD.flatten(input, contextJson, options);
	        	} else {
	        		result = JSONLD.flatten(input, options);
	        	}
	        } else if (testType.contains("jld:FrameTest")) {
	            InputStream frameStream = cl.getResourceAsStream(TEST_DIR + "/" + test.get("frame"));
	            Map<String,Object> frameJson = (Map<String, Object>) JSONUtils.fromInputStream(frameStream);
	            result = JSONLD.frame(input, frameJson, options);
	        } else if (testType.contains("jld:ToRDFTest")) {
	        	options.format = "application/nquads";
	        	result = JSONLD.toRDF(input, options);
	        	result = ((String)result).trim();
	        } else if (testType.contains("jld:FromRDFTest")) {
	        	//result = JSONLD.fromRDF(input, new NQuadJSONLDSerializer());
	        	result = JSONLD.fromRDF(input, options);
	        } else if (testType.contains("jld:SimplifyTest")) {
	        	result = JSONLD.simplify(input, options);
	        } else {
	            assertFalse("Unknown test type", true);
	        }
        } catch (JSONLDProcessingError e) {
    		result = e;
    	}

        Boolean testpassed = false;
        try {
        	// TODO: for tests that are supposed to fail, a more detailed check that it failed in the right way is needed
            testpassed = JSONUtils.equals(expect, result) || failure_expected;
            if (testpassed == false) {
                //System.out.println("failed test!!! details:");
                //jsonDiff("/", expect, result);
                //Map<String,Object> pp = new LinkedHashMap<String, Object>();
                //pp.put("expected", expect);
                //pp.put("result", result);
                //System.out.println("{\"expected\": " + JSONUtils.toString(expect) + "\n,\"result\": " + JSONUtils.toString(result) + "}");
                //JSONUtils.writePrettyPrint(new OutputStreamWriter(System.out), pp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // write details to report
        final String manifest = this.group;
        final String id = (String) this.test.get("@id");
        Date d = new Date();
        String dateTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(d);
        String zone = new SimpleDateFormat("Z").format(d);
        zone = zone.substring(0, 3) + ":" + zone.substring(3);
        final String dateTimeZone = dateTime + zone;
        final Boolean passed = testpassed;
        REPORT_GRAPH.add(new LinkedHashMap<String, Object>() {{
        	put("@type", "earl:Assertion");
        	put("earl:assertedBy", new LinkedHashMap<String, Object>() {{
        		put("@id", ASSERTOR);
        	}});
        	put("earl:subject", new LinkedHashMap<String, Object>() {{ 
        		put("@id", "http://github.com/jsonld-java/jsonld-java");
        	}});
        	put("earl:test", new LinkedHashMap<String, Object>() {{ 
	    			put("@id", manifest + id);
        	}});
        	put("earl:result", new LinkedHashMap<String, Object>() {{
        		put("@type", "earl:TestResult");
	        	put("earl:outcome", passed ? "earl:passed" : "earl:failed");
	        	put("dc:date", new LinkedHashMap<String, Object>() {{
	        		put("@value", dateTimeZone);
	        		put("@type", "xsd:dateTime");
	        	}});
        	}});
        	// for error expand the correct error is thrown, but the test suite doesn't yet automatically figure that out.
        	put("earl:mode", "http://json-ld.org/test-suite/tests/error-expand-manifest.jsonld".equals(manifest) ? "earl:semiAuto" : "earl:automatic");
        }});
        
        assertTrue("\nFailed test: " + group + test.get("@id") + " " + test.get("name") + " (" + test.get("input") + "," + test.get("expect") + ")\n" + "expected: "
                + JSONUtils.toString(expect) + "\nresult: " + JSONUtils.toString(result), testpassed);
    }

    /**
     * compares the expected and resulting objects and prints out differences of the two
     * 
     * @param parent
     * @param expect
     * @param result
     */
	private void jsonDiff(String parent, Object expect, Object result) {
	    if (expect == null) {
	    	if (result != null) {
	    		System.out.println(parent + " expected null, got: " + result);
	    	}
	        //fail("Expected object was null");
	    }
	    else if (expect instanceof Map && result instanceof Map) {
			Map<String,Object> e = (Map<String,Object>)expect;
			Map<String,Object> r = (Map<String,Object>)JSONLDUtils.clone(result);
			for (String k: e.keySet()) {
				if (r.containsKey(k)){
					jsonDiff(parent + "/" + k, e.get(k), r.remove(k));
				} else {
					System.out.println(parent + " result missing key: " + k);
				}
			}
			for (String k: r.keySet()) {
				System.out.println(parent + " result has extra key: " + k);
			}
		}
		// List diffs are hard if we aren't strict with array ordering!
		else if (expect instanceof List && result instanceof List) {
			List<Object> e = (List<Object>)expect;
			List<Object> r = (List<Object>)JSONLDUtils.clone(result);
			if (e.size() != r.size()) {
				System.out.println(parent + " results are not the same size");
			}
			int i = 0;
			for (Object o: e) {
				int j = r.indexOf(o);
				if (j < 0) {
					System.out.println(parent + " result missing value at index: " + i);
				} else {
					r.remove(j);
				}
				i++;
			}
			for (Object o: r) {
				System.out.println(parent + " result has extra items");
			}
		}
		else {
			if (expect != null && !expect.equals(result)) {
				System.out.println(parent + " results are not equal: \"" + expect + "\" != \"" + result + "\"");
			}
		}
	}
}
