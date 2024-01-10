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

public class ContextFramingTest {

    @Test
    public void testFraming() throws Exception {

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

        final String frameStr = "{\"@id\": \"http://schema.org/myid\", \"@context\": \"http://schema.org/\"}";
        final Object frame = JsonUtils.fromString(frameStr);

        final Map<String, Object> framed = JsonLdProcessor.frame(json, frame, options);

        assertTrue(framed.containsKey("@context"), "Framing removed the context");
        assertFalse(framed.get("@context") instanceof List,
                "Framing of context should be a string, not a list");
    }

    @Test
    public void testFramingRemoteContext() throws Exception {
        final String jsonString = "{\"@id\": \"http://schema.org/myid\", \"@type\": [\"http://schema.org/Person\"]}";
        final String frameStr = "{\"@id\": \"http://schema.org/myid\", \"@context\": \"http://schema.org/\"}";

        final Object json = JsonUtils.fromString(jsonString);
        final Object frame = JsonUtils.fromString(frameStr);

        final JsonLdOptions options = new JsonLdOptions();
        options.setOmitGraph(true);

        final Map<String, Object> framed = JsonLdProcessor.frame(json, frame, options);

        assertEquals("http://schema.org/", framed.get("@context"),"Wrong returned context");
        assertEquals("schema:myid", framed.get("id"),"Wrong id");
        assertEquals( "Person", framed.get("type"),"Wrong type");
        assertEquals(3, framed.size(),"Wrong number of Json entries");
    }

}
