/**
 *
 */
package com.github.jsonldjava.core;

import java.io.File;
import java.io.FileInputStream;
import java.util.zip.GZIPInputStream;

import org.junit.Ignore;
import org.junit.Test;

import com.github.jsonldjava.utils.JsonUtils;

/**
 *
 * @author Peter Ansell p_ansell@yahoo.com
 */
public class JsonLdPerformanceTest {

    /**
     * Test performance parsing using test data from:
     *
     * https://dl.dropboxusercontent.com/s/yha7x0paj8zvvz5/2000007922.jsonld.gz
     *
     * @throws Exception
     */
    @Ignore("Enable as necessary for manual testing, particularly to test that it fails due to irregular URIs")
    @Test
    public final void test() throws Exception {
        final long parseStart = System.currentTimeMillis();
        final Object inputObject = JsonUtils.fromInputStream(new GZIPInputStream(
                new FileInputStream(new File("/home/ans025/Downloads/2000007922.jsonld.gz"))));
        final long parseEnd = System.currentTimeMillis();
        System.out.printf("Parse time: %d", (parseEnd - parseStart));
        final JsonLdOptions opts = new JsonLdOptions("urn:test:");

        final long compactStart = System.currentTimeMillis();
        JsonLdProcessor.compact(inputObject, null, opts);
        final long compactEnd = System.currentTimeMillis();
        System.out.printf("Compaction time: %d", (compactEnd - compactStart));
    }

}
