package com.github.jsonldjava.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.github.jsonldjava.utils.JsonUtils;
import org.junit.jupiter.api.Test;

public class ContextFlatteningTest {

    @Test
    public void testFlatenning() throws Exception {

        final Map<String, Object> contextAbbrevs = new HashMap<>();
        contextAbbrevs.put("so", "http://schema.org/");

        final Map<String, Object> json = new HashMap<>();
        json.put("@context", contextAbbrevs);
        json.put("@id", "http://example.org/my_work");

        final List<Object> types = new LinkedList<>();
        types.add("so:CreativeWork");

        json.put("@type", types);
        json.put("so:name", "My Work");
        json.put("so:url", "http://example.org/my_work");

        final JsonLdOptions options = new JsonLdOptions();
        options.setBase("http://schema.org/");
        options.setCompactArrays(true);
        options.setOmitGraph(true);

        final String flattenStr = "{\"@id\": \"http://schema.org/myid\", \"@context\": \"http://schema.org/\"}";
        final Object flatten = JsonUtils.fromString(flattenStr);

        final Map<String, Object> flattened = ((Map<String, Object>)JsonLdProcessor.flatten(json, flatten, options));

        assertTrue(flattened.containsKey("@context"),"Flattening removed the context");
        assertFalse(flattened.get("@context") instanceof List,
                "Flattening of context should be a string, not a list");
    }

    @Test
    public void testFlatteningRemoteContext() throws Exception {
        final String jsonString =
                "{\"@context\": {\"@vocab\": \"http://schema.org/\"}, \"knows\": [{\"name\": \"a\"}, {\"name\": \"b\"}] }";
        final String flattenStr = "{\"@context\": \"http://schema.org/\"}";

        final Object json = JsonUtils.fromString(jsonString);
        final Object flatten = JsonUtils.fromString(flattenStr);

        final JsonLdOptions options = new JsonLdOptions();
        options.setOmitGraph(true);

        final Map<String, Object> flattened = ((Map<String, Object>)JsonLdProcessor.flatten(json, flatten, options));

        assertEquals("http://schema.org/", flattened.get("@context"),"Wrong returned context");
        assertEquals(2, flattened.size(),"Wrong number of Json entries");
    }

}
