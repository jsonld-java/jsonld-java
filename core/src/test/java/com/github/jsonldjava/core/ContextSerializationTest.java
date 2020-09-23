package com.github.jsonldjava.core;

import com.github.jsonldjava.utils.JsonUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ContextSerializationTest {

    @Test
    // Added in order to have some coverage on the serialize method since is not used anywhere.
    public void serializeTest() throws IOException {
        final Map<String, Object> json = (Map<String, Object>)JsonUtils
                .fromInputStream(getClass().getResourceAsStream("/custom/contexttest-0005.jsonld"));

        final Map<String, Object> contextValue = (Map<String, Object>)json.get(JsonLdConsts.CONTEXT);
        final Map<String, Object> serializedContext = new Context().parse(contextValue).serialize();

        assertEquals("Wrong serialized context", json, serializedContext);
    }
}
