package com.github.jsonldjava.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

class ContextTest {

    @Test
    void removeBase() {
        // TODO: test if Context.removeBase actually works
    }

    // See https://github.com/jsonld-java/jsonld-java/issues/141

    @Test
    void issue141_errorOnEmptyKey_compact() {
        assertThrows(JsonLdError.class, () -> {
            JsonLdProcessor.compact(ImmutableMap.of(), ImmutableMap.of("", "http://example.com"),
                    new JsonLdOptions());
        });
    }

    @Test
    void issue141_errorOnEmptyKey_expand() {
        assertThrows(JsonLdError.class, () -> {
            JsonLdProcessor.expand(
                    ImmutableMap.of("@context", ImmutableMap.of("", "http://example.com")),
                    new JsonLdOptions());
        });
    }

    @Test
    void issue141_errorOnEmptyKey_newContext1() {
        assertThrows(JsonLdError.class, () -> {
            new Context(ImmutableMap.of("", "http://example.com"));
        });
    }

    @Test
    void issue141_errorOnEmptyKey_newContext2() {
        assertThrows(JsonLdError.class, () -> {
            new Context(ImmutableMap.of("", "http://example.com"), new JsonLdOptions());
        });
    }

    /*
     * schema.org documentation says some properties can be either Text or URL,
     * but sets `@type : @id` in the context, e.g. for
     * https://schema.org/roleName:
     */
    Map<String, Object> schemaOrg = ImmutableMap.of("roleName",
            ImmutableMap.of("@id", "http://schema.org/roleName", "@type", "@id"));

    // See https://github.com/jsonld-java/jsonld-java/issues/248

    @Test
    void issue248_uriExpected() {
        assertThrows(IllegalArgumentException.class, () -> {
            JsonLdProcessor
                    .expand(ImmutableMap.of("roleName", "Production Company", "@context", schemaOrg));
        });
    }

    @Test
    void issue248_forceValue() {
        final List<?> value = Arrays.asList(ImmutableMap.of("@value", "Production Company"));
        final Map<String, Object> input = ImmutableMap.of("roleName", value, "@context", schemaOrg);
        final Object output = JsonLdProcessor.expand(input);
        assertEquals("[{http://schema.org/roleName=[{@value=Production Company}]}]",
                output.toString());
    }

    @Test
    void issue248_overrideContext() {
        final List<?> context = Arrays.asList(schemaOrg,
                ImmutableMap.of("roleName", ImmutableMap.of("@id", "http://schema.org/roleName")));
        final Map<String, Object> input = ImmutableMap.of("roleName", "Production Company",
                "@context", context);
        final Object output = JsonLdProcessor.expand(input);
        assertEquals("[{http://schema.org/roleName=[{@value=Production Company}]}]",
                output.toString());
    }

}
