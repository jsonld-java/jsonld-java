package com.github.jsonldjava.core;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.impl.client.CloseableHttpClient;

import com.github.jsonldjava.utils.JsonUtils;

public class DocumentLoader {

    private Map<String, Object> m_injectedDocs = new HashMap<>();

    /**
     * Identifies a system property that can be set to "true" in order to
     * disallow remote context loading.
     */
    public static final String DISALLOW_REMOTE_CONTEXT_LOADING = "com.github.jsonldjava.disallowRemoteContextLoading";

    public DocumentLoader addInjectedDoc(String url, String doc) throws JsonLdError {
        try {
            m_injectedDocs.put(url, JsonUtils.fromString(doc));
            return this;
        } catch (final Exception e) {
            throw new JsonLdError(JsonLdError.Error.LOADING_INJECTED_CONTEXT_FAILED, url, e);
        }
    }

    public RemoteDocument loadDocument(String url) throws JsonLdError {
        final RemoteDocument doc = new RemoteDocument(url, null);

        if (m_injectedDocs.containsKey(url)) {
            try {
                doc.setDocument(m_injectedDocs.get(url));
            } catch (final Exception e) {
                throw new JsonLdError(JsonLdError.Error.LOADING_INJECTED_CONTEXT_FAILED, url, e);
            }
            return doc;
        }

        final String disallowRemote = System
                .getProperty(DocumentLoader.DISALLOW_REMOTE_CONTEXT_LOADING);
        if ("true".equalsIgnoreCase(disallowRemote)) {
            throw new JsonLdError(JsonLdError.Error.LOADING_REMOTE_CONTEXT_FAILED, "Remote context loading has been disallowed (url was " + url + ")");
        }

        try {
            doc.setDocument(JsonUtils.fromURL(new URL(url), getHttpClient()));
        } catch (final Exception e) {
            throw new JsonLdError(JsonLdError.Error.LOADING_REMOTE_CONTEXT_FAILED, url, e);
        }
        return doc;
    }

    /**
     * An HTTP Accept header that prefers JSONLD.
     *
     * @deprecated Use {@link JsonUtils#ACCEPT_HEADER} instead.
     */
    @Deprecated
    public static final String ACCEPT_HEADER = JsonUtils.ACCEPT_HEADER;

    private volatile CloseableHttpClient httpClient;

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

    public void setHttpClient(CloseableHttpClient nextHttpClient) {
        httpClient = nextHttpClient;
    }
}
