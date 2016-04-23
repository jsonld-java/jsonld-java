/**
 *
 */
package com.github.jsonldjava.core;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Random;
import java.util.zip.GZIPInputStream;

import org.junit.Ignore;
import org.junit.Test;

import com.github.jsonldjava.core.RDFDataset.Quad;
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

    @Test
    public final void testSerialisationPerformance() throws Exception {
        Random prng = new Random();

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
            potentialSubjects.add(exNs + Integer.toHexString(i) + "/z"
                    + Integer.toOctalString(i % 20));
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

        JsonLdOptions options = new JsonLdOptions();
        JsonLdApi jsonLdApi = new JsonLdApi(options);
        int rounds = 10000;
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
    }
}
