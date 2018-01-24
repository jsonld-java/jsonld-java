package com.github.jsonldjava.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.github.jsonldjava.utils.JsonUtils;

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
    public void testCompactValue() throws Exception {
        final InputStream contexttest = getClass().getResourceAsStream("/custom/compact-0001-in.jsonld");
        assertNotNull(contexttest);
        Object jsonld = JsonUtils.fromInputStream(contexttest);
        System.out.println("Input:");
        System.out.println(JsonUtils.toPrettyString(jsonld));
        
        JsonLdOptions options = new JsonLdOptions();
        options.setCompactArrays(false);
        
        //Map<String, Object> context = new HashMap<>();
    
        Object compact = JsonLdProcessor.compact(jsonld, null, options);
        System.out.println("Output:");
        System.out.println(JsonUtils.toPrettyString(compact));
    }

    @Test
    public void testCompactValueCustom0001() throws Exception {
        final InputStream input = getClass().getResourceAsStream("/custom/compact-0001-in.jsonld");
        assertNotNull(input);
        Object jsonld = JsonUtils.fromInputStream(input);
        System.out.println("Input:");
        System.out.println(JsonUtils.toPrettyString(jsonld));
        
        JsonLdOptions options = new JsonLdOptions();
        options.setCompactArrays(true);
        
        final InputStream contexttest = getClass().getResourceAsStream("/custom/compact-0001-context.jsonld");
        assertNotNull(contexttest);
        Object context = JsonUtils.fromInputStream(contexttest);
        System.out.println("Context:");
        System.out.println(JsonUtils.toPrettyString(context));
    
        Object compact = JsonLdProcessor.compact(jsonld, context, options);
        System.out.println("Output:");
        System.out.println(JsonUtils.toPrettyString(compact));
    }
    
    @Test
    public void testCompactValue0011() throws Exception {
        final InputStream input = getClass().getResourceAsStream("/json-ld.org/compact-0011-in.jsonld");
        assertNotNull(input);
        Object jsonld = JsonUtils.fromInputStream(input);
        System.out.println("Input:");
        System.out.println(JsonUtils.toPrettyString(jsonld));
        
        JsonLdOptions options = new JsonLdOptions();
        options.setCompactArrays(true);
        
        final InputStream contexttest = getClass().getResourceAsStream("/json-ld.org/compact-0011-context.jsonld");
        assertNotNull(contexttest);
        Object context = JsonUtils.fromInputStream(contexttest);
        System.out.println("Context:");
        System.out.println(JsonUtils.toPrettyString(context));
        //Map<String, Object> context = new HashMap<>();
    
        Object compact = JsonLdProcessor.compact(jsonld, context, options);
        System.out.println("Output:");
        System.out.println(JsonUtils.toPrettyString(compact));
    }
}
