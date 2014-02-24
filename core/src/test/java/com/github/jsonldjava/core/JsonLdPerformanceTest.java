/**
 * 
 */
package com.github.jsonldjava.core;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import org.junit.After;
import org.junit.Before;
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
        long parseStart = System.currentTimeMillis();
        Object inputObject = JsonUtils.fromInputStream(new GZIPInputStream(new FileInputStream(
                new File("/home/ans025/Downloads/2000007922.jsonld.gz"))));
        long parseEnd = System.currentTimeMillis();
        System.out.printf("Parse time: %d", (parseEnd - parseStart));
        JsonLdOptions opts = new JsonLdOptions("urn:test:");

        long compactStart = System.currentTimeMillis();
        JsonLdProcessor.compact(inputObject, null, opts);
        long compactEnd = System.currentTimeMillis();
        System.out.printf("Compaction time: %d", (compactEnd - compactStart));
    }

}
