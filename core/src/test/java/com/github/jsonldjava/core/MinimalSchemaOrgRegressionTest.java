package com.github.jsonldjava.core;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class MinimalSchemaOrgRegressionTest {

    @Test
    public void testHttpURLConnection() throws Exception {
        URL url = new URL("http://schema.org/");
        HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
        urlConn.addRequestProperty("Accept",
                "application/ld+json, application/json;q=0.9, application/javascript;q=0.5, text/javascript;q=0.5, text/plain;q=0.2, */*;q=0.1");

        InputStream directStream = urlConn.getInputStream();
        StringWriter output = new StringWriter();
        try {
            IOUtils.copy(directStream, output, Charset.forName("UTF-8"));
        } finally {
            directStream.close();
            output.flush();
        }
        String outputString = output.toString();
        // Test for some basic conditions without including the JSON/JSON-LD
        // parsing code here
        assertTrue(outputString.endsWith("}\n"));
        assertTrue(outputString.length() > 100000);
    }

}
