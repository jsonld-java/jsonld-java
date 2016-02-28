package com.github.jsonldjava.core;

import com.github.jsonldjava.utils.JsonUtils;
import java.io.IOException;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.*;

public class JsonLdFramingTest {

    @Test
    public void testFrame0001() throws IOException, JsonLdError {
        Object frame = JsonUtils.fromInputStream(
                getClass().getResourceAsStream("/custom/frame-0001-frame.jsonld"));
        Object in = JsonUtils.fromInputStream(
                getClass().getResourceAsStream("/custom/frame-0001-in.jsonld"));

        Map<String, Object> frame2 = JsonLdProcessor.frame(in, frame, new JsonLdOptions());
        
        assertEquals(2, frame2.size());
    }

}
