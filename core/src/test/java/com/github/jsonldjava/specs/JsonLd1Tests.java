package com.github.jsonldjava.specs;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.utils.SuiteUtils;

/**
 * Run the test suite from
 * https://github.com/json-ld/json-ld.org/tree/master/test-suite/tests
 */
@RunWith(Parameterized.class)
public class JsonLd1Tests {
    private static final String TEST_DIR = "json-ld-1.0-tests";
    private static final String REPORT_OUT = "reports/" + TEST_DIR + "-report";
    private static final String SKIP_FILE = "reports/" + TEST_DIR + "-skip";

    private static Map<String, Object> REPORT = SuiteUtils.generateReport();
    @SuppressWarnings("unchecked")
    private static List<Object> REPORT_GRAPH = (List<Object>) REPORT.get("@graph");

    private final String group;
    private String id;
    private final Map<String, Object> test;

    public JsonLd1Tests(final String group, final String id, final Map<String, Object> test) {
        this.group = group;
        this.id = id;
        this.test = test;
    }

    @Parameters(name = "{0}{1}")
    public static Collection<Object[]> data() throws URISyntaxException, IOException {
        return SuiteUtils.getData(TEST_DIR);
    }

    @BeforeClass
    public static void setUp() {
        SuiteUtils.setUpSkipFile(SKIP_FILE);
    }

    @Test
    public void runTest() throws URISyntaxException, IOException, JsonLdError, Exception {
        SuiteUtils.runChecked(TEST_DIR, group, id, test, REPORT_GRAPH);
    }

    @AfterClass
    public static void writeReport()
            throws JsonGenerationException, JsonMappingException, IOException, JsonLdError {
        // pass VM param: -Dreport.format=jsonld
        SuiteUtils.writeReport(REPORT, REPORT_OUT);
    }
}
