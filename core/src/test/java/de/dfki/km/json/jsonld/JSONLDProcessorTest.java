package de.dfki.km.json.jsonld;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.dfki.km.json.JSONUtils;
import de.dfki.km.json.jsonld.JSONLDProcessor.Options;
import de.dfki.km.json.jsonld.impl.NQuadJSONLDSerializer;
import de.dfki.km.json.jsonld.impl.NQuadTripleCallback;

@RunWith(Parameterized.class)
public class JSONLDProcessorTest {

    private static final String TEST_DIR = "testfiles";

    @Parameters
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
                    System.out.println("Using manifest: " + dir + " " + name);
                    return true;
                }
                return false;
            }
        }));

        Collection<Object[]> rdata = new ArrayList<Object[]>();
        int count = 0;
        for (File in : manifestfiles) {
            System.out.println("Reading: " + in.getCanonicalPath());
            FileInputStream manifestfile = new FileInputStream(in);

            Map<String, Object> manifest = (Map<String, Object>) JSONUtils.fromInputStream(manifestfile);

            for (Map<String, Object> test : (List<Map<String, Object>>) manifest.get("sequence")) {
                List<String> testType = (List<String>) test.get("@type");
                if (// test.get("input").equals("normalize-0044-in.jsonld") && (
                testType.contains("jld:ExpandTest") || testType.contains("jld:CompactTest") //|| testType.contains("jld:NormalizeTest")
                || testType.contains("jld:FrameTest")// || testType.contains("jld:TriplesTest")
                || testType.contains("jld:SimplifyTest")
                || testType.contains("jld:ToRDFTest") || testType.contains("jld:FromRDFTest")
                ) {
                    System.out.println("Adding test: " + test.get("name"));
                    rdata.add(new Object[] { manifest.get("name"), test });
                } else {
                    // TODO: many disabled while implementation is incomplete
                    System.out.println("Skipping test: " + test.get("name"));
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

    public JSONLDProcessorTest(final String group, final Map<String, Object> test) {
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
        System.out.println("running test: " + test.get("input"));
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        String inputFile = (String) test.get("input");
        InputStream inputStream = cl.getResourceAsStream(TEST_DIR + "/" + inputFile);
        assertNotNull("unable to find input file: " + test.get("input"), inputStream);
        String inputType = inputFile.substring(inputFile.lastIndexOf(".") + 1);

        Object input = null;
        if (inputType.equals("jsonld")) {
        	input = JSONUtils.fromInputStream(inputStream);
        } else if (inputType.equals("nt") || inputType.equals("nq")) {
        	List<String> inputLines = new ArrayList<String>();
            BufferedReader buf = new BufferedReader(new InputStreamReader(inputStream));
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
        String expectFile = (String) test.get("expect");
        String sparqlFile = (String) test.get("sparql");
        if (expectFile != null) {
            InputStream expectStream = cl.getResourceAsStream(TEST_DIR + "/" + expectFile);
            assertNotNull("unable to find expect file: " + test.get("expect"), expectStream);

            String expectType = expectFile.substring(expectFile.lastIndexOf(".") + 1);
            if (expectType.equals("jsonld")) {
                expect = JSONUtils.fromInputStream(expectStream);
            } else if (expectType.equals("nt") || expectType.equals("nq")) {
                List<String> expectLines = new ArrayList<String>();
                BufferedReader buf = new BufferedReader(new InputStreamReader(expectStream));
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
        } else if (sparqlFile != null) {
            InputStream sparqlStream = cl.getResourceAsStream(TEST_DIR + "/" + sparqlFile);
            assertNotNull("unable to find expect file: " + sparqlFile, sparqlStream);
            BufferedReader buf = new BufferedReader(new InputStreamReader(sparqlStream));
            String buffer = null;
            while ((buffer = buf.readLine()) != null)
                sparql += buffer + "\n";
        } else {
            assertFalse("Nothing to expect from this test, thus nothing to test if it works", true);
        }

        Object result = null;

        List<String> testType = (List<String>) test.get("@type");
        Options options = new JSONLDProcessor.Options("http://json-ld.org/test-suite/tests/" + test.get("input"));
        if (testType.contains("jld:NormalizeTest")) {
            //result = new JSONLDProcessor().normalize(inputJson);
        } else if (testType.contains("jld:ExpandTest")) {
            result = JSONLD.expand(input, options);
        } else if (testType.contains("jld:CompactTest")) {
            InputStream contextStream = cl.getResourceAsStream(TEST_DIR + "/" + test.get("context"));
            Object contextJson = JSONUtils.fromInputStream(contextStream);
            result = JSONLD.compact(input, (Map<String, Object>) contextJson, options);
        } else if (testType.contains("jld:FrameTest")) {
            InputStream frameStream = cl.getResourceAsStream(TEST_DIR + "/" + test.get("frame"));
            Object frameJson = JSONUtils.fromInputStream(frameStream);
            result = JSONLD.frame(input, frameJson, options);
        } else if (testType.contains("jld:ToRDFTest")) {
            // TODO: many of the tests here fail simply because of an ordering issue
        	NQuadTripleCallback nqtc = new NQuadTripleCallback();
        	JSONLD.toRDF(input, options, nqtc);
        	
            result = nqtc.getResult();
        } else if (testType.contains("jld:FromRDFTest")) {
        	result = JSONLD.fromRDF(input, new NQuadJSONLDSerializer());
        } else if (testType.contains("jld:SimplifyTest")) {
        	result = JSONLD.simplify(input, options);
        } else {
            assertFalse("Unknown test type", true);
        }

        boolean testpassed = false;
        try {
            testpassed = JSONUtils.equals(expect, result);
            if (testpassed == false) {
                System.out.println("failed test: " + test.get("input"));
                jsonDiff("/", expect, result);
                System.out.println("{\"expected\": " + JSONUtils.toString(expect) + "\n,\"result\": " + JSONUtils.toString(result) + "}");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue("\nFailed test: " + this.group + " " + this.test.get("name") + " (" + test.get("input") + "," + test.get("expect") + ")\n" + "expected: "
                + JSONUtils.toString(expect) + "\nresult: " + JSONUtils.toString(result), testpassed);
    }

	private void jsonDiff(String parent, Object expect, Object result) {
	    if (expect == null) {
	        fail("Expected object was null");
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
			if (!expect.equals(result)) {
				System.out.println(parent + " results are not equal: \"" + expect + "\" != \"" + result + "\"");
			}
		}
	}
}
