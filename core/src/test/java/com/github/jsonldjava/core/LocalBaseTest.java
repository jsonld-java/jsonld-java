package com.github.jsonldjava.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.github.jsonldjava.utils.JsonUtils;

public class LocalBaseTest {
    @Ignore("TODO: context does no more resolve - see https://github.com/jsonld-java/jsonld-java/issues/175")
    @Test
    public void testMixedLocalRemoteBaseRemoteContextFirst() throws Exception {

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

    @Ignore("TODO: context does no more resolve - see https://github.com/jsonld-java/jsonld-java/issues/175")
    @Test
    public void testMixedLocalRemoteBaseLocalContextFirst() throws Exception {

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
    public void testUriResolveWhenExpandingBase() throws Exception {

        final Reader reader = new BufferedReader(new InputStreamReader(
                this.getClass().getResourceAsStream("/custom/base-0003-in.jsonld"),
                Charset.forName("UTF-8")));
        final Object input = JsonUtils.fromReader(reader);
        assertNotNull(input);

        final JsonLdOptions options = new JsonLdOptions();
        final List<Object> expanded = JsonLdProcessor.expand(input, options);
        assertFalse("expanded form must not be empty", expanded.isEmpty());

        final Reader outReader = new BufferedReader(new InputStreamReader(
                this.getClass().getResourceAsStream("/custom/base-0003-out.jsonld"),
                Charset.forName("UTF-8")));
        final Object expected = JsonLdProcessor.expand(JsonUtils.fromReader(outReader), options);
        assertNotNull(expected);
        assertEquals(expected, expanded);
    }

}
