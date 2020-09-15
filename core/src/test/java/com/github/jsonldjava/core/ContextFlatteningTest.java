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

        // System.out.println("Before flattening");
        // System.out.println(JsonUtils.toPrettyString(json));

        final String flattenStr = "{\"@id\": \"http://schema.org/myid\", \"@context\": \"http://schema.org/\"}";
        final Object flatten = JsonUtils.fromString(flattenStr);

        final Map<String, Object> flattened = ((Map<String, Object>)JsonLdProcessor.flatten(json, flatten, options));

        // System.out.println("\n\nAfter flattening:");
        // System.out.println(JsonUtils.toPrettyString(flattened));

        assertTrue("Flattening removed the context", flattened.containsKey("@context"));
        assertFalse("Flattening of context should be a string, not a list",
                flattened.get("@context") instanceof List);
    }

    @Test
    public void testFlatteningRemoteContext() throws Exception {
        final String jsonString =
                "{\"@context\": {\"@vocab\": \"http://schema.org/\"}, \"knows\": [{\"name\": \"a\"}, {\"name\": \"b\"}] }";
        final String flattenStr = "{\"@context\": \"http://schema.org/\"}";

        final Object json = JsonUtils.fromString(jsonString);
        final Object flatten = JsonUtils.fromString(flattenStr);

         // System.out.println("Before flattening");
         // System.out.println(JsonUtils.toPrettyString(json));

        final JsonLdOptions options = new JsonLdOptions();
        options.setOmitGraph(true);

        final Map<String, Object> flattened = ((Map<String, Object>)JsonLdProcessor.flatten(json, flatten, options));

         // System.out.println("\n\nAfter flattened:");
         // System.out.println(JsonUtils.toPrettyString(flattened));

        assertEquals("Wrong returned context", "http://schema.org/", flattened.get("@context"));
        assertEquals("Wrong number of Json entries",2, flattened.size());
    }

}
