package com.github.jsonldjava.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.github.jsonldjava.utils.JsonUtils;
import org.junit.Test;

public class ContextFramingTest {

    // @Ignore("Disable until schema.org is fixed")
    @Test
    public void testFraming() throws Exception {

        final Map<String, Object> contextAbbrevs = new HashMap<String, Object>();
        contextAbbrevs.put("so", "http://schema.org/");

        final Map<String, Object> json = new HashMap<String, Object>();
        json.put("@context", contextAbbrevs);
        json.put("@id", "http://example.org/my_work");

        final List<Object> types = new LinkedList<Object>();
        types.add("so:CreativeWork");

        json.put("@type", types);
        json.put("so:name", "My Work");
        json.put("so:url", "http://example.org/my_work");

        final JsonLdOptions options = new JsonLdOptions();
        options.setBase("http://schema.org/");
        options.setCompactArrays(true);
        options.setOmitGraph(true);

        // System.out.println("Before compact");
        // System.out.println(JsonUtils.toPrettyString(json));

        final String frameStr = "{\"@id\": \"http://schema.org/myid\", \"@context\": \"http://schema.org/\"}";
        final Object frame = JsonUtils.fromString(frameStr);

        final Map<String, Object> compacted = JsonLdProcessor.frame(json, frame, options);

        // System.out.println("\n\nAfter compact:");
        // System.out.println(JsonUtils.toPrettyString(compacted));

        assertTrue("Framing removed the context", compacted.containsKey("@context"));
        assertFalse("Framing of context should be a string, not a list",
                compacted.get("@context") instanceof List);
    }

    @Test
    public void testFramingRemoteContext() throws Exception {
        final String jsonString = "{\"@id\": \"http://schema.org/myid\", \"@type\": [\"http://schema.org/Person\"]}";
        final String frameStr = "{\"@id\": \"http://schema.org/myid\", \"@context\": \"http://schema.org/\"}";

        final Object json = JsonUtils.fromString(jsonString);
        final Object frame = JsonUtils.fromString(frameStr);

        final JsonLdOptions options = new JsonLdOptions();
        options.setOmitGraph(true);

        final Map<String, Object> compacted = JsonLdProcessor.frame(json, frame, options);

        // System.out.println("\n\nAfter compact:");
        // System.out.println(JsonUtils.toPrettyString(compacted));

        assertEquals("Wrong framing context", "http://schema.org/", compacted.get("@context"));
        assertEquals("Wrong framing id", "schema:myid", compacted.get("id"));
        assertEquals("Wrong framing type", "Person", compacted.get("type"));
        assertEquals("Wrong number of Json entries",3, compacted.size());
    }

}
