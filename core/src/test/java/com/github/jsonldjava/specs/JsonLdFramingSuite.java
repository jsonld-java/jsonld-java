package com.github.jsonldjava.specs;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
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
 * https://github.com/w3c/json-ld-framing/blob/master/tests/README.md
 */
@RunWith(Parameterized.class)
public class JsonLdFramingSuite {
    private static final String TEST_DIR = "json-ld-framing-tests";
    private static final String REPORT_OUT = "reports/" + TEST_DIR + "-report";

    private static Map<String, Object> REPORT = SuiteUtils.generateReport();
    @SuppressWarnings("unchecked")
    private static List<Object> REPORT_GRAPH = (List<Object>) REPORT.get("@graph");

    private final String group;
    private final Map<String, Object> test;

    public JsonLdFramingSuite(final String group, final String id, final Map<String, Object> test) {
        this.group = group;
        this.test = test;
    }

    @Parameters(name = "{0}|{1}")
    public static Collection<Object[]> data() throws URISyntaxException, IOException {
        return SuiteUtils.getData(TEST_DIR);
    }

    @Test
    public void runTest() throws URISyntaxException, IOException, JsonLdError {
        SuiteUtils.run(TEST_DIR, group, test, REPORT_GRAPH);
    }

    @AfterClass
    public static void writeReport()
            throws JsonGenerationException, JsonMappingException, IOException, JsonLdError {
        // pass VM param: -Dreport.format=jsonld
        SuiteUtils.writeReport(REPORT, REPORT_OUT);
    }
}
