package com.github.jsonldjava.utils;


import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class JsonUtilsTest {
    @Test
    void resolveTest() {
        final String baseUri = "http://mysite.net";
        final String pathToResolve = "picture.jpg";
        String resolve = "";

        try {
            resolve = JsonLdUrl.resolve(baseUri, pathToResolve);
            assertEquals(baseUri + "/" + pathToResolve, resolve);
        } catch (final Exception e) {
            assertTrue(false);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void fromStringTest() {
        final String testString = "{\"seq\":3,\"id\":\"e48dfa735d9fad88db6b7cd696002df7\",\"changes\":[{\"rev\":\"2-6aebf275bc3f29b67695c727d448df8e\"}]}";
        final String testFailure = "{{{{{{{{{{{";
        Object obj = null;

        try {
            obj = JsonUtils.fromString(testString);
            assertTrue(((Map<String, Object>) obj).containsKey("seq"));
            assertTrue(((Map<String, Object>) obj).get("seq") instanceof Number);
        } catch (final Exception e) {
            assertTrue(false);
        }

        try {
            obj = JsonUtils.fromString(testFailure);
            assertTrue(false);
        } catch (final Exception e) {
            assertTrue(true);
        }
    }

    @Test
    void fromJsonParser() throws Exception {
        final ObjectMapper jsonMapper = new ObjectMapper();
        final JsonFactory jsonFactory = new JsonFactory(jsonMapper);
        final Reader testInputString = new StringReader("{}");
        final JsonParser jp = jsonFactory.createParser(testInputString);
        JsonUtils.fromJsonParser(jp);
    }

    @Test
    void trailingContent_1() throws JsonParseException, IOException {
        trailingContent("{}");
    }

    @Test
    void trailingContent_2() throws JsonParseException, IOException {
        trailingContent("{}  \t  \r \n  \r\n   ");
    }

    @Test
    void trailingContent_3() throws JsonParseException, IOException {
        assertThrows(JsonParseException.class, () -> {
            trailingContent("{}x");
        });
    }

    @Test
    void trailingContent_4() throws JsonParseException, IOException {
        assertThrows(JsonParseException.class, () -> {
            trailingContent("{}   x");
        });
    }

    @Test
    void trailingContent_5() throws JsonParseException, IOException {
        assertThrows(JsonParseException.class, () -> {
            trailingContent("{} \"x\"");
        });
    }

    @Test
    void trailingContent_6() throws JsonParseException, IOException {
        assertThrows(JsonParseException.class, () -> {
            trailingContent("{} {}");
        });
    }

    @Test
    void trailingContent_7() throws JsonParseException, IOException {
        assertThrows(JsonParseException.class, () -> {
            trailingContent("{},{}");
        });
    }

    @Test
    void trailingContent_8() throws JsonParseException, IOException {
        assertThrows(JsonParseException.class, () -> {
            trailingContent("{},[]");
        });
    }

    private void trailingContent(String string) throws JsonParseException, IOException {
        JsonUtils.fromString(string);
    }
}
