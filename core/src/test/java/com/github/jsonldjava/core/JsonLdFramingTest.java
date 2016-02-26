package com.github.jsonldjava.core;

import com.github.jsonldjava.utils.JsonUtils;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

public class JsonLdFramingTest {

    @Test
    public void testFrame0001() throws IOException, JsonLdError {
        try {
            Object frame = JsonUtils.fromInputStream(
                    getClass().getResourceAsStream("/custom/frame-0001-frame.jsonld"));
            Object in = JsonUtils.fromInputStream(
                    getClass().getResourceAsStream("/custom/frame-0001-in.jsonld"));

            JsonLdProcessor.frame(in, frame, new JsonLdOptions());
        } catch (Throwable t) {
            t.printStackTrace();
            
            fail();
        }
    }

}
