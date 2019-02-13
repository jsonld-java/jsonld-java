package com.github.jsonldjava.utils;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtilsTest {

    @SuppressWarnings("unchecked")
    @Test
    public void fromStringTest() {
        final String testString = "{\"seq\":3,\"id\":\"e48dfa735d9fad88db6b7cd696002df7\",\"changes\":[{\"rev\":\"2-6aebf275bc3f29b67695c727d448df8e\"}]}";
        final String testFailure = "{{{{{{{{{{{";
        Object obj = null;

        try {
            obj = JsonUtils.fromString(testString);
        } catch (final Exception e) {
            assertTrue(false);
        }

        assertTrue(((Map<String, Object>) obj).containsKey("seq"));
        assertTrue(((Map<String, Object>) obj).get("seq") instanceof Number);

        try {
            obj = JsonUtils.fromString(testFailure);
            assertTrue(false);
        } catch (final Exception e) {
            assertTrue(true);
        }
    }

    @Test
    public void testFromJsonParser() throws Exception {
        final ObjectMapper jsonMapper = new ObjectMapper();
        final JsonFactory jsonFactory = new JsonFactory(jsonMapper);
        final Reader testInputString = new StringReader("{}");
        final JsonParser jp = jsonFactory.createParser(testInputString);
        JsonUtils.fromJsonParser(jp);
    }

    @Test
    public void trailingContent_1() throws JsonParseException, IOException {
        trailingContent("{}");
    }

    @Test
    public void trailingContent_2() throws JsonParseException, IOException {
        trailingContent("{}  \t  \r \n  \r\n   ");
    }

    @Test(expected = JsonParseException.class)
    public void trailingContent_3() throws JsonParseException, IOException {
        trailingContent("{}x");
    }

    @Test(expected = JsonParseException.class)
    public void trailingContent_4() throws JsonParseException, IOException {
        trailingContent("{}   x");
    }

    @Test(expected = JsonParseException.class)
    public void trailingContent_5() throws JsonParseException, IOException {
        trailingContent("{} \"x\"");
    }

    @Test(expected = JsonParseException.class)
    public void trailingContent_6() throws JsonParseException, IOException {
        trailingContent("{} {}");
    }

    @Test(expected = JsonParseException.class)
    public void trailingContent_7() throws JsonParseException, IOException {
        trailingContent("{},{}");
    }

    @Test(expected = JsonParseException.class)
    public void trailingContent_8() throws JsonParseException, IOException {
        trailingContent("{},[]");
    }

    private void trailingContent(String string) throws JsonParseException, IOException {
        JsonUtils.fromString(string);
    }
}
