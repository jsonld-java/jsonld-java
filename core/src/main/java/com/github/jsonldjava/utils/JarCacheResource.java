package com.github.jsonldjava.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.cache.Resource;

public class JarCacheResource implements Resource {

    private static final long serialVersionUID = -7101296464577357444L;

    private final Log log = LogFactory.getLog(getClass());

    private URLConnection connection;

    public JarCacheResource(URL classpath) throws IOException {
        this.connection = classpath.openConnection();
    }

    @Override
    public long length() {
        return connection.getContentLengthLong();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return connection.getInputStream();
    }

    @Override
    public void dispose() {
        try {
            connection.getInputStream().close();
        } catch (IOException e) {
            log.error("Can't close JarCacheResource input stream", e);
        }
    }
}