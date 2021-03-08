package com.github.jsonldjava.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assume;
import org.junit.BeforeClass;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.jsonldjava.core.DocumentLoader;
import com.github.jsonldjava.core.JsonLdConsts;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.core.JsonLdUtils;
import com.github.jsonldjava.core.RemoteDocument;
import com.github.jsonldjava.utils.JsonUtils;
import com.github.jsonldjava.utils.Obj;

public class SuiteUtils {

    // private static final String TEST_DIR = "json-ld-api-tests";
    // option: run the json-ld-framing test suite:
    // (https://github.com/w3c/json-ld-framing/blob/master/tests/README.md)
    // private static final String TEST_DIR = "json-ld-framing-tests";
    // option: run the old 1.0 test suite:
    // (https://github.com/json-ld/json-ld.org/tree/master/test-suite/tests)
    // private static final String TEST_DIR = "json-ld-1.0-tests";

    private static String ASSERTOR = "http://tristan.github.com/foaf#me";
    
    public static LinkedHashMap<String, Object> generateReport() {
        return new LinkedHashMap<String, Object>() {
            {
                // context
                put("@context", new LinkedHashMap<String, Object>() {
                    {
                        put("@vocab", "http://www.w3.org/ns/earl#");
                        put("foaf", "http://xmlns.com/foaf/0.1/");
                        put("earl", "http://www.w3.org/ns/earl#");
                        put("doap", "http://usefulinc.com/ns/doap#");
                        put("dc", "http://purl.org/dc/terms/");
                        put("xsd", "http://www.w3.org/2001/XMLSchema#");
                        put("foaf:homepage", new LinkedHashMap<String, Object>() {
                            {
                                put("@type", "@id");
                            }
                        });
                        put("doap:homepage", new LinkedHashMap<String, Object>() {
                            {
                                put("@type", "@id");
                            }
                        });
                    }
                });
                put("@graph", new ArrayList<Object>() {
                    {
                        // asserter
                        add(new LinkedHashMap<String, Object>() {
                            {
                                put("@id", "http://tristan.github.com/foaf#me");
                                put("@type", new ArrayList<Object>() {
                                    {
                                        add("foaf:Person");
                                        add("earl:Assertor");
                                    }
                                });
                                put("foaf:name", "Tristan King");
                                put("foaf:title", "Implementor");
                                put("foaf:homepage", "http://tristan.github.com");
                            }
                        });

                        // project
                        add(new LinkedHashMap<String, Object>() {
                            {
                                put("@id", "https://github.com/jsonld-java/jsonld-java");
                                put("@type", new ArrayList<Object>() {
                                    {
                                        add("doap:Project");
                                        add("earl:TestSubject");
                                        add("earl:Software");
                                    }
                                });
                                put("doap:name", "JSONLD-Java");
                                put("doap:homepage", "https://github.com/jsonld-java/jsonld-java");
                                put("doap:description", new LinkedHashMap<String, Object>() {
                                    {
                                        put("@value",
                                                "An Implementation of the JSON-LD Specification for Java");
                                        put("@language", "en");
                                    }
                                });
                                put("doap:programming-language", "Java");
                                put("doap:developer", new ArrayList<Object>() {
                                    {
                                        add(new LinkedHashMap<String, Object>() {
                                            {
                                                put("@id", "http://tristan.github.com/foaf#me");
                                            }
                                        });
                                        add(new LinkedHashMap<String, Object>() {
                                            {
                                                put("@id", "https://github.com/ansell/foaf#me");
                                                put("foaf:name", "Peter Ansell");
                                                put("foaf:title", "Contributor");
                                            }
                                        });
                                    }
                                });
                                put("doap:title", "JSONLD-Java");
                                put("dc:date", new LinkedHashMap<String, Object>() {
                                    {
                                        put("@type", "xsd:date");
                                        put("@value", "2013-05-16");
                                    }
                                });
                                put("dc:creator", new LinkedHashMap<String, Object>() {
                                    {
                                        put("@id", "http://tristan.github.com/foaf#me");
                                    }
                                });
                            }
                        });
                    }
                });
            }
        };
    }

