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

        // System.out.println("Before framing");
        // System.out.println(JsonUtils.toPrettyString(json));

        final String frameStr = "{\"@id\": \"http://schema.org/myid\", \"@context\": \"http://schema.org/\"}";
        final Object frame = JsonUtils.fromString(frameStr);

        final Map<String, Object> framed = JsonLdProcessor.frame(json, frame, options);

        // System.out.println("\n\nAfter framing:");
        // System.out.println(JsonUtils.toPrettyString(framed));

        assertTrue("Framing removed the context", framed.containsKey("@context"));
        assertFalse("Framing of context should be a string, not a list",
                framed.get("@context") instanceof List);
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

        // System.out.println("\n\nAfter framing:");
        // System.out.println(JsonUtils.toPrettyString(framed));

        assertEquals("Wrong framing context", "http://schema.org/", framed.get("@context"));
        assertEquals("Wrong framing id", "schema:myid", framed.get("id"));
        assertEquals("Wrong framing type", "Person", framed.get("type"));
        assertEquals("Wrong number of Json entries",3, framed.size());
    }

}
