package de.dfki.km.json.jsonld;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

@RunWith(Parameterized.class)
public class JSONLDProcessorTest {

    private static final String TEST_DIR = "testfiles";

    private class TestTripleCallback implements JSONLDTripleCallback {

        List<String> results = new ArrayList<String>();

        @Override
        public void triple(String s, String p, String o) {
            results.add("<" + s + "> <" + p + "> <" + o + "> .");
        }

        @Override
        public void triple(String s, String p, String value, String datatype, String language) {
            if (language != null) {
                results.add("<" + s + "> <" + p + "> \"" + value + "\"@" + language + " .");
            } else {
                if (datatype.equals(JSONLDConsts.XSD_STRING)) {
                    results.add("<" + s + "> <" + p + "> \"" + value + "\" .");
                } else {
                    results.add("<" + s + "> <" + p + "> \"" + value + "\"^^<" + datatype + "> .");
                }
            }
        }

        public List<String> getResults() {
            return results;
        }
    }

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
                testType.contains("jld:NormalizeTest") || testType.contains("jld:ExpandTest") || testType.contains("jld:CompactTest")
                        || testType.contains("jld:FrameTest") || testType.contains("jld:TriplesTest") || testType.contains("jld:SimplifyTest")
                // || testType.contains("jld:RDFTest")
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

    private static String join(Collection<String> list, String delim) {
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
    public void runTest() throws URISyntaxException, IOException {
        System.out.println("running test: " + test.get("input"));
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        InputStream inputStream = cl.getResourceAsStream(TEST_DIR + "/" + test.get("input"));
        assertNotNull("unable to find input file: " + test.get("input"), inputStream);

        Object inputJson = JSONUtils.fromInputStream(inputStream);
        Object expect = null;
        String sparql = null;
        String expectFile = (String) test.get("expect");
        String sparqlFile = (String) test.get("sparql");
        if (expectFile != null) {
            InputStream expectStream = cl.getResourceAsStream(TEST_DIR + "/" + test.get("expect"));
            assertNotNull("unable to find expect file: " + test.get("expect"), expectStream);

            String expectType = expectFile.substring(expectFile.lastIndexOf(".") + 1);
            if (expectType.equals("jsonld")) {
                expect = JSONUtils.fromInputStream(expectStream);
            } else if (expectType.equals("nt")) {
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
        if (testType.contains("jld:NormalizeTest")) {
            result = new JSONLDProcessor().normalize(inputJson);
        } else if (testType.contains("jld:ExpandTest")) {
            result = new JSONLDProcessor().expand(inputJson);
        } else if (testType.contains("jld:CompactTest")) {
            InputStream contextStream = cl.getResourceAsStream(TEST_DIR + "/" + test.get("context"));
            Object contextJson = JSONUtils.fromInputStream(contextStream);
            result = new JSONLDProcessor().compact(((Map<String, Object>) contextJson).get("@context"), inputJson);
        } else if (testType.contains("jld:FrameTest")) {
            InputStream frameStream = cl.getResourceAsStream(TEST_DIR + "/" + test.get("frame"));
            Object frameJson = JSONUtils.fromInputStream(frameStream);
            result = new JSONLDProcessor().frame(inputJson, frameJson);
        } else if (testType.contains("jld:TriplesTest")) {
            // TODO: many of the tests here fail simply because of an ordering issue

            TestTripleCallback ttc = new TestTripleCallback();
            new JSONLDProcessor().triples(inputJson, ttc);
            List<String> results = ttc.getResults();
            Collections.sort(results);
            result = join(results, "\n");
        } else if (testType.contains("jld:RDFTest")) {
            // TODO: implement this
            // sparql query is stored in String sparql
        } else if (testType.contains("jld:SimplifyTest")) {
            result = new JSONLDProcessor().simplify((Map) inputJson);
        } else {
            assertFalse("Unknown test type", true);
        }

        boolean testpassed = false;
        try {
            testpassed = compareResults(expect, result);
            if (testpassed == false) {
                System.out.println("failed test: " + test.get("input"));
                System.out.println("{\"expected\": " + JSONUtils.toString(expect) + "\n,\"result\": " + JSONUtils.toString(result) + "}");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue("\nFailed test: " + this.group + " " + this.test.get("name") + " (" + test.get("input") + "," + test.get("expect") + ")\n" + "expected: "
                + JSONUtils.toString(expect) + "\nresult: " + JSONUtils.toString(result), testpassed);
    }

    private boolean compareResults(Object v1, Object v2) {
        boolean rval = true;
        // TODO Auto-generated method stub
        if (v1 instanceof List && v2 instanceof List) {
            if (((List) v1).size() != ((List) v2).size()) {
                rval = false;
            } else {
                // TODO: should the order of things in the list matter?
                for (int i = 0; i < ((List<Object>) v1).size() && rval == true; i++) {
                    rval = compareResults(((List<Object>) v1).get(i), ((List<Object>) v2).get(i));
                }
            }
        } else if (v1 instanceof Number && v2 instanceof Number) {
            // TODO: this is VERY sketchy
            double n1 = ((Number) v1).doubleValue();
            double n2 = ((Number) v2).doubleValue();

            rval = n1 == n2;
        } else if (v1 instanceof String && v2 instanceof String) {
            rval = ((String) v1).equals((String) v2);
        } else if (v1 instanceof Map && v2 instanceof Map) {
            if (((Map) v1).size() != ((Map) v2).size()) {
                rval = false;
            } else {
                for (Object k1 : ((Map) v1).keySet()) {
                    rval = ((Map) v2).containsKey(k1) ? compareResults(((Map) v1).get(k1), ((Map) v2).get(k1)) : false;
                    if (rval != true) {
                        break;
                    }
                }
            }
        } else if (v1 instanceof Boolean && v2 instanceof Boolean) {
            rval = v1 == v2;
        } else if (v1 != null && v2 != null) {
            rval = v1.equals(v2);
        } else {
            rval = v1 == v2;
        }

        return rval;
    }
}
