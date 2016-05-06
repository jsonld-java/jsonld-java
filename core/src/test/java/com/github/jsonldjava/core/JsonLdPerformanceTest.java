/**
 *
 */
package com.github.jsonldjava.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Random;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.jsonldjava.core.RDFDataset.Quad;
import com.github.jsonldjava.utils.JsonUtils;

/**
 *
 * @author Peter Ansell p_ansell@yahoo.com
 */
public class JsonLdPerformanceTest {

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    private File testDir;

    @Before
    public void setUp() throws Exception {
        testDir = tempDir.newFolder("jsonld-perf-tests-");
    }

    /**
     * Test performance parsing using test data from:
     *
     * https://dl.dropboxusercontent.com/s/yha7x0paj8zvvz5/2000007922.jsonld.gz
     *
     * @throws Exception
     */
    @Ignore("Enable as necessary for manual testing, particularly to test that it fails due to irregular URIs")
    @Test
    public final void testPerformance1() throws Exception {
        testCompaction("Long", new GZIPInputStream(
                new FileInputStream(new File("/home/peter/Downloads/2000007922.jsonld.gz"))));
    }

    /**
     * Test performance parsing using test data from:
     *
     * https://github.com/jsonld-java/jsonld-java/files/245372/jsonldperfs.zip
     *
     * @throws Exception
     */
    @Ignore("Enable as necessary to test performance")
    @Test
    public final void testLaxMergeValuesPerfFast() throws Exception {
        testCompaction("Fast",
                new FileInputStream(new File("/home/peter/Downloads/jsonldperfs/fast.jsonld")));
    }

    /**
     * Test performance parsing using test data from:
     *
     * https://github.com/jsonld-java/jsonld-java/files/245372/jsonldperfs.zip
     *
     * @throws Exception
     */
    @Ignore("Enable as necessary to test performance")
    @Test
    public final void testLaxMergeValuesPerfSlow() throws Exception {
        testCompaction("Slow",
                new FileInputStream(new File("/home/peter/Downloads/jsonldperfs/slow.jsonld")));
    }

    private void testCompaction(String label, InputStream nextInputStream)
            throws IOException, FileNotFoundException, JsonLdError {
        File testFile = File.createTempFile("jsonld-perf-source-", ".jsonld", testDir);
        FileUtils.copyInputStreamToFile(nextInputStream, testFile);

        LongSummaryStatistics parseStats = new LongSummaryStatistics();
        LongSummaryStatistics compactStats = new LongSummaryStatistics();

        for (int i = 0; i < 1000; i++) {
            InputStream testInput = new BufferedInputStream(new FileInputStream(testFile));
            try {
                final long parseStart = System.currentTimeMillis();
                final Object inputObject = JsonUtils.fromInputStream(testInput);
                parseStats.accept(System.currentTimeMillis() - parseStart);
                final JsonLdOptions opts = new JsonLdOptions("urn:test:");

                final long compactStart = System.currentTimeMillis();
                JsonLdProcessor.compact(inputObject, null, opts);
                compactStats.accept(System.currentTimeMillis() - compactStart);
            } finally {
                testInput.close();
            }
        }

        System.out.println("(" + label + ") Parse average : " + parseStats.getAverage());
        System.out.println("(" + label + ") Compact average : " + compactStats.getAverage());
    }

