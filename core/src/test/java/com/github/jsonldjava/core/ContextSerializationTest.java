package com.github.jsonldjava.core;

import com.github.jsonldjava.utils.JsonUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContextSerializationTest {

    // Added in order to have some coverage on the serialize method since is not used anywhere.
    @Test
    void serializeTest() throws IOException {
        final Map<String, Object> json = (Map<String, Object>) JsonUtils
                .fromInputStream(getClass().getResourceAsStream("/custom/contexttest-0005.jsonld"));

        final Map<String, Object> contextValue = (Map<String, Object>)json.get(JsonLdConsts.CONTEXT);
        final Map<String, Object> serializedContext = new Context().parse(contextValue).serialize();

        assertEquals(json, serializedContext, "Wrong serialized context");
    }
}
