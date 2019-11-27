package com.github.jsonldjava.core;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class DecimalLiteralCanonicalTest {
    
    @Test
    public void testDecimalIsNotCanonicalized() {
        double value = 6.5;
        
        Map<String, Object> innerMap = new HashMap<>();
        innerMap.put("@value", value);
        innerMap.put("@type", "http://www.w3.org/2001/XMLSchema#decimal");
        
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("ex:id", innerMap);
        
        JsonLdApi api = new JsonLdApi(jsonMap, new JsonLdOptions(""));
        RDFDataset dataset = api.toRDF();
        
        List<Object> defaultList = (List<Object>) dataset.get("@default");
        Map<String, Object> tripleMap = (Map<String, Object>) defaultList.get(0);
        Map<String, String> objectMap = (Map<String, String>) tripleMap.get("object");
        
        assertEquals("http://www.w3.org/2001/XMLSchema#decimal", objectMap.get("datatype"));
        assertEquals(Double.toString(value), objectMap.get("value"));
    }
}
