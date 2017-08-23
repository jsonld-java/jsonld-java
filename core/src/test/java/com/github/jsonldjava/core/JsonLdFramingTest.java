package com.github.jsonldjava.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.Map;

import org.junit.Test;

import com.github.jsonldjava.utils.JsonUtils;

public class JsonLdFramingTest {

    @Test
    public void testFrame0001() throws IOException, JsonLdError {
        final Object frame = JsonUtils
                .fromInputStream(getClass().getResourceAsStream("/custom/frame-0001-frame.jsonld"));
        final Object in = JsonUtils
                .fromInputStream(getClass().getResourceAsStream("/custom/frame-0001-in.jsonld"));

        final Map<String, Object> frame2 = JsonLdProcessor.frame(in, frame, new JsonLdOptions());

        assertEquals(2, frame2.size());
    }

    @Test
    public void testFrame0002() throws IOException, JsonLdError {
        final Object frame = JsonUtils
                .fromInputStream(getClass().getResourceAsStream("/custom/frame-0002-frame.jsonld"));
        final Object in = JsonUtils
                .fromInputStream(getClass().getResourceAsStream("/custom/frame-0002-in.jsonld"));

        JsonLdOptions opts = new JsonLdOptions();
        opts.setCompactArrays(false);
		final Map<String, Object> frame2 = JsonLdProcessor.frame(in, frame, opts);

        final Object out = JsonUtils
                .fromInputStream(getClass().getResourceAsStream("/custom/frame-0002-out.jsonld"));
        // System.out.println(JsonUtils.toPrettyString(out));
        // System.out.println(JsonUtils.toPrettyString(frame2));
        assertEquals(out, frame2);
    }

    @Test
    public void testFrame0003() throws IOException, JsonLdError {
        final Object frame = JsonUtils
                .fromInputStream(getClass().getResourceAsStream("/custom/frame-0002-frame.jsonld"));
        final Object in = JsonUtils
                .fromInputStream(getClass().getResourceAsStream("/custom/frame-0002-in.jsonld"));

        JsonLdOptions opts = new JsonLdOptions();
        opts.setCompactArrays(false);
        opts.setProcessingMode("json-ld-1.1");
        final Map<String, Object> frame2 = JsonLdProcessor.frame(in, frame, opts);
        assertFalse("Result should contain no blank nodes", frame2.toString().contains("_:"));

        final Object out = JsonUtils
                .fromInputStream(getClass().getResourceAsStream("/custom/frame-0003-out.jsonld"));
        assertEquals(out, frame2);
    }

    @Test
    public void testFrame0004() throws IOException, JsonLdError {
        final Object frame = JsonUtils
                .fromInputStream(getClass().getResourceAsStream("/custom/frame-0004-frame.jsonld"));
        final Object in = JsonUtils
                .fromInputStream(getClass().getResourceAsStream("/custom/frame-0004-in.jsonld"));

        JsonLdOptions opts = new JsonLdOptions();
        opts.setCompactArrays(true);
        final Map<String, Object> frame2 = JsonLdProcessor.frame(in, frame, opts);

        final Object out = JsonUtils
                .fromInputStream(getClass().getResourceAsStream("/custom/frame-0004-out.jsonld"));
        assertEquals(out, frame2);
    }

    @Test
    public void testFrame0005() throws IOException, JsonLdError {
        final Object frame = JsonUtils
                .fromInputStream(getClass().getResourceAsStream("/custom/frame-0005-frame.jsonld"));
        final Object in = JsonUtils
                .fromInputStream(getClass().getResourceAsStream("/custom/frame-0005-in.jsonld"));

        JsonLdOptions opts = new JsonLdOptions();
        opts.setCompactArrays(true);
        final Map<String, Object> frame2 = JsonLdProcessor.frame(in, frame, opts);

        final Object out = JsonUtils
                .fromInputStream(getClass().getResourceAsStream("/custom/frame-0005-out.jsonld"));
        assertEquals(out, frame2);
    }

    @Test
    public void testFrame0006() throws IOException, JsonLdError {
        final Object frame = JsonUtils
                .fromInputStream(getClass().getResourceAsStream("/custom/frame-0006-frame.jsonld"));
        final Object in = JsonUtils
                .fromInputStream(getClass().getResourceAsStream("/custom/frame-0006-in.jsonld"));

        JsonLdOptions opts = new JsonLdOptions();
        opts.setCompactArrays(true);
        final Map<String, Object> frame2 = JsonLdProcessor.frame(in, frame, opts);

        final Object out = JsonUtils
                .fromInputStream(getClass().getResourceAsStream("/custom/frame-0006-out.jsonld"));
        assertEquals(out, frame2);
    }

    @Test
    public void testFrame0007() throws IOException, JsonLdError {
        final Object frame = JsonUtils
                .fromInputStream(getClass().getResourceAsStream("/custom/frame-0007-frame.jsonld"));
        final Object in = JsonUtils
                .fromInputStream(getClass().getResourceAsStream("/custom/frame-0007-in.jsonld"));

        JsonLdOptions opts = new JsonLdOptions();
        opts.setCompactArrays(true);
        final Map<String, Object> frame2 = JsonLdProcessor.frame(in, frame, opts);

        final Object out = JsonUtils
                .fromInputStream(getClass().getResourceAsStream("/custom/frame-0007-out.jsonld"));
        assertEquals(out, frame2);
    }
}
