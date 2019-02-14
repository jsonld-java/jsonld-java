package com.github.jsonldjava.core;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class ContextTest {

    @Test
    public void testRemoveBase() {
        // TODO: test if Context.removeBase actually works
    }

    // See https://github.com/jsonld-java/jsonld-java/issues/141

    @Test(expected = JsonLdError.class)
    public void testIssue141_errorOnEmptyKey_compact() {
        JsonLdProcessor.compact(ImmutableMap.of(),
                ImmutableMap.of("","http://example.com"), new JsonLdOptions());
    }

    @Test(expected = JsonLdError.class)
    public void testIssue141_errorOnEmptyKey_expand() {
        JsonLdProcessor.expand(ImmutableMap.of("@context",
                ImmutableMap.of("","http://example.com")), new JsonLdOptions());
    }

    @Test(expected = JsonLdError.class)
    public void testIssue141_errorOnEmptyKey_newContext1() {
        new Context(ImmutableMap.of("","http://example.com"));
    }

    @Test(expected = JsonLdError.class)
    public void testIssue141_errorOnEmptyKey_newContext2() {
        new Context(ImmutableMap.of("","http://example.com"), new JsonLdOptions());
    }

}