    public static void writeReport(Object report, String reportOutputFile)
            throws JsonGenerationException, JsonMappingException, IOException, JsonLdError {

        // Only write reports if "-Dreport.format=..." is set
        String reportFormat = System.getProperty("report.format");
        if (reportFormat != null) {
            reportFormat = reportFormat.toLowerCase();
            if ("application/ld+json".equals(reportFormat) || "jsonld".equals(reportFormat)
                    || "*".equals(reportFormat)) {
                System.out.println("Generating JSON-LD Report");
                JsonUtils.writePrettyPrint(
                        new OutputStreamWriter(new FileOutputStream(reportOutputFile + ".jsonld"),
                                StandardCharsets.UTF_8),
                        report);
            }
        }
        if (skipFileWriter != null) {
            skipFileWriter.close();
        }
    }

    public static Collection<Object[]> getData(String dir) throws URISyntaxException, FileNotFoundException, IOException {
        // TODO: look into getting the test data from github, which will help
        // more
        // with keeping up to date with the spec.
        // perhaps use http://develop.github.com/p/object.html
        // to pull info from
        // https://github.com/json-ld/json-ld.org/tree/master/test-suite/tests

        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final File testDir = new File(cl.getResource(dir).toURI());

        final List<File> manifestfiles = loadManifestFiles(testDir);

        final Collection<Object[]> rdata = new ArrayList<Object[]>();
        for (final File in : manifestfiles) {
            // System.out.println("Reading: " + in.getCanonicalPath());
            final FileInputStream manifestfile = new FileInputStream(in);

            final Map<String, Object> manifest = (Map<String, Object>) JsonUtils
                    .fromInputStream(manifestfile);

            for (final Map<String, Object> test : (List<Map<String, Object>>) manifest
                    .get("sequence")) {
                final List<String> testType = (List<String>) test.get("@type");
                if (testType.contains("jld:ExpandTest") || testType.contains("jld:CompactTest")
                        || testType.contains("jld:FlattenTest")
                        || testType.contains("jld:FrameTest")
                        || testType.contains("jld:FromRDFTest")
                        || testType.contains("jld:ToRDFTest")
                        || testType.contains("jld:NormalizeTest")) {
                    // System.out.println("Adding test: " + test.get("name"));
                    final String id = (String) manifest.get("baseIri") + in.getName();
                    rdata.add(new Object[] {
                            // e.g. http://json-ld.org/test-suite/tests/flatten-manifest.jsonld#t0003
                            // vs https://w3c.github.io/json-ld-api/tests/flatten-manifest#t0003
                            dir.contains("json-ld-1.0") ? id : id.replace(".jsonld", ""),
                            test.get("@id"),
                            test });
                } else {
                    // TODO: many disabled while implementation is incomplete
                    System.out.println("Skipping test: " + test.get("name"));
                }
            }
        }
        return rdata;
    }

