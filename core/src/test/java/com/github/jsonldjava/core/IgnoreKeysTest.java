package com.github.jsonldjava.core;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.github.jsonldjava.impl.NQuadTripleCallback;
import com.github.jsonldjava.utils.JSONUtils;
import com.github.jsonldjava.utils.Obj;

/**
 * Test to make sure the ignoreKeys are behaving as expected
 * 
 * @author tristan
 * 
 *         NOTE: all these tests are currently disabled as the functionality has
 *         been removed from the core code
 */
public class IgnoreKeysTest {

    private static final String TEST_DIR = "custom";

    // @Test
    public void expandTest() throws JSONLDProcessingError, IOException {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final InputStream inputStream = cl.getResourceAsStream(TEST_DIR + "/ignore-0001-in.jsonld");
        final InputStream outputStream = cl.getResourceAsStream(TEST_DIR
                + "/ignore-0001-out.jsonld");
        final Object input = JSONUtils.fromInputStream(inputStream);
        final Object output = JSONUtils.fromInputStream(outputStream);

        final Object result = JSONLD.expand(input, new Options("").ignoreKey("@ignoreMe"));

        docheck("expand", output, result);
    }

    // @Test
    public void compactTest1() throws JSONLDProcessingError, IOException {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final InputStream inputStream = cl.getResourceAsStream(TEST_DIR + "/ignore-0002-in.jsonld");
        final InputStream outputStream = cl.getResourceAsStream(TEST_DIR
                + "/ignore-0002-out.jsonld");
        final InputStream contextStream = cl.getResourceAsStream(TEST_DIR
                + "/compact-0002-context.jsonld");
        final Object input = JSONUtils.fromInputStream(inputStream);
        final Object output = JSONUtils.fromInputStream(outputStream);
        final Object context = JSONUtils.fromInputStream(contextStream);

        final Object result = JSONLD
                .compact(input, context, new Options("").ignoreKey("@ignoreMe"));

        docheck("compact 1", output, result);
    }

    // @Test
    public void compactTest2() throws JSONLDProcessingError, IOException {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final InputStream inputStream = cl.getResourceAsStream(TEST_DIR + "/ignore-0003-in.jsonld");
        final InputStream outputStream = cl.getResourceAsStream(TEST_DIR
                + "/ignore-0003-out.jsonld");
        final InputStream contextStream = cl.getResourceAsStream(TEST_DIR
                + "/compact-0002-context.jsonld");
        final Object input = JSONUtils.fromInputStream(inputStream);
        final Object output = JSONUtils.fromInputStream(outputStream);
        final Object context = JSONUtils.fromInputStream(contextStream);

        final Object result = JSONLD
                .compact(input, context, new Options("").ignoreKey("@ignoreMe"));

        docheck("compact 2", output, result);
    }

    // @Test
    public void frameTest1() throws JSONLDProcessingError, IOException {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final InputStream inputStream = cl.getResourceAsStream(TEST_DIR + "/ignore-0004-in.jsonld");
        final InputStream outputStream = cl.getResourceAsStream(TEST_DIR
                + "/ignore-0004-out.jsonld");
        final InputStream frameStream = cl.getResourceAsStream(TEST_DIR
                + "/frame-0002-frame.jsonld");
        final Object input = JSONUtils.fromInputStream(inputStream);
        final Object output = JSONUtils.fromInputStream(outputStream);
        final Object frame = JSONUtils.fromInputStream(frameStream);

        final Object result = JSONLD.frame(input, (Map<String, Object>) frame,
                new Options("").ignoreKey("@ignoreMe"));

        docheck("frame 1", output, result);
    }

    // @Test
    public void toRDFTest() throws JSONLDProcessingError, IOException {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final InputStream inputStream = cl.getResourceAsStream(TEST_DIR + "/ignore-0005-in.jsonld");
        final InputStream expectStream = cl.getResourceAsStream(TEST_DIR + "/toRdf-0001-out.nq");
        final Object input = JSONUtils.fromInputStream(inputStream);
        final List<String> expectLines = new ArrayList<String>();
        final BufferedReader buf = new BufferedReader(new InputStreamReader(expectStream));
        String line;
        while ((line = buf.readLine()) != null) {
            line = line.trim();
            if (line.length() == 0 || line.charAt(0) == '#') {
                continue;
            }
            expectLines.add(line);
        }
        Collections.sort(expectLines);
        final String expect = JSONLDProcessorTest.join(expectLines, "\n");
        final NQuadTripleCallback callback = new NQuadTripleCallback();
        // JSONLD.toRDF(input, new Options("").ignoreKey("@ignoreMe"),
        // callback);
        // TODO: FIXME!

        // docheck("toRDF", expect, callback.getResult());
    }

    private void docheck(String test, Object expect, Object result) {
        boolean testpassed = false;
        try {
            testpassed = Obj.equals(expect, result);
            if (testpassed == false) {
                System.out.println("failed test: " + test);
                System.out.println("{\"expected\": " + JSONUtils.toString(expect)
                        + "\n,\"result\": " + JSONUtils.toString(result) + "}");
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
        assertTrue(
                "\nFailed test: ignore check " + test + "\n" + "expected: "
                        + JSONUtils.toString(expect) + "\nresult: " + JSONUtils.toString(result),
                testpassed);
    }

}
