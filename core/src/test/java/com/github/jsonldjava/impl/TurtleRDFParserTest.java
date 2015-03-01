package com.github.jsonldjava.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.RDFDataset;
import com.github.jsonldjava.core.RDFDataset.Quad;
import com.github.jsonldjava.core.RDFDatasetUtils;
import com.github.jsonldjava.utils.EarlTestSuite;
import com.github.jsonldjava.utils.Obj;

@Ignore
@RunWith(Parameterized.class)
public class TurtleRDFParserTest {

    // @Test
    public void simpleTest() throws JsonLdError {

        final String input = "@prefix ericFoaf: <http://www.w3.org/People/Eric/ericP-foaf.rdf#> .\n"
                + "@prefix : <http://xmlns.com/foaf/0.1/> .\n"
                + "ericFoaf:ericP :givenName \"Eric\" ;\n"
                + "\t:knows <http://norman.walsh.name/knows/who/dan-brickley> ,\n"
                + "\t\t[ :mbox <mailto:timbl@w3.org> ] ,\n" + "\t\t<http://getopenid.com/amyvdh> .";

        final List<Map<String, Object>> expected = new ArrayList<Map<String, Object>>() {
            {
                add(new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "_:b1");
                        put("http://xmlns.com/foaf/0.1/mbox", new ArrayList<Object>() {
                            {
                                add(new LinkedHashMap<String, Object>() {
                                    {
                                        put("@id", "mailto:timbl@w3.org");
                                    }
                                });
                            }
                        });
                    }
                });
                add(new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://getopenid.com/amyvdh");
                    }
                });
                add(new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://norman.walsh.name/knows/who/dan-brickley");
                    }
                });
                add(new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/People/Eric/ericP-foaf.rdf#ericP");
                        put("http://xmlns.com/foaf/0.1/givenName", new ArrayList<Object>() {
                            {
                                add(new LinkedHashMap<String, Object>() {
                                    {
                                        put("@value", "Eric");
                                    }
                                });
                            }
                        });
                        put("http://xmlns.com/foaf/0.1/knows", new ArrayList<Object>() {
                            {
                                add(new LinkedHashMap<String, Object>() {
                                    {
                                        put("@id",
                                                "http://norman.walsh.name/knows/who/dan-brickley");
                                    }
                                });
                                add(new LinkedHashMap<String, Object>() {
                                    {
                                        put("@id", "_:b1");
                                    }
                                });
                                add(new LinkedHashMap<String, Object>() {
                                    {
                                        put("@id", "http://getopenid.com/amyvdh");
                                    }
                                });
                            }
                        });
                    }
                });
                add(new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "mailto:timbl@w3.org");
                    }
                });
            }
        };

        final Object json = null; /*
         * JsonLdProcessor.fromRDF(input, new
         * JsonLdOptions() { { format = "text/turtle";
         * } }, new TurtleRDFParser());
         */
        assertTrue(Obj.equals(expected, json));
    }

    @BeforeClass
    public static void before() {
        if (CACHE_DIR == null) {
            System.out.println("Using temp dir: " + System.getProperty("java.io.tmpdir"));
        }
    }

    private static String TURTLE_TEST_MANIFEST = "https://dvcs.w3.org/hg/rdf/raw-file/default/rdf-turtle/tests-ttl/manifest.ttl";
    private static final String LAST_ETAG = null; // "1369157887.0";
    private static final String CACHE_DIR = null;

    @Parameters(name = "{0}{1}")
    public static Collection<Object[]> data() throws URISyntaxException, IOException {

        final EarlTestSuite testSuite = new EarlTestSuite(TURTLE_TEST_MANIFEST, CACHE_DIR,
                LAST_ETAG);

        final Collection<Object[]> rdata = new ArrayList<Object[]>();

        for (final Map<String, Object> test : testSuite.getTests()) {
            rdata.add(new Object[] { testSuite, test.get("@id"), test });
        }

        return rdata;
    }

    private final Map<String, Object> test;
    private final EarlTestSuite testSuite;

    public TurtleRDFParserTest(final EarlTestSuite testSuite, final String id,
            final Map<String, Object> test) {
        this.test = test;
        this.testSuite = testSuite;
    }

    @Test
    public void runTest() throws IOException, JsonLdError {
        final String inputfn = (String) Obj.get(test, "mf:action", "@id");
        final String outputfn = (String) Obj.get(test, "mf:result", "@id");
        final String type = (String) Obj.get(test, "@type");
        final String input = testSuite.getFile(inputfn);

        Boolean passed = false;
        String failmsg = "";
        if ("rdft:TestTurtleEval".equals(type)) {
            final RDFDataset result = new TurtleRDFParser().parse(input);
            final RDFDataset expected = RDFDatasetUtils.parseNQuads(testSuite.getFile(outputfn));
            passed = compareDatasets("http://example/base/" + inputfn, result, expected);
            if (!passed) {
                failmsg = "\n" + "Expected: " + RDFDatasetUtils.toNQuads(expected) + "\n"
                        + "Result  : " + RDFDatasetUtils.toNQuads(result);
            }
        } else if ("rdft:TestTurtlePositiveSyntax".equals(type)) {
            /*
             * JsonLdProcessor.fromRDF(input, new
             * JsonLdOptions("http://example/base/") { { format = "text/turtle";
             * } }); passed = true; // otherwise an exception would have been
             * thrown
             */
            // TODO: temporary until new code is done
            throw new JsonLdError(JsonLdError.Error.NOT_IMPLEMENTED, "");
        } else if ("rdft:TestTurtleNegativeSyntax".equals(type)
                || "rdft:TestTurtleNegativeEval".equals(type)) {
            // TODO: need to figure out how to properly deal with negative tests
            try {
                /*
                 * JsonLdProcessor.fromRDF(input, new
                 * JsonLdOptions("http://example/base/") { { format =
                 * "text/turtle"; } });
                 */
                failmsg = "Expected parse error, but no problems detected";
                throw new JsonLdError(JsonLdError.Error.NOT_IMPLEMENTED, "");
            } catch (final JsonLdError e) {
                if (e.getType() == JsonLdError.Error.PARSE_ERROR) {
                    passed = true;
                } else {
                    failmsg = "Expected parse error, got: " + e.getMessage();
                }
            }
        } else {
            failmsg = "DON'T KNOW HOW TO HANDLE: " + type;
        }
        assertTrue(failmsg, passed);
    }

    /**
     * Compare datasets, normalizing the blank nodes and adding baseIRI to
     * relative IRIs
     *
     * @param result
     * @param expected
     * @return
     */
    private Boolean compareDatasets(final String baseIRI, final RDFDataset result,
            final RDFDataset expected) {
        final String baseIRIpath = baseIRI.substring(0, baseIRI.lastIndexOf("/") + 1);
        final List<RDFDataset.Quad> res = new ArrayList<RDFDataset.Quad>() {
            {
                for (final RDFDataset.Quad q : result.getQuads("@default")) {
                    final RDFDataset.Node s = q.getSubject();
                    final RDFDataset.Node p = q.getPredicate();
                    final RDFDataset.Node o = q.getObject();
                    if (s.isIRI() && !s.getValue().contains(":")) {
                        final String v = s.getValue();
                        if (v.startsWith("#") || v.startsWith("?")) {
                            s.put("value", baseIRI + s.getValue());
                        } else {
                            s.put("value", baseIRIpath + s.getValue());
                        }
                    }
                    if (p.isIRI() && !p.getValue().contains(":")) {
                        final String v = p.getValue();
                        if (v.startsWith("#") || v.startsWith("?")) {
                            p.put("value", baseIRI + p.getValue());
                        } else {
                            p.put("value", baseIRIpath + p.getValue());
                        }
                    }
                    if (o.isIRI() && !o.getValue().contains(":")) {
                        final String v = o.getValue();
                        if (v.startsWith("#") || v.startsWith("?")) {
                            o.put("value", baseIRI + o.getValue());
                        } else {
                            o.put("value", baseIRIpath + o.getValue());
                        }
                    }
                    add(q);
                }
            }
        };
        final List<RDFDataset.Quad> exp = new ArrayList<RDFDataset.Quad>() {
            {
                addAll(expected.getQuads("@default"));
            }
        };
        final List<RDFDataset.Quad> unmatched = new ArrayList<RDFDataset.Quad>();
        final BnodeMappings bnodeMaps = new BnodeMappings();
        boolean finalpass = false;
        while (!exp.isEmpty() && !res.isEmpty()) {
            final Quad eq = exp.remove(0);
            int matches = 0;
            RDFDataset.Quad last_match = null;
            for (final RDFDataset.Quad rq : res) {
                // if predicates are not equal there cannot be a match
                if (!eq.getPredicate().equals(rq.getPredicate())) {
                    continue;
                }
                if (eq.getSubject().isBlankNode() && rq.getSubject().isBlankNode()) {
                    // check for locking
                    boolean subjectLocked = false;
                    if (bnodeMaps.isLocked(eq.getSubject().getValue())) {
                        // if this mapping doesn't match the locked mapping, we
                        // don't have a match
                        if (!rq.getSubject().getValue()
                                .equals(bnodeMaps.getMapping(eq.getSubject().getValue()))) {
                            continue;
                        }
                        subjectLocked = true;
                    }
                    // if the objects are also both blank nodes
                    if (eq.getObject().isBlankNode() && rq.getObject().isBlankNode()) {
                        // check for locking
                        if (bnodeMaps.isLocked(eq.getObject().getValue())) {
                            // if this mapping doesn't match the locked mapping,
                            // we don't have a match
                            if (!rq.getObject().getValue()
                                    .equals(bnodeMaps.getMapping(eq.getObject().getValue()))) {
                                continue;
                            }
                        } else {
                            // add possible mappings for the objects
                            bnodeMaps.addPossibleMapping(eq.getObject().getValue(), rq.getObject()
                                    .getValue());
                        }
                    }
                    // otherwise, if the objects aren't equal we can't have a
                    // match
                    else if (!eq.getObject().equals(rq.getObject())) {
                        continue;
                    }
                    // objects are equal or both blank nodes so we have a match
                    matches++;
                    last_match = rq;
                    // if subject is not locked add a possible mapping between
                    // subjects
                    if (!subjectLocked) {
                        bnodeMaps.addPossibleMapping(eq.getSubject().getValue(), rq.getSubject()
                                .getValue());
                    }
                }
                // otherwise check if the subjects are equal
                else if (eq.getSubject().equals(rq.getSubject())) {
                    // if both objects are blank nodes, add possible mappings
                    // for them
                    if (eq.getObject().isBlankNode() && rq.getObject().isBlankNode()) {
                        // check for locking
                        if (bnodeMaps.isLocked(eq.getObject().getValue())) {
                            // if this mapping doesn't match the locked mapping,
                            // we don't have a match
                            if (!rq.getObject().getValue()
                                    .equals(bnodeMaps.getMapping(eq.getObject().getValue()))) {
                                continue;
                            }
                        } else {
                            // add possible mappings for the objects
                            bnodeMaps.addPossibleMapping(eq.getObject().getValue(), rq.getObject()
                                    .getValue());
                        }
                        // if we get here we have a match
                        matches++;
                        last_match = rq;
                    }
                    // otherwise, if the objects are equal we we have an exact
                    // match
                    else if (eq.getObject().equals(rq.getObject())) {
                        matches = 1;
                        last_match = rq;
                        break;
                    }
                }
            }

            if (matches == 0) {
                // if we didn't find any matches, we're done and things didn't
                // match!
                return false;
            } else if (matches == 1) {
                // we have one match
                if (eq.getSubject().isBlankNode()) {
                    // lock this mapping
                    bnodeMaps.lockMapping(eq.getSubject().getValue(), last_match.getSubject()
                            .getValue());
                }
                if (eq.getObject().isBlankNode()) {
                    // lock this mapping
                    bnodeMaps.lockMapping(eq.getObject().getValue(), last_match.getObject()
                            .getValue());
                }
                res.remove(last_match);
            } else {
                // we got multiple matches, we need to figure this stuff out
                // later!
                unmatched.add(eq);
            }

            // TODO: no tests so far test this out, make one!
            if (exp.isEmpty() && !finalpass) {
                // if we are at the end and we have unmatched triples
                if (!unmatched.isEmpty()) {
                    // lock the remaining bnodes, and test again
                    bnodeMaps.lockRemaining();
                    exp.addAll(unmatched);
                    unmatched.clear();
                }
                // we also only want to do this once, if we get here again
                // without matching everything
                // we're not going to match everything
                finalpass = true;
            }
        }

        // they both matched if we have nothing left over
        return res.isEmpty() && exp.isEmpty() && unmatched.isEmpty();
    }

    private class BnodeMappings {
        Map<String, Map<String, Integer>> possiblebnodemappings = new LinkedHashMap<String, Map<String, Integer>>();
        Map<String, String> lockedbnodemappings = new LinkedHashMap<String, String>();

        public void lockMapping(final String bn1, final String bn2) {
            lockedbnodemappings.put(bn1, bn2);
            possiblebnodemappings.remove(bn1);
            for (final String i : possiblebnodemappings.keySet()) {
                // remove bn2 as a possible mapping for any other bnodes
                possiblebnodemappings.get(i).remove(bn2);
            }
        }

        public void lockRemaining() {
            final List<String> unlocked = new ArrayList<String>(possiblebnodemappings.keySet());
            for (final String bn1 : unlocked) {
                final String bn2 = getMapping(bn1);
                assertNotNull("Unable to find mapping for blank node " + bn1
                        + ". Possible error in mapping code", bn2);
                lockMapping(bn1, bn2);
            }
        }

        public boolean isLocked(final String b) {
            return lockedbnodemappings.containsKey(b);
        }

        /**
         * return either the locked mapping, or the highest matching
         *
         * @param b
         * @return
         */
        public String getMapping(final String b) {
            if (isLocked(b)) {
                return lockedbnodemappings.get(b);
            } else {
                int max = -1;
                String rval = null;
                for (final Entry<String, Integer> map : possiblebnodemappings.get(b).entrySet()) {
                    if (map.getValue() > max) {
                        max = map.getValue();
                        rval = map.getKey();
                    }
                }
                return rval;
            }
        }

        public void addPossibleMapping(final String bn1, final String bn2) {
            Map<String, Integer> bn1m;
            if (possiblebnodemappings.containsKey(bn1)) {
                bn1m = possiblebnodemappings.get(bn1);
            } else {
                bn1m = new LinkedHashMap<String, Integer>();
                possiblebnodemappings.put(bn1, bn1m);
            }
            Integer mappingcount = 0;
            if (bn1m.containsKey(bn2)) {
                mappingcount = bn1m.get(bn2);
            }
            bn1m.put(bn2, mappingcount + 1);
        }
    }

}
