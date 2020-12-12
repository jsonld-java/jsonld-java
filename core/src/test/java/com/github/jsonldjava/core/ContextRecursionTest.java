package com.github.jsonldjava.core;

import com.github.jsonldjava.utils.JsonUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public class ContextRecursionTest {

    @BeforeClass
    public static void setup() {
        System.setProperty(DocumentLoader.DISALLOW_REMOTE_CONTEXT_LOADING, "true");
    }

    @AfterClass
    public static void tearDown() {
        System.setProperty(DocumentLoader.DISALLOW_REMOTE_CONTEXT_LOADING, "false");
    }

    @Test
    public void testAllowedRecursion() throws IOException {

        final String contextB = "{\"@context\": [\"http://localhost/d\", {\"b\": \"http://localhost/b\"} ] }";
        final String contextC = "{\"@context\": [\"http://localhost/d\", {\"c\": \"http://localhost/c\"} ] }";
        final String contextD = "{\"@context\": [\"http://localhost/e\", {\"d\": \"http://localhost/d\"} ] }";
        final String contextE = "{\"@context\": {\"e\": \"http://localhost/e\"} }";

        final DocumentLoader dl = new DocumentLoader();
        dl.addInjectedDoc("http://localhost/b", contextB);
        dl.addInjectedDoc("http://localhost/c", contextC);
        dl.addInjectedDoc("http://localhost/d", contextD);
        dl.addInjectedDoc("http://localhost/e", contextE);
        final JsonLdOptions options = new JsonLdOptions();
        options.setDocumentLoader(dl);

        final String jsonString = "{\"@context\": [\"http://localhost/d\", \"http://localhost/b\", \"http://localhost/c\", {\"a\": \"http://localhost/a\"} ], \"a\": \"A\", \"b\": \"B\", \"c\": \"C\", \"d\": \"D\"}";
        final Object json = JsonUtils.fromString(jsonString);
        final Object expanded = JsonLdProcessor.expand(json, options);
        assertEquals(
                "[{http://localhost/a=[{@value=A}], http://localhost/b=[{@value=B}], http://localhost/c=[{@value=C}], http://localhost/d=[{@value=D}]}]",
                expanded.toString());
    }

    @Test
    public void testCyclicRecursion() throws IOException {

        final String contextC = "{\"@context\": [\"http://localhost/d\", {\"c\": \"http://localhost/c\"} ] }";
        final String contextD = "{\"@context\": [\"http://localhost/e\", {\"d\": \"http://localhost/d\"} ] }";
        final String contextE = "{\"@context\": [\"http://localhost/c\", {\"e\": \"http://localhost/e\"} ] }";

        final DocumentLoader dl = new DocumentLoader();
        dl.addInjectedDoc("http://localhost/c", contextC);
        dl.addInjectedDoc("http://localhost/d", contextD);
        dl.addInjectedDoc("http://localhost/e", contextE);
        final JsonLdOptions options = new JsonLdOptions();
        options.setDocumentLoader(dl);

        final String jsonString = "{\"@context\": [\"http://localhost/c\", {\"a\": \"http://localhost/a\"} ]}";
        final Object json = JsonUtils.fromString(jsonString);
        try {
            JsonLdProcessor.expand(json, options);
            fail("it should throw");
        } catch(JsonLdError err) {
            assertEquals(err.getType(), JsonLdError.Error.RECURSIVE_CONTEXT_INCLUSION);
            assertEquals(err.getMessage(), "recursive context inclusion: http://localhost/c");
        }
    }

}
