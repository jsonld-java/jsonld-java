package com.github.jsonldjava.core;

import static org.junit.Assert.assertEquals;

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

}
