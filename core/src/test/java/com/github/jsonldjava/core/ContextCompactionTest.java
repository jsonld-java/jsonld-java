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


public class ContextCompactionTest {

    @Test
    public void testCompaction() {

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

        final List<String> newContexts = new LinkedList<String>();
        newContexts.add("http://schema.org/");
        final Map<String, Object> compacted = JsonLdProcessor.compact(json, newContexts, options);

        assertTrue(compacted.containsKey("@context"), "Compaction removed the context");
        assertFalse(compacted.get("@context") instanceof List,
                "Compaction of context should be a string, not a list");
    }

    @Test
    public void testCompactionSingleRemoteContext() throws Exception {
        final String jsonString = "[{\"@type\": [\"http://schema.org/Person\"] } ]";
        final String ctxStr = "{\"@context\": \"http://schema.org/\"}";

        final Object json = JsonUtils.fromString(jsonString);
        final Object ctx = JsonUtils.fromString(ctxStr);

        final JsonLdOptions options = new JsonLdOptions();

        final Map<String, Object> compacted = JsonLdProcessor.compact(json, ctx, options);

        assertEquals("http://schema.org/", compacted.get("@context"),"Wrong returned context");
        assertEquals( "Person", compacted.get("type"),"Wrong type");
        assertEquals(2, compacted.size(),"Wrong number of Json entries");
    }

}
