package com.github.jsonldjava.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

import javax.swing.text.html.HTMLEditorKit.Parser;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * A bunch of functions to make loading JSON easy
 * 
 * @author tristan
 * 
 */
public class JSONUtils {
    public static Object fromString(String jsonString) throws JsonParseException, JsonMappingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Object rval = null;
        if (jsonString.trim().startsWith("[")) {
            try {
                rval = objectMapper.readValue(jsonString, List.class);
            } catch (IOException e) {
                // TODO: what?
                if (e instanceof JsonParseException) {
                    throw (JsonParseException) e;
                } else if (e instanceof JsonMappingException) {
                    throw (JsonMappingException) e;
                } else {
                    // TODO: Auto-generated catch block
                    e.printStackTrace();
                }
            }
        } else if (jsonString.trim().startsWith("{")) {
            try {
                rval = objectMapper.readValue(jsonString, Map.class);
            } catch (IOException e) {
                if (e instanceof JsonParseException) {
                    throw (JsonParseException) e;
                } else if (e instanceof JsonMappingException) {
                    throw (JsonMappingException) e;
                } else {
                    // TODO: Auto-generated catch block
                    e.printStackTrace();
                }
            }
        } else if (jsonString.trim().startsWith("\"")) {
            try {
                rval = objectMapper.readValue(jsonString, String.class);
            } catch (IOException e) {
                if (e instanceof JsonParseException) {
                    throw (JsonParseException) e;
                } else if (e instanceof JsonMappingException) {
                    throw (JsonMappingException) e;
                } else {
                    // TODO: Auto-generated catch block
                    e.printStackTrace();
                }
            }
        } else if (jsonString.trim().equals("true") || (jsonString.trim().equals("false"))) {
            try {
                rval = objectMapper.readValue(jsonString, Boolean.class);
            } catch (IOException e) {
                if (e instanceof JsonParseException) {
                    throw (JsonParseException) e;
                } else if (e instanceof JsonMappingException) {
                    throw (JsonMappingException) e;
                } else {
                    // TODO: Auto-generated catch block
                    e.printStackTrace();
                }
            }
        } else if (jsonString.trim().matches("[0-9.e+-]+")) {
            try {
                rval = objectMapper.readValue(jsonString, Number.class);
            } catch (IOException e) {
                if (e instanceof JsonParseException) {
                    throw (JsonParseException) e;
                } else if (e instanceof JsonMappingException) {
                    throw (JsonMappingException) e;
                } else {
                    // TODO: Auto-generated catch block
                    e.printStackTrace();
                }
            }
        } else if (jsonString.trim().equals("null")) {
            rval = null;
        } else {
            throw new JsonParseException("document doesn't start with a valid json element", new JsonLocation("\""
                    + jsonString.substring(0, Math.min(jsonString.length(), 100)) + "...\"", 0, 1, 0));
        }
        return rval;
    }

    public static Object fromReader(Reader r) throws IOException {
        StringBuffer sb = new StringBuffer();
        int b;
        while ((b = r.read()) != -1) {
            sb.append((char) b);
        }
        return fromString(sb.toString());
    }

    public static void write(Writer w, Object jsonObject) throws JsonGenerationException, JsonMappingException, IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.getJsonFactory().disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        objectMapper.writeValue(w, jsonObject);
    }

    public static void writePrettyPrint(Writer w, Object jsonObject) throws JsonGenerationException, JsonMappingException, IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.getJsonFactory().disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        ObjectWriter objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
        
        objectWriter.writeValue(w, jsonObject);
    }

    public static Object fromInputStream(InputStream content) throws IOException {
        return fromInputStream(content, "UTF-8"); // no readers from
                                                  // inputstreams w.o.
                                                  // encoding!!
    }

    public static Object fromInputStream(InputStream content, String enc) throws IOException {
        return fromReader(new BufferedReader(new InputStreamReader(content, enc)));
    }

    public static String toPrettyString(Object obj) {
        StringWriter sw = new StringWriter();
        try {
            writePrettyPrint(sw, obj);
        } catch (Exception e) {
            // TODO Is this really possible with stringwriter?
            // I think it's only there because of the interface
            // however, if so... well, we have to do something!
            // it seems weird for toString to throw an IOException
        }
        return sw.toString();
    }

    public static String toString(Object obj) { // throws
                                                // JsonGenerationException,
                                                // JsonMappingException {
        StringWriter sw = new StringWriter();
        try {
            write(sw, obj);
        } catch (Exception e) {
            // TODO Is this really possible with stringwriter?
            // I think it's only there because of the interface
            // however, if so... well, we have to do something!
            // it seems weird for toString to throw an IOException
        }
        return sw.toString();
    }

    public static boolean equals(Object v1, Object v2) {
    	return v1 == null ? v2 == null : v1.equals(v2);
    }

    public static Object fromURL(java.net.URL url) throws JsonParseException,
            IOException {
        new MappingJsonFactory();
        MappingJsonFactory jsonFactory = new MappingJsonFactory();
        URLConnection conn = url.openConnection();
        // For content negotiation
        conn.addRequestProperty("Accept", "application/ld+json);");
        // Fallbacks                       
        conn.addRequestProperty("Accept", "application/json;q=0.9, "
                        + "application/javascript;q=0.5, text/javascript;q=0.5, text/plain;q=0.2, "
                        + "*/*;q=0.1");
        InputStream in = conn.getInputStream();
        try {
            JsonParser parser = jsonFactory.createParser(in);
            JsonToken token = parser.nextToken();
            Class<?> type;
            if (token == JsonToken.START_OBJECT) {
                type = Map.class;
            } else if(token == JsonToken.START_ARRAY) {
                type = List.class;
            } else {
                type = String.class;
            }
            try {
                return parser.readValueAs(type);
            } finally {
                parser.close();
            }
        } finally {
            in.close();
        }
    }
}
