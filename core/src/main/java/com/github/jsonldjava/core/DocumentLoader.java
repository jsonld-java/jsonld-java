package com.github.jsonldjava.core;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.impl.client.CloseableHttpClient;

import com.github.jsonldjava.utils.JsonUtils;

/**
 * Resolves URLs to {@link RemoteDocument}s. Subclass this class to change the
 * behaviour of loadDocument to suit your purposes.
 */
public class DocumentLoader {

    private final Map<String, Object> m_injectedDocs = new HashMap<>();

    /**
     * Identifies a system property that can be set to "true" in order to
     * disallow remote context loading.
     */
    public static final String DISALLOW_REMOTE_CONTEXT_LOADING = "com.github.jsonldjava.disallowRemoteContextLoading";

    /**
     * Avoid resolving a document by instead using the given serialised
     * representation.
     *
     * @param url
     *            The URL this document represents.
     * @param doc
     *            The serialised document as a String
     * @return This object for fluent addition of other injected documents.
     * @throws JsonLdError
     *             If loading of the document failed for any reason.
     */
    public DocumentLoader addInjectedDoc(String url, String doc) throws JsonLdError {
        try {
            m_injectedDocs.put(url, JsonUtils.fromString(doc));
            return this;
        } catch (final Exception e) {
            throw new JsonLdError(JsonLdError.Error.LOADING_INJECTED_CONTEXT_FAILED, url, e);
        }
    }

    /**
     * Loads the URL if possible, returning it as a RemoteDocument.
     *
     * @param url
     *            The URL to load
     * @return The resolved URL as a RemoteDocument
     * @throws JsonLdError
     *             If there are errors loading or remote context loading has
     *             been disallowed.
     */
    public RemoteDocument loadDocument(String url) throws JsonLdError {
        if (m_injectedDocs.containsKey(url)) {
            try {
                return new RemoteDocument(url, m_injectedDocs.get(url));
            } catch (final Exception e) {
                throw new JsonLdError(JsonLdError.Error.LOADING_INJECTED_CONTEXT_FAILED, url, e);
            }
        } else {
            final String disallowRemote = System
                    .getProperty(DocumentLoader.DISALLOW_REMOTE_CONTEXT_LOADING);
            if ("true".equalsIgnoreCase(disallowRemote)) {
                throw new JsonLdError(JsonLdError.Error.LOADING_REMOTE_CONTEXT_FAILED,
                        "Remote context loading has been disallowed (url was " + url + ")");
            }

            try {
                return new RemoteDocument(url, JsonUtils.fromURL(new URL(url), getHttpClient()));
            } catch (final Exception e) {
                throw new JsonLdError(JsonLdError.Error.LOADING_REMOTE_CONTEXT_FAILED, url, e);
            }
        }
    }

    private volatile CloseableHttpClient httpClient;

    /**
     * Get the {@link CloseableHttpClient} which will be used by this
     * DocumentLoader to resolve HTTP and HTTPS resources.
     *
     * @return The {@link CloseableHttpClient} which this DocumentLoader uses.
     */
    public CloseableHttpClient getHttpClient() {
        CloseableHttpClient result = httpClient;
        if (result == null) {
            synchronized (DocumentLoader.class) {
                result = httpClient;
                if (result == null) {
                    result = httpClient = JsonUtils.getDefaultHttpClient();
                }
            }
        }
        return result;
    }

    /**
     * Call this method to override the default CloseableHttpClient provided by
     * JsonUtils.getDefaultHttpClient.
     *
     * @param nextHttpClient
     *            The {@link CloseableHttpClient} to replace the default with.
     */
    public void setHttpClient(CloseableHttpClient nextHttpClient) {
        httpClient = nextHttpClient;
    }
}
