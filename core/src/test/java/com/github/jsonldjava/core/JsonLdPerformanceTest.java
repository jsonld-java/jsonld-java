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
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Random;
import java.util.function.Function;
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
        final File testFile = File.createTempFile("jsonld-perf-source-", ".jsonld", testDir);
        FileUtils.copyInputStreamToFile(nextInputStream, testFile);

        final LongSummaryStatistics parseStats = new LongSummaryStatistics();
        final LongSummaryStatistics compactStats = new LongSummaryStatistics();

        for (int i = 0; i < 1000; i++) {
            final InputStream testInput = new BufferedInputStream(new FileInputStream(testFile));
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
        final Random prng = new Random();
        final int rounds = 10000;

        final String exNs = "http://example.org/";

        final String bnode = "_:anon";
        final String uri1 = exNs + "a1";
        final String uri2 = exNs + "b2";
        final String uri3 = exNs + "c3";
        final List<String> potentialSubjects = new ArrayList<String>();
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

        final List<String> potentialObjects = new ArrayList<String>();
        potentialObjects.addAll(potentialSubjects);
        Collections.shuffle(potentialObjects, prng);

        final List<String> potentialPredicates = new ArrayList<String>();
        potentialPredicates.add(JsonLdConsts.RDF_TYPE);
        potentialPredicates.add(JsonLdConsts.RDF_LIST);
        potentialPredicates.add(JsonLdConsts.RDF_NIL);
        potentialPredicates.add(JsonLdConsts.RDF_FIRST);
        potentialPredicates.add(JsonLdConsts.RDF_OBJECT);
        potentialPredicates.add(JsonLdConsts.XSD_STRING);
        Collections.shuffle(potentialPredicates, prng);

        final RDFDataset testData = new RDFDataset();

        for (int i = 0; i < 2000; i++) {
            final String nextObject = potentialObjects.get(prng.nextInt(potentialObjects.size()));
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
        final JsonLdOptions options = new JsonLdOptions();
        final JsonLdApi jsonLdApi = new JsonLdApi(options);
        final int[] hashCodes = new int[rounds];
        final LongSummaryStatistics statsFirst5000 = new LongSummaryStatistics();
        final LongSummaryStatistics stats = new LongSummaryStatistics();
        for (int i = 0; i < rounds; i++) {
            final long start = System.nanoTime();
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

        System.out.println(
                "RDF triples to JSON-LD (internal objects, not parsed from a document), using laxMergeValue...");
        final JsonLdOptions optionsLax = new JsonLdOptions();
        final JsonLdApi jsonLdApiLax = new JsonLdApi(optionsLax);
        final int[] hashCodesLax = new int[rounds];
        final LongSummaryStatistics statsLaxFirst5000 = new LongSummaryStatistics();
        final LongSummaryStatistics statsLax = new LongSummaryStatistics();
        for (int i = 0; i < rounds; i++) {
            final long start = System.nanoTime();
            Object fromRDF = jsonLdApiLax.fromRDF(testData, true);
            if (i < 5000) {
                statsLaxFirst5000.accept(System.nanoTime() - start);
            } else {
                statsLax.accept(System.nanoTime() - start);
            }
            hashCodesLax[i] = fromRDF.hashCode();
            fromRDF = null;
        }
        System.out.println("First 5000 out of " + rounds);
        System.out.println("Average: " + statsLaxFirst5000.getAverage() / 100000);
        System.out.println("Sum: " + statsLaxFirst5000.getSum() / 100000);
        System.out.println("Maximum: " + statsLaxFirst5000.getMax() / 100000);
        System.out.println("Minimum: " + statsLaxFirst5000.getMin() / 100000);
        System.out.println("Count: " + statsLaxFirst5000.getCount());

        System.out.println("Post 5000 out of " + rounds);
        System.out.println("Average: " + statsLax.getAverage() / 100000);
        System.out.println("Sum: " + statsLax.getSum() / 100000);
        System.out.println("Maximum: " + statsLax.getMax() / 100000);
        System.out.println("Minimum: " + statsLax.getMin() / 100000);
        System.out.println("Count: " + statsLax.getCount());

        System.out.println("Non-pretty print benchmarking...");
        final JsonLdOptions options2 = new JsonLdOptions();
        final JsonLdApi jsonLdApi2 = new JsonLdApi(options2);
        final LongSummaryStatistics statsFirst5000Part2 = new LongSummaryStatistics();
        final LongSummaryStatistics statsPart2 = new LongSummaryStatistics();
        final Object fromRDF2 = jsonLdApi2.fromRDF(testData);
        for (int i = 0; i < rounds; i++) {
            final long start = System.nanoTime();
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
        final JsonLdOptions options3 = new JsonLdOptions();
        final JsonLdApi jsonLdApi3 = new JsonLdApi(options3);
        final LongSummaryStatistics statsFirst5000Part3 = new LongSummaryStatistics();
        final LongSummaryStatistics statsPart3 = new LongSummaryStatistics();
        final Object fromRDF3 = jsonLdApi3.fromRDF(testData);
        for (int i = 0; i < rounds; i++) {
            final long start = System.nanoTime();
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
        final JsonLdOptions options4 = new JsonLdOptions();
        final JsonLdApi jsonLdApi4 = new JsonLdApi(options4);
        final LongSummaryStatistics statsFirst5000Part4 = new LongSummaryStatistics();
        final LongSummaryStatistics statsPart4 = new LongSummaryStatistics();
        final Object fromRDF4 = jsonLdApi4.fromRDF(testData);
        for (int i = 0; i < rounds; i++) {
            final long start = System.nanoTime();
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

    /**
     * many triples with same subject and prop: current implementation is slow
     *
     * @author fpservant
     */
    @Ignore("Disable performance tests by default")
    @Test
    public final void slowVsFast5Predicates() throws Exception {

        final String ns = "http://www.example.com/foo/";

        final Function<Integer, String> subjectGenerator = new Function<Integer, String>() {
            @Override
            public String apply(Integer index) {
                return ns + "s";
            }
        };
        final Function<Integer, String> predicateGenerator = new Function<Integer, String>() {
            @Override
            public String apply(Integer index) {
                return ns + "p" + Integer.toString(index % 5);
            }
        };
        final Function<Integer, String> objectGenerator = new Function<Integer, String>() {
            @Override
            public String apply(Integer index) {
                return ns + "o" + Integer.toString(index);
            }
        };
        final int tripleCount = 2000;
        final int warmingRounds = 200;
        final int rounds = 1000;

        runLaxVersusSlowToRDFTest("5 predicates", ns, subjectGenerator, predicateGenerator,
                objectGenerator, tripleCount, warmingRounds, rounds);

    }

    /**
     * many triples with same subject and prop: current implementation is slow
     *
     * @author fpservant
     */
    @Ignore("Disable performance tests by default")
    @Test
    public final void slowVsFast2Predicates() throws Exception {

        final String ns = "http://www.example.com/foo/";

        final Function<Integer, String> subjectGenerator = new Function<Integer, String>() {
            @Override
            public String apply(Integer index) {
                return ns + "s";
            }
        };
        final Function<Integer, String> predicateGenerator = new Function<Integer, String>() {
            @Override
            public String apply(Integer index) {
                return ns + "p" + Integer.toString(index % 2);
            }
        };
        final Function<Integer, String> objectGenerator = new Function<Integer, String>() {
            @Override
            public String apply(Integer index) {
                return ns + "o" + Integer.toString(index);
            }
        };
        final int tripleCount = 2000;
        final int warmingRounds = 200;
        final int rounds = 1000;

        runLaxVersusSlowToRDFTest("2 predicates", ns, subjectGenerator, predicateGenerator,
                objectGenerator, tripleCount, warmingRounds, rounds);

    }

    /**
     * many triples with same subject and prop: current implementation is slow
     *
     * @author fpservant
     */
    @Ignore("Disable performance tests by default")
    @Test
    public final void slowVsFast1Predicate() throws Exception {

        final String ns = "http://www.example.com/foo/";

        final Function<Integer, String> subjectGenerator = new Function<Integer, String>() {
            @Override
            public String apply(Integer index) {
                return ns + "s";
            }
        };
        final Function<Integer, String> predicateGenerator = new Function<Integer, String>() {
            @Override
            public String apply(Integer index) {
                return ns + "p";
            }
        };
        final Function<Integer, String> objectGenerator = new Function<Integer, String>() {
            @Override
            public String apply(Integer index) {
                return ns + "o" + Integer.toString(index);
            }
        };
        final int tripleCount = 2000;
        final int warmingRounds = 200;
        final int rounds = 1000;

        runLaxVersusSlowToRDFTest("1 predicate", ns, subjectGenerator, predicateGenerator,
                objectGenerator, tripleCount, warmingRounds, rounds);

    }

    /**
     * many triples with same subject and prop: current implementation is slow
     *
     * @author fpservant
     */
    @Ignore("Disable performance tests by default")
    @Test
    public final void slowVsFastMultipleSubjects1Predicate() throws Exception {

        final String ns = "http://www.example.com/foo/";

        final Function<Integer, String> subjectGenerator = new Function<Integer, String>() {
            @Override
            public String apply(Integer index) {
                return ns + "s" + Integer.toString(index % 100);
            }
        };
        final Function<Integer, String> predicateGenerator = new Function<Integer, String>() {
            @Override
            public String apply(Integer index) {
                return ns + "p";
            }
        };
        final Function<Integer, String> objectGenerator = new Function<Integer, String>() {
            @Override
            public String apply(Integer index) {
                return ns + "o" + Integer.toString(index);
            }
        };
        final int tripleCount = 2000;
        final int warmingRounds = 200;
        final int rounds = 1000;

        runLaxVersusSlowToRDFTest("100 subjects and 1 predicate", ns, subjectGenerator,
                predicateGenerator, objectGenerator, tripleCount, warmingRounds, rounds);

    }

    /**
     * many triples with same subject and prop: current implementation is slow
     *
     * @author fpservant
     */
    @Ignore("Disable performance tests by default")
    @Test
    public final void slowVsFastMultipleSubjects5Predicates() throws Exception {

        final String ns = "http://www.example.com/foo/";

        final Function<Integer, String> subjectGenerator = new Function<Integer, String>() {
            @Override
            public String apply(Integer index) {
                return ns + "s" + Integer.toString(index % 1000);
            }
        };
        final Function<Integer, String> predicateGenerator = new Function<Integer, String>() {
            @Override
            public String apply(Integer index) {
                return ns + "p" + Integer.toString(index % 5);
            }
        };
        final Function<Integer, String> objectGenerator = new Function<Integer, String>() {
            @Override
            public String apply(Integer index) {
                return ns + "o" + Integer.toString(index);
            }
        };
        final int tripleCount = 2000;
        final int warmingRounds = 200;
        final int rounds = 1000;

        runLaxVersusSlowToRDFTest("1000 subjects and 5 predicates", ns, subjectGenerator,
                predicateGenerator, objectGenerator, tripleCount, warmingRounds, rounds);

    }

    /**
     * Run a test on lax versus slow methods for toRDF.
     *
     * @param ns
     *            The namespace to assign
     * @param subjectGenerator
     *            A {@link Function} used to generate the subject IRIs
     * @param predicateGenerator
     *            A {@link Function} used to generate the predicate IRIs
     * @param objectGenerator
     *            A {@link Function} used to generate the object IRIs
     * @param tripleCount
     *            The number of triples to create for the dataset
     * @param warmingRounds
     *            The number of warming rounds to use
     * @param rounds
     *            The number of test rounds to use
     * @throws JsonLdError
     *             If there is an error with the JSONLD processing.
     */
    private void runLaxVersusSlowToRDFTest(final String label, final String ns,
            Function<Integer, String> subjectGenerator,
            Function<Integer, String> predicateGenerator, Function<Integer, String> objectGenerator,
            int tripleCount, int warmingRounds, int rounds) throws JsonLdError {

        System.out.println("Running test for lax versus slow for " + label);

        final RDFDataset inputRdf = new RDFDataset();
        inputRdf.setNamespace("ex", ns);

        for (int i = 0; i < tripleCount; i++) {
            inputRdf.addTriple(subjectGenerator.apply(i), predicateGenerator.apply(i),
                    objectGenerator.apply(i));
        }

        final JsonLdOptions options = new JsonLdOptions();
        options.useNamespaces = true;

        // warming
        for (int i = 0; i < warmingRounds; i++) {
            new JsonLdApi(options).fromRDF(inputRdf);
            // JsonLdProcessor.expand(new JsonLdApi(options).fromRDF(inputRdf));
        }

        for (int i = 0; i < warmingRounds; i++) {
            new JsonLdApi(options).fromRDF(inputRdf, true);
            // JsonLdProcessor.expand(new JsonLdApi(options).fromRDF(inputRdf,
            // true));
        }

        System.out.println("Average time to parse a dataset containing " + tripleCount
                + " different triples:");
        final long startLax = System.currentTimeMillis();
        for (int i = 0; i < rounds; i++) {
            new JsonLdApi(options).fromRDF(inputRdf, true);
            // JsonLdProcessor.expand(new JsonLdApi(options).fromRDF(inputRdf,
            // true));
        }
        System.out.println("\t- Assuming no duplicates: "
                + (((System.currentTimeMillis() - startLax)) / rounds));

        final long start = System.currentTimeMillis();
        for (int i = 0; i < rounds; i++) {
            new JsonLdApi(options).fromRDF(inputRdf);
            // JsonLdProcessor.expand(new JsonLdApi(options).fromRDF(inputRdf));
        }
        System.out.println(
                "\t- Assuming duplicates: " + (((System.currentTimeMillis() - start)) / rounds));
    }

    /**
     * @author fpservant
     */
    @Test
    public final void duplicatedTriplesInAnRDFDataset() throws Exception {
        final RDFDataset inputRdf = new RDFDataset();
        final String ns = "http://www.example.com/foo/";
        inputRdf.setNamespace("ex", ns);
        inputRdf.addTriple(ns + "s", ns + "p", ns + "o");
        inputRdf.addTriple(ns + "s", ns + "p", ns + "o");

        // System.out.println("Twice the same triple in RDFDataset:/n");
        for (final Quad quad : inputRdf.getQuads("@default")) {
            // System.out.println(quad);
        }

        final JsonLdOptions options = new JsonLdOptions();
        options.useNamespaces = true;

        // System.out.println("\nJSON-LD output is OK:\n");
        final Object fromRDF1 = JsonLdProcessor.compact(new JsonLdApi(options).fromRDF(inputRdf),
                inputRdf.getContext(), options);

        final String jsonld1 = JsonUtils.toPrettyString(fromRDF1);
        // System.out.println(jsonld1);

        // System.out.println(
        // "\nWouldn't be the case assuming there is no duplicated triple in
        // RDFDataset:\n");
        final Object fromRDF2 = JsonLdProcessor.compact(
                new JsonLdApi(options).fromRDF(inputRdf, true), inputRdf.getContext(), options);
        final String jsonld2 = JsonUtils.toPrettyString(fromRDF2);
        // System.out.println(jsonld2);

    }
}
