package com.github.jsonldjava.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.cache.CacheResponseStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.cache.CachingHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.Ignore;
import org.junit.Test;

public class JsonUtilsIT {

    @Test
    public void fromURLredirectHTTPSToHTTP() throws Exception {
        final URL url = new URL("https://w3id.org/bundle/context");
        final Object context = JSONUtils.fromURL(url);
        // Should not fail because of
        // http://stackoverflow.com/questions/1884230/java-doesnt-follow-redirect-in-urlconnection
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4620571
        assertTrue(context instanceof Map);
        assertFalse(((Map<?, ?>) context).isEmpty());
    }

    @Test
    public void fromURLredirect() throws Exception {
        final URL url = new URL("http://purl.org/wf4ever/ro-bundle/context.json");
        final Object context = JSONUtils.fromURL(url);
        assertTrue(context instanceof Map);
        assertFalse(((Map<?, ?>) context).isEmpty());
    }

    @Test
    public void fromURLCache() throws Exception {
        final URL url = new URL("http://json-ld.org/contexts/person.jsonld");
        JSONUtils.fromURL(url);
        
        // Now try to get it again and ensure it is
        // cached
        final HttpClient client = new CachingHttpClient(JSONUtils.getHttpClient());
        final HttpUriRequest get = new HttpGet(url.toURI());
        get.setHeader("Accept", JSONUtils.ACCEPT_HEADER);
        final HttpContext localContext = new BasicHttpContext();
        final HttpResponse respo = client.execute(get, localContext);
        EntityUtils.consume(respo.getEntity());

        // Check cache status
        // http://hc.apache.org/httpcomponents-client-ga/tutorial/html/caching.html
        final CacheResponseStatus responseStatus = (CacheResponseStatus) localContext
                .getAttribute(CachingHttpClient.CACHE_RESPONSE_STATUS);
        assertFalse(CacheResponseStatus.CACHE_MISS.equals(responseStatus));
    }
    
    @Ignore
    @Test
    public void fromURLCacheWithRedirect() throws Exception {
        final URL url = new URL("https://w3id.org/web-keys/v1");
        JSONUtils.fromURL(url);
        
        System.out.println("To test caching, turn off wifi/disconnect network cable now!");
        Thread.sleep(5000);
        System.out.println("Checking loading from cache:");
        
        // Now try to get it again and ensure it is cached
        Object cached = JSONUtils.fromURL(url);
        System.out.println(cached);
        assertTrue(cached instanceof Map);
        Map<String,?> cachedMap = (Map<String, ?>) cached;
        assertFalse(cachedMap.isEmpty());
    }

}
