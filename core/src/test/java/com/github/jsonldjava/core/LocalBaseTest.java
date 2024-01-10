package com.github.jsonldjava.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.jsonldjava.utils.JsonUtils;

class LocalBaseTest {
    @Test
    void mixedLocalRemoteBaseRemoteContextFirst() throws Exception {

        final Reader reader = new BufferedReader(new InputStreamReader(
                this.getClass().getResourceAsStream("/custom/base-0001-in.jsonld"),
                Charset.forName("UTF-8")));
        final Object context = JsonUtils.fromReader(reader);
        assertNotNull(context);

        final JsonLdOptions options = new JsonLdOptions();
        final Object expanded = JsonLdProcessor.expand(context, options);
        // System.out.println(JsonUtils.toPrettyString(expanded));

        final Reader outReader = new BufferedReader(new InputStreamReader(
                this.getClass().getResourceAsStream("/custom/base-0001-out.jsonld"),
                Charset.forName("UTF-8")));
        final Object output = JsonUtils.fromReader(outReader);
        assertNotNull(output);
        assertEquals(expanded, output);
    }

    @Test
    void mixedLocalRemoteBaseLocalContextFirst() throws Exception {

        final Reader reader = new BufferedReader(new InputStreamReader(
                this.getClass().getResourceAsStream("/custom/base-0002-in.jsonld"),
                Charset.forName("UTF-8")));
        final Object context = JsonUtils.fromReader(reader);
        assertNotNull(context);

        final JsonLdOptions options = new JsonLdOptions();
        final Object expanded = JsonLdProcessor.expand(context, options);
        // System.out.println(JsonUtils.toPrettyString(expanded));

        final Reader outReader = new BufferedReader(new InputStreamReader(
                this.getClass().getResourceAsStream("/custom/base-0002-out.jsonld"),
                Charset.forName("UTF-8")));
        final Object output = JsonUtils.fromReader(outReader);
        assertNotNull(output);
        assertEquals(expanded, output);
    }

    @Test
    void uriResolveWhenExpandingBase() throws Exception {

        final Reader reader = new BufferedReader(new InputStreamReader(
                this.getClass().getResourceAsStream("/custom/base-0003-in.jsonld"),
                Charset.forName("UTF-8")));
        final Object input = JsonUtils.fromReader(reader);
        assertNotNull(input);

        final JsonLdOptions options = new JsonLdOptions();
        final List<Object> expanded = JsonLdProcessor.expand(input, options);
        assertFalse(expanded.isEmpty(), "expanded form must not be empty");

        final Reader outReader = new BufferedReader(new InputStreamReader(
                this.getClass().getResourceAsStream("/custom/base-0003-out.jsonld"),
                Charset.forName("UTF-8")));
        final Object expected = JsonLdProcessor.expand(JsonUtils.fromReader(outReader), options);
        assertNotNull(expected);
        assertEquals(expected, expanded);
    }

}
