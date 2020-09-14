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


public class ContextCompactionTest {

    // @Ignore("Disable until schema.org is fixed")
    @Test
    public void testCompaction() throws Exception {

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

        // System.out.println("Before compact");
        // System.out.println(JsonUtils.toPrettyString(json));

        final List<String> newContexts = new LinkedList<String>();
        newContexts.add("http://schema.org/");
        final Map<String, Object> compacted = JsonLdProcessor.compact(json, newContexts, options);

        // System.out.println("\n\nAfter compact:");
        // System.out.println(JsonUtils.toPrettyString(compacted));

        assertTrue("Compaction removed the context", compacted.containsKey("@context"));
        assertFalse("Compaction of context should be a string, not a list",
                compacted.get("@context") instanceof List);
    }

    @Test
    public void testCompactionUriSingleContext() throws Exception {
        final String jsonString = "[{\"@type\": [\"http://schema.org/Person\"] } ]";
        final String ctxStr = "{\"@context\": \"http://schema.org/\"}";

        final Object json = JsonUtils.fromString(jsonString);
        final Object ctx = JsonUtils.fromString(ctxStr);

        final JsonLdOptions options = new JsonLdOptions();

        final Map<String, Object> compacted = JsonLdProcessor.compact(json, ctx, options);

         // System.out.println("\n\nAfter compact:");
         // System.out.println(JsonUtils.toPrettyString(compacted));

        assertEquals("Wrong compaction context", "http://schema.org/", compacted.get("@context"));
    }

}