    private static List<File> loadManifestFiles(final File testDir) {
        List<File> manifestfiles = new ArrayList<File>();
        if (!testDir.getName().equals("json-ld-1.0-tests")) {
            final File mainManifestFile = new File(testDir, "manifest.jsonld");
            Map<String, Object> mainManifest;
            try {
                mainManifest = (Map<String, Object>) JsonUtils.fromInputStream(new FileInputStream(mainManifestFile));
                final List<String> manifestFileNames = (List<String>) mainManifest.get("sequence");
                for (final String manifestFileName : manifestFileNames) {
                    System.out.println("Using manifest: " + testDir + " " + manifestFileName);
                    manifestfiles.add(new File(testDir, manifestFileName));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            manifestfiles = Arrays.asList(testDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    if (name.contains("manifest") && name.endsWith(".jsonld") && !name.equals("manifest.jsonld")) {
                        System.out.println("Using manifest: " + dir + " " + name);
                        // Remote-doc tests are not currently supported
                        if (name.contains("remote-doc")) {
                            return false;
                        }
                        return true;
                    }
                    return false;
                }
            }));
        }
        return manifestfiles;
    }

    static class TestDocumentLoader extends DocumentLoader {

        private final String base;
        private String dir;

        public TestDocumentLoader(String base, String dir) {
            this.dir = dir;
            this.base = base;
        }

        @Override
        public RemoteDocument loadDocument(String url) throws JsonLdError {
            if (url == null) {
                throw new JsonLdError(JsonLdError.Error.LOADING_REMOTE_CONTEXT_FAILED,
                        "URL was null");
            }
            if (url.contains(":")) {
                // check if the url is relative to the test base
                if (url.startsWith(this.base)) {
                    final String classpath = url.substring(this.base.length());
                    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    final InputStream inputStream = cl
                            .getResourceAsStream(dir + "/" + classpath);
                    try {
                        return new RemoteDocument(url, JsonUtils.fromInputStream(inputStream));
                    } catch (final IOException e) {
                        throw new JsonLdError(JsonLdError.Error.LOADING_DOCUMENT_FAILED, e);
                    }
                }
            }
            // we can't load this remote document from the test suite
            throw new JsonLdError(JsonLdError.Error.NOT_IMPLEMENTED,
                    "URL scheme was not recognised: " + url);
        }

        public void setRedirectTo(String string) {
            // TODO Auto-generated method stub

        }

        public void setHttpStatus(Integer integer) {
            // TODO Auto-generated method stub

        }

        public void setContentType(String string) {
            // TODO Auto-generated method stub

        }

        public void addHttpLink(String nextLink) {
            // TODO Auto-generated method stub

        }
    }

    public static void run(String dir, String group, String testId, Map<String, Object> test, List<Object> reportGraph) throws URISyntaxException, IOException, JsonLdError {
        // System.out.println("running test: " + group + test.get("@id") +
        // " :: " + test.get("name"));
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();

        final List<String> testType = (List<String>) test.get("@type");

        final String inputFile = (String) test.get("input");
        final InputStream inputStream = cl.getResourceAsStream(dir + "/" + inputFile);
        String skipId = skipId(group, testId);
        if(inputStream == null) {
            skipFileWriter.append(skipId+"\n");
        }
        assertNotNull("unable to find input file: " + test.get("input"), inputStream);
        final String inputType = inputFile.substring(inputFile.lastIndexOf(".") + 1);

        Object input = null;
        if (inputType.equals("jsonld")) {
            input = JsonUtils.fromInputStream(inputStream);
        } else if (inputType.equals("nt") || inputType.equals("nq")) {
            final List<String> inputLines = new ArrayList<String>();
            final BufferedReader buf = new BufferedReader(
                    new InputStreamReader(inputStream, "UTF-8"));
            String line;
            while ((line = buf.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0 || line.charAt(0) == '#') {
                    continue;
                }
                inputLines.add(line);
            }
            // Collections.sort(inputLines);
            input = TestUtils.join(inputLines, "\n");
        }
        Object expect = null;
        String sparql = null;
        Boolean failure_expected = false;
        final String expectFile = (String) test.get("expect");
        final String sparqlFile = (String) test.get("sparql");
        if (expectFile != null) {
            final InputStream expectStream = cl.getResourceAsStream(dir + "/" + expectFile);
            if (expectStream == null && testType.contains("jld:NegativeEvaluationTest")) {
                // in the case of negative evaluation tests the expect field can
                // be a description of what should happen
                expect = expectFile;
                failure_expected = true;
            } else if (expectStream == null) {
                assertFalse("Unable to find expect file: " + expectFile, true);
            } else {
                final String expectType = expectFile.substring(expectFile.lastIndexOf(".") + 1);
                if (expectType.equals("jsonld")) {
                    expect = JsonUtils.fromInputStream(expectStream);
                } else if (expectType.equals("nt") || expectType.equals("nq")) {
                    final List<String> expectLines = new ArrayList<String>();
                    final BufferedReader buf = new BufferedReader(
                            new InputStreamReader(expectStream, "UTF-8"));
                    String line;
                    while ((line = buf.readLine()) != null) {
                        line = line.trim();
                        if (line.length() == 0 || line.charAt(0) == '#') {
                            continue;
                        }
                        expectLines.add(line);
                    }
                    Collections.sort(expectLines);
                    expect = TestUtils.join(expectLines, "\n");
                } else {
                    expect = "";
                    assertFalse("Unknown expect type: " + expectType, true);
                }
            }
        } else if (sparqlFile != null) {
            final InputStream sparqlStream = cl.getResourceAsStream(dir + "/" + sparqlFile);
            assertNotNull("unable to find expect file: " + sparqlFile, sparqlStream);
            final BufferedReader buf = new BufferedReader(
                    new InputStreamReader(sparqlStream, "UTF-8"));
            String buffer = null;
            while ((buffer = buf.readLine()) != null) {
                sparql += buffer + "\n";
            }
        } else if (testType.contains("jld:NegativeEvaluationTest")) {
            failure_expected = true;
            if(test.containsKey("expectErrorCode")) {
                expect = test.get("expectErrorCode");
            }
        }

        Object result = null;

        // OPTIONS SETUP
        final String base = group.substring(0, group.lastIndexOf('/') + 1);
        final JsonLdOptions options = new JsonLdOptions(base + test.get("input"));
        final TestDocumentLoader testLoader = new TestDocumentLoader(base, dir);
        options.setDocumentLoader(testLoader);
        if (test.containsKey("option")) {
            final Map<String, Object> test_opts = (Map<String, Object>) test.get("option");
            if (test_opts.containsKey("base")) {
                options.setBase((String) test_opts.get("base"));
            }
            if (test_opts.containsKey("expandContext")) {
                final InputStream contextStream = cl
                        .getResourceAsStream(dir + "/" + test_opts.get("expandContext"));
                options.setExpandContext(JsonUtils.fromInputStream(contextStream));
            }
            if (test_opts.containsKey("compactArrays")) {
                options.setCompactArrays((Boolean) test_opts.get("compactArrays"));
            }
            if (test_opts.containsKey("useNativeTypes")) {
                options.setUseNativeTypes((Boolean) test_opts.get("useNativeTypes"));
            }
            if (test_opts.containsKey("useRdfType")) {
                options.setUseRdfType((Boolean) test_opts.get("useRdfType"));
            }
            if (test_opts.containsKey("processingMode")) {
                options.setProcessingMode((String) test_opts.get("processingMode"));
            }
            if (test_opts.containsKey("omitGraph")) {
                options.setOmitGraph((Boolean) test_opts.get("omitGraph"));
            }
            if (test_opts.containsKey("produceGeneralizedRdf")) {
                options.setProduceGeneralizedRdf((Boolean) test_opts.get("produceGeneralizedRdf"));
            }
            if (test_opts.containsKey("redirectTo")) {
                testLoader.setRedirectTo((String) test_opts.get("redirectTo"));
            }
            if (test_opts.containsKey("httpStatus")) {
                testLoader.setHttpStatus((Integer) test_opts.get("httpStatus"));
            }
            if (test_opts.containsKey("contentType")) {
                testLoader.setContentType((String) test_opts.get("contentType"));
            }
            if (test_opts.containsKey("httpLink")) {
                if (test_opts.get("httpLink") instanceof List) {
                    for (final String nextLink : (List<String>) test_opts.get("httpLink")) {
                        testLoader.addHttpLink(nextLink);
                    }
                } else {
                    testLoader.addHttpLink((String) test_opts.get("httpLink"));
                }
            }
        }

        // RUN TEST
        try {
            if (testType.contains("jld:ExpandTest")) {
                result = JsonLdProcessor.expand(input, options);
            } else if (testType.contains("jld:CompactTest")) {
                final InputStream contextStream = cl
                        .getResourceAsStream(dir + "/" + test.get("context"));
                final Object contextJson = JsonUtils.fromInputStream(contextStream);
                result = JsonLdProcessor.compact(input, contextJson, options);
            } else if (testType.contains("jld:FlattenTest")) {
                if (test.containsKey("context")) {
                    final InputStream contextStream = cl
                            .getResourceAsStream(dir + "/" + test.get("context"));
                    final Object contextJson = JsonUtils.fromInputStream(contextStream);
                    result = JsonLdProcessor.flatten(input, contextJson, options);
                } else {
                    result = JsonLdProcessor.flatten(input, options);
                }
            } else if (testType.contains("jld:FrameTest")) {
                final InputStream frameStream = cl
                        .getResourceAsStream(dir + "/" + test.get("frame"));
                final Map<String, Object> frameJson = (Map<String, Object>) JsonUtils
                        .fromInputStream(frameStream);
                result = JsonLdProcessor.frame(input, frameJson, options);
            } else if (testType.contains("jld:FromRDFTest")) {
                result = JsonLdProcessor.fromRDF(input, options);
            } else if (testType.contains("jld:ToRDFTest")) {
                options.format = JsonLdConsts.APPLICATION_NQUADS;
                result = JsonLdProcessor.toRDF(input, options);
                result = ((String) result).trim();
            } else if (testType.contains("jld:NormalizeTest")) {
                options.format = JsonLdConsts.APPLICATION_NQUADS;
                result = JsonLdProcessor.normalize(input, options);
                result = ((String) result).trim();
            } else {
                fail("Unknown test type: " + testType);
            }
        } catch (final JsonLdError e) {
            result = e;
        }

        Boolean testpassed = false;
        try {
            if (failure_expected) {
                if (result instanceof JsonLdError) {
                    testpassed = Obj.equals(expect, ((JsonLdError) result).getType().toString());
                    if (!testpassed) {
                        ((JsonLdError) result).printStackTrace();
                    }
                }
            } else {
                testpassed = testType.contains("jld:PositiveSyntaxTest") || JsonLdUtils.deepCompare(expect, result);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }

        if (testpassed == false && result instanceof JsonLdError) {
            throw (JsonLdError) result;
        }

        if(!testpassed && skipFileWriter != null){
            System.err.println(skipId);
            skipFileWriter.append(skipId+"\n");
        }

        // write details to report
        final String manifest = group;
        final String id = (String) test.get("@id");
        final Date d = new Date();
        final String dateTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(d);
        String zone = new SimpleDateFormat("Z").format(d);
        zone = zone.substring(0, 3) + ":" + zone.substring(3);
        final String dateTimeZone = dateTime + zone;
        final Boolean passed = testpassed;
        reportGraph.add(new LinkedHashMap<String, Object>() {
            {
                put("@type", "earl:Assertion");
                put("earl:assertedBy", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", ASSERTOR);
                    }
                });
                put("earl:subject", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "https://github.com/jsonld-java/jsonld-java");
                    }
                });
                put("earl:test", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", manifest + id);
                    }
                });
                put("earl:result", new LinkedHashMap<String, Object>() {
                    {
                        put("@type", "earl:TestResult");
                        put("earl:outcome", new LinkedHashMap<String, Object>() {
                            {
                                put("@id", passed ? "earl:passed" : "earl:failed");
                            }
                        });
                        put("dc:date", new LinkedHashMap<String, Object>() {
                            {
                                put("@value", dateTimeZone);
                                put("@type", "xsd:dateTime");
                            }
                        });
                    }
                });
                // for error expand the correct error is thrown, but the test
                // suite doesn't yet automatically figure that out.
                put("earl:mode", new LinkedHashMap<String, Object>() {
                    {
                        put("@id",
                                "http://json-ld.org/test-suite/tests/error-expand-manifest.jsonld"
                                        .equals(manifest) ? "earl:semiAuto" : "earl:automatic");
                    }
                });
            }
        });

        assertTrue("\nFailed test: " + group + test.get("@id") + " " + test.get("name") + " ("
                + test.get("input") + "," + test.get("expect") + ")\n" + "expected: "
                + JsonUtils.toPrettyString(expect) + "\nresult: "
                + (result instanceof JsonLdError ? ((JsonLdError) result).toString()
                        : JsonUtils.toPrettyString(result)),
                testpassed);
    }

    static FileWriter skipFileWriter = null;
    static List<String> skipFileEntries = Collections.emptyList();

    public static void runChecked(String testDir, String group, String id, Map<String, Object> test,
            List<Object> graph) throws Exception {
        Assume.assumeFalse("Skip: " + skipId(group, id),
                skipFileEntries.contains(skipId(group, id)));
        try {
            SuiteUtils.run(testDir, group, id, test, graph);
        } catch (Exception e) {
            if (skipFileWriter != null) {
                skipFileWriter.append(skipId(group, id) + "\n");
            }
            e.printStackTrace();
            throw e;
        }
    }

    public static void setUpSkipFile(String skipFileLocation) {
        try {
            File skipFile = new File(skipFileLocation);
            if (skipFile.exists()) {
                skipFileEntries = Files.readAllLines(Paths.get(skipFile.toURI()));
            } else {
                skipFileWriter = new FileWriter(skipFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String skipId(String group, String id) {
        return group + id;
    }
}
