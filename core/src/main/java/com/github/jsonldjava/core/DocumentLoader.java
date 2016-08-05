package com.github.jsonldjava.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.http.impl.client.CloseableHttpClient;

import com.fasterxml.jackson.core.JsonParseException;
import com.github.jsonldjava.utils.JsonUtils;

public class DocumentLoader {

    /**
     * Identifies a system property that can be set to "true" in order to
     * disallow remote context loading.
     */
    public static final String DISALLOW_REMOTE_CONTEXT_LOADING = "com.github.jsonldjava.disallowRemoteContextLoading";

    public RemoteDocument loadDocument(String url) throws JsonLdError {
        String disallowRemote = System.getProperty(DocumentLoader.DISALLOW_REMOTE_CONTEXT_LOADING);

        if ("true".equalsIgnoreCase(disallowRemote)) {
            throw new JsonLdError(JsonLdError.Error.LOADING_REMOTE_CONTEXT_FAILED, url);
        }

        final RemoteDocument doc = new RemoteDocument(url, null);
        try {
            if(url.equalsIgnoreCase("http://schema.org/")) {
                doc.setDocument(JsonUtils.fromURLJavaNet(new URL(url)));
            } else {
                doc.setDocument(JsonUtils.fromURL(new URL(url), getHttpClient()));
            }
        } catch (final Exception e) {
            throw new JsonLdError(JsonLdError.Error.LOADING_REMOTE_CONTEXT_FAILED, url);
        }
        return doc;
    }

    /**
     * An HTTP Accept header that prefers JSONLD.
     * @deprecated Use {@link JsonUtils#ACCEPT_HEADER} instead.
     */
    @Deprecated
    public static final String ACCEPT_HEADER = JsonUtils.ACCEPT_HEADER;

    private volatile CloseableHttpClient httpClient;

    /**
     * Returns a Map, List, or String containing the contents of the JSON
     * resource resolved from the JsonLdUrl.
     *
     * @param url
     *            The JsonLdUrl to resolve
     * @return The Map, List, or String that represent the JSON resource
     *         resolved from the JsonLdUrl
     * @throws JsonParseException
     *             If the JSON was not valid.
     * @throws IOException
     *             If there was an error resolving the resource.
     * @deprecated Since 0.8.4, use {@link #loadDocument(String)} instead.
     */
    @Deprecated
    public Object fromURL(java.net.URL url) throws JsonParseException, IOException {
        return JsonUtils.fromURL(url, getHttpClient());
    }
    
    /**
     * Opens an {@link InputStream} for the given {@link java.net.URL},
     * including support for http and https URLs that are requested using
     * Content Negotiation with application/ld+json as the preferred content
     * type.
     *
     * @param url
     *            The {@link java.net.URL} identifying the source.
     * @return An InputStream containing the contents of the source.
     * @throws IOException
     *             If there was an error resolving the {@link java.net.URL}.
     * @deprecated Since 0.8.4, use {@link #loadDocument(String)} instead.
     */
    @Deprecated
    public InputStream openStreamFromURL(java.net.URL url) throws IOException {
        return JsonUtils.openStreamForURL(url, getHttpClient());
    }
    
    public CloseableHttpClient getHttpClient() {
        CloseableHttpClient result = httpClient;
        if (result == null) {
            synchronized(DocumentLoader.class) {
                result = httpClient;
                if(result == null) {
                    result = httpClient = JsonUtils.getDefaultHttpClient();
                }
            }
        }
        return result;
    }

    public void setHttpClient(CloseableHttpClient nextHttpClient) {
        httpClient = nextHttpClient;
    }
}