    @Ignore("Disable performance tests by default")
    @Test
    public final void testPerformanceRandom() throws Exception {
        Random prng = new Random();
        int rounds = 2000;

        String exNs = "http://example.org/";

        String bnode = "_:anon";
        String uri1 = exNs + "a1";
        String uri2 = exNs + "b2";
        String uri3 = exNs + "c3";
        List<String> potentialSubjects = new ArrayList<String>();
        potentialSubjects.add(bnode);
        potentialSubjects.add(uri1);
        potentialSubjects.add(uri2);
        potentialSubjects.add(uri3);
        for (int i = 0; i < 50; i++) {
            potentialSubjects.add("_:" + i);
        }
        for (int i = 1; i < 50; i++) {
            potentialSubjects.add("_:a" + Integer.toHexString(i).toUpperCase());
        }
        for (int i = 0; i < 200; i++) {
            potentialSubjects
                    .add(exNs + Integer.toHexString(i) + "/z" + Integer.toOctalString(i % 20));
        }
        Collections.shuffle(potentialSubjects, prng);

        List<String> potentialObjects = new ArrayList<String>();
        potentialObjects.addAll(potentialSubjects);
        Collections.shuffle(potentialObjects, prng);

        List<String> potentialPredicates = new ArrayList<String>();
        potentialPredicates.add(JsonLdConsts.RDF_TYPE);
        potentialPredicates.add(JsonLdConsts.RDF_LIST);
        potentialPredicates.add(JsonLdConsts.RDF_NIL);
        potentialPredicates.add(JsonLdConsts.RDF_FIRST);
        potentialPredicates.add(JsonLdConsts.RDF_OBJECT);
        potentialPredicates.add(JsonLdConsts.XSD_STRING);
        Collections.shuffle(potentialPredicates, prng);

        RDFDataset testData = new RDFDataset();

        for (int i = 0; i < 8000; i++) {
            String nextObject = potentialObjects.get(prng.nextInt(potentialObjects.size()));
            boolean isLiteral = true;
            if (nextObject.startsWith("_:") || nextObject.startsWith("http://")) {
                isLiteral = false;
            }
            if (isLiteral) {
                if (i % 2 == 0) {
                    testData.addQuad(potentialSubjects.get(prng.nextInt(potentialSubjects.size())),
                            potentialPredicates.get(prng.nextInt(potentialPredicates.size())),
                            nextObject, JsonLdConsts.XSD_STRING,
                            potentialSubjects.get(prng.nextInt(potentialSubjects.size())), null);
                } else if (i % 5 == 0) {
                    testData.addTriple(
                            potentialSubjects.get(prng.nextInt(potentialSubjects.size())),
                            potentialPredicates.get(prng.nextInt(potentialPredicates.size())),
                            nextObject, JsonLdConsts.RDF_LANGSTRING, "en");
                }
            } else {
                if (i % 2 == 0) {
                    testData.addQuad(potentialSubjects.get(prng.nextInt(potentialSubjects.size())),
                            potentialPredicates.get(prng.nextInt(potentialPredicates.size())),
                            nextObject,
                            potentialSubjects.get(prng.nextInt(potentialSubjects.size())));
                } else if (i % 5 == 0) {
                    testData.addTriple(
                            potentialSubjects.get(prng.nextInt(potentialSubjects.size())),
                            potentialPredicates.get(prng.nextInt(potentialPredicates.size())),
                            nextObject);
                }
            }
        }

        System.out.println(
                "RDF triples to JSON-LD (internal objects, not parsed from a document)...");
        JsonLdOptions options = new JsonLdOptions();
        JsonLdApi jsonLdApi = new JsonLdApi(options);
        int[] hashCodes = new int[rounds];
        LongSummaryStatistics statsFirst5000 = new LongSummaryStatistics();
        LongSummaryStatistics stats = new LongSummaryStatistics();
        for (int i = 0; i < rounds; i++) {
            long start = System.nanoTime();
            Object fromRDF = jsonLdApi.fromRDF(testData);
            if (i < 5000) {
                statsFirst5000.accept(System.nanoTime() - start);
            } else {
                stats.accept(System.nanoTime() - start);
            }
            hashCodes[i] = fromRDF.hashCode();
            fromRDF = null;
        }
        System.out.println("First 5000 out of " + rounds);
        System.out.println("Average: " + statsFirst5000.getAverage() / 100000);
        System.out.println("Sum: " + statsFirst5000.getSum() / 100000);
        System.out.println("Maximum: " + statsFirst5000.getMax() / 100000);
        System.out.println("Minimum: " + statsFirst5000.getMin() / 100000);
        System.out.println("Count: " + statsFirst5000.getCount());

        System.out.println("Post 5000 out of " + rounds);
        System.out.println("Average: " + stats.getAverage() / 100000);
        System.out.println("Sum: " + stats.getSum() / 100000);
        System.out.println("Maximum: " + stats.getMax() / 100000);
        System.out.println("Minimum: " + stats.getMin() / 100000);
        System.out.println("Count: " + stats.getCount());

        System.out.println("Non-pretty print benchmarking...");
        JsonLdOptions options2 = new JsonLdOptions();
        JsonLdApi jsonLdApi2 = new JsonLdApi(options2);
        LongSummaryStatistics statsFirst5000Part2 = new LongSummaryStatistics();
        LongSummaryStatistics statsPart2 = new LongSummaryStatistics();
        Object fromRDF2 = jsonLdApi2.fromRDF(testData);
        for (int i = 0; i < rounds; i++) {
            long start = System.nanoTime();
            JsonUtils.toString(fromRDF2);
            if (i < 5000) {
                statsFirst5000Part2.accept(System.nanoTime() - start);
            } else {
                statsPart2.accept(System.nanoTime() - start);
            }
        }
        System.out.println("First 5000 out of " + rounds);
        System.out.println("Average: " + statsFirst5000Part2.getAverage() / 100000);
        System.out.println("Sum: " + statsFirst5000Part2.getSum() / 100000);
        System.out.println("Maximum: " + statsFirst5000Part2.getMax() / 100000);
        System.out.println("Minimum: " + statsFirst5000Part2.getMin() / 100000);
        System.out.println("Count: " + statsFirst5000Part2.getCount());

        System.out.println("Post 5000 out of " + rounds);
        System.out.println("Average: " + statsPart2.getAverage() / 100000);
        System.out.println("Sum: " + statsPart2.getSum() / 100000);
        System.out.println("Maximum: " + statsPart2.getMax() / 100000);
        System.out.println("Minimum: " + statsPart2.getMin() / 100000);
        System.out.println("Count: " + statsPart2.getCount());

        System.out.println("Pretty print benchmarking...");
        JsonLdOptions options3 = new JsonLdOptions();
        JsonLdApi jsonLdApi3 = new JsonLdApi(options3);
        LongSummaryStatistics statsFirst5000Part3 = new LongSummaryStatistics();
        LongSummaryStatistics statsPart3 = new LongSummaryStatistics();
        Object fromRDF3 = jsonLdApi3.fromRDF(testData);
        for (int i = 0; i < rounds; i++) {
            long start = System.nanoTime();
            JsonUtils.toPrettyString(fromRDF3);
            if (i < 5000) {
                statsFirst5000Part3.accept(System.nanoTime() - start);
            } else {
                statsPart3.accept(System.nanoTime() - start);
            }
        }
        System.out.println("First 5000 out of " + rounds);
        System.out.println("Average: " + statsFirst5000Part3.getAverage() / 100000);
        System.out.println("Sum: " + statsFirst5000Part3.getSum() / 100000);
        System.out.println("Maximum: " + statsFirst5000Part3.getMax() / 100000);
        System.out.println("Minimum: " + statsFirst5000Part3.getMin() / 100000);
        System.out.println("Count: " + statsFirst5000Part3.getCount());

        System.out.println("Post 5000 out of " + rounds);
        System.out.println("Average: " + statsPart3.getAverage() / 100000);
        System.out.println("Sum: " + statsPart3.getSum() / 100000);
        System.out.println("Maximum: " + statsPart3.getMax() / 100000);
        System.out.println("Minimum: " + statsPart3.getMin() / 100000);
        System.out.println("Count: " + statsPart3.getCount());

        System.out.println("Expansion benchmarking...");
        JsonLdOptions options4 = new JsonLdOptions();
        JsonLdApi jsonLdApi4 = new JsonLdApi(options4);
        LongSummaryStatistics statsFirst5000Part4 = new LongSummaryStatistics();
        LongSummaryStatistics statsPart4 = new LongSummaryStatistics();
        Object fromRDF4 = jsonLdApi4.fromRDF(testData);
        for (int i = 0; i < rounds; i++) {
            long start = System.nanoTime();
            JsonLdProcessor.expand(fromRDF4, options4);
            if (i < 5000) {
                statsFirst5000Part4.accept(System.nanoTime() - start);
            } else {
                statsPart4.accept(System.nanoTime() - start);
            }
        }
        System.out.println("First 5000 out of " + rounds);
        System.out.println("Average: " + statsFirst5000Part4.getAverage() / 100000);
        System.out.println("Sum: " + statsFirst5000Part4.getSum() / 100000);
        System.out.println("Maximum: " + statsFirst5000Part4.getMax() / 100000);
        System.out.println("Minimum: " + statsFirst5000Part4.getMin() / 100000);
        System.out.println("Count: " + statsFirst5000Part4.getCount());

        System.out.println("Post 5000 out of " + rounds);
        System.out.println("Average: " + statsPart4.getAverage() / 100000);
        System.out.println("Sum: " + statsPart4.getSum() / 100000);
        System.out.println("Maximum: " + statsPart4.getMax() / 100000);
        System.out.println("Minimum: " + statsPart4.getMin() / 100000);
        System.out.println("Count: " + statsPart4.getCount());

    }
}
