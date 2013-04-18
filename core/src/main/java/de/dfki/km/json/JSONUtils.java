package de.dfki.km.json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
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
        boolean rval = true;
        // TODO Auto-generated method stub
        if (v1 instanceof List && v2 instanceof List) {
            if (((List) v1).size() != ((List) v2).size()) {
                rval = false;
            } else {
                // TODO: should the order of things in the list matter?
            	// I think not, but if it is decided so, then use this code instead
            	// of the code below it
            	/*
                for (int i = 0; i < ((List<Object>) v1).size() && rval == true; i++) {
                    rval = equals(((List<Object>) v1).get(i), ((List<Object>) v2).get(i));
                }
                */
            	for (int i = 0; i < ((List<Object>) v1).size() && rval == true; i++) {
            		boolean found = false;
            		for (int j = 0; j < ((List<Object>) v2).size() && found == false; j++) {
            			found = equals(((List<Object>) v1).get(i), ((List<Object>) v2).get(j));
            		}
            		rval = found;
                }
            }
        } else if (v1 instanceof Number && v2 instanceof Number) {
            // TODO: this is VERY sketchy
            double n1 = ((Number) v1).doubleValue();
            double n2 = ((Number) v2).doubleValue();

            rval = n1 == n2;
        } else if (v1 instanceof String && v2 instanceof String) {
            rval = ((String) v1).equals((String) v2);
        } else if (v1 instanceof Map && v2 instanceof Map) {
            if (((Map) v1).size() != ((Map) v2).size()) {
                rval = false;
            } else {
                for (Object k1 : ((Map) v1).keySet()) {
                    rval = ((Map) v2).containsKey(k1) ? equals(((Map) v1).get(k1), ((Map) v2).get(k1)) : false;
                    if (rval != true) {
                        break;
                    }
                }
            }
        } else if (v1 instanceof Boolean && v2 instanceof Boolean) {
            rval = v1 == v2;
        } else if (v1 != null && v2 != null) {
            rval = v1.equals(v2);
        } else {
            rval = v1 == v2;
        }

        return rval;
    }
}
