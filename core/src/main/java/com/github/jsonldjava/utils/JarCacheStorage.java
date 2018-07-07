package com.github.jsonldjava.utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import org.apache.http.Header;
import org.apache.http.HttpVersion;
import org.apache.http.client.cache.HeaderConstants;
import org.apache.http.client.cache.HttpCacheEntry;
import org.apache.http.client.cache.HttpCacheStorage;
import org.apache.http.client.cache.HttpCacheUpdateCallback;
import org.apache.http.client.cache.HttpCacheUpdateException;
import org.apache.http.client.cache.Resource;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.impl.client.cache.BasicHttpCacheStorage;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.MapMaker;

/**
 * Implementation of the Apache HttpClient {@link HttpCacheStorage} interface
 * using {@code jarcache.json} files on the classpath to identify static JSON-LD
 * resources on the classpath, to avoid retrieving them.
 *
 * @author Stian Soiland-Reyes
 * @author Peter Ansell p_ansell@yahoo.com
 */
public class JarCacheStorage implements HttpCacheStorage {

    /**
     * The classpath location that is searched inside of the classloader set for
     * this cache. Note this search is also done on the Thread
     * contextClassLoader if none is explicitly set, and the System classloader
     * if there is no contextClassLoader.
     */
    private static final String JARCACHE_JSON = "jarcache.json";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final CacheConfig cacheConfig;

    /**
     * The classloader to use, defaults to null which will use the thread
     * context classloader.
     */
    private ClassLoader classLoader = null;

    /**
     * A holder for the case where the System class loader needs to be used, but
     * cannot be directly identified in another way.
     *
     * Used as a key in cachedResourceList.
     */
    private static final Object NULL_CLASS_LOADER = new Object();

    /**
     * All live caching that is not found locally is delegated to this
     * implementation.
     */
    private final HttpCacheStorage delegate;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Map from uri of jarcache.json (e.g. jar://blab.jar!jarcache.json) to a
     * SoftReference to its parsed content as JsonNode.
     *
     * @see #getJarCache(URL)
     */
    private final LoadingCache<URL, JsonNode> jarCaches = CacheBuilder.newBuilder()
            .concurrencyLevel(4).maximumSize(100).softValues()
            .build(new CacheLoader<URL, JsonNode>() {
                @Override
                public JsonNode load(URL url) throws IOException {
                    return mapper.readTree(url);
                }
            });

    /**
     * Cached URLs from the given ClassLoader to identified locations of
     * jarcache.json resources on the classpath
     *
     * Uses a Guava concurrent weak reference key map to avoid holding onto
     * ClassLoader instances after they are otherwise unavailable.
     */
    private static final ConcurrentMap<Object, List<URL>> cachedResourceList = new MapMaker()
            .concurrencyLevel(4).weakKeys().makeMap();

    public JarCacheStorage(ClassLoader classLoader, CacheConfig cacheConfig) {
        this(classLoader, cacheConfig, new BasicHttpCacheStorage(cacheConfig));
    }

    public JarCacheStorage(ClassLoader classLoader, CacheConfig cacheConfig,
            HttpCacheStorage delegate) {
        setClassLoader(classLoader);
        this.cacheConfig = Objects.requireNonNull(cacheConfig, "Cache config cannot be null");
        this.delegate = Objects.requireNonNull(delegate, "Delegate cannot be null");
    }

    public ClassLoader getClassLoader() {
        final ClassLoader nextClassLoader = classLoader;
        if (nextClassLoader != null) {
            return nextClassLoader;
        }
        return Thread.currentThread().getContextClassLoader();
    }

    /**
     * Sets the ClassLoader used internally to a new value, or null to use
     * {@link Thread#currentThread()} and {@link Thread#getContextClassLoader()}
     * for each access.
     *
     * @param classLoader
     *            The classloader to use, or null to use the thread context
     *            classloader
     */
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void putEntry(String key, HttpCacheEntry entry) throws IOException {
        delegate.putEntry(key, entry);
    }

    @Override
    public HttpCacheEntry getEntry(String key) throws IOException {
        log.trace("Requesting {}", key);
        Optional<URI> parsedUri = Optional.empty();
        try {
            parsedUri = Optional.of(new URI(key));
        } catch (final URISyntaxException e) {
            // Ignore, will delegate this request
        }
        if (parsedUri.isPresent()) {
            URI requestedUri = parsedUri.get();
            if ((requestedUri.getScheme().equals("http") && requestedUri.getPort() == 80)
                    || (requestedUri.getScheme().equals("https")
                            && requestedUri.getPort() == 443)) {
                // Strip away default http ports
                try {
                    requestedUri = new URI(requestedUri.getScheme(), requestedUri.getHost(),
                            requestedUri.getPath(), requestedUri.getFragment());
                } catch (final URISyntaxException e) {
                    if (log.isTraceEnabled()) {
                        log.trace("Failed to normalise URI port before looking in cache: "
                                + requestedUri, e);
                    }
                    // Ignore syntax error and use the original URI directly
                    // instead
                    // This shouldn't happen as we already attempted to parse
                    // the URI earlier and
                    // would not come here if that failed
                }
            }

            // getResources uses a cache to avoid scanning the classpath again
            // for the
            // current classloader
            for (final URL url : getResources()) {
                // getJarCache attempts to use already parsed in-memory
                // locations to avoid
                // retrieving and parsing again
                final JsonNode tree = getJarCache(url);
                for (final JsonNode node : tree) {
                    final URI uri = URI.create(node.get("Content-Location").asText());
                    if (uri.equals(requestedUri)) {
                        return cacheEntry(requestedUri, url, node);
                    }
                }
            }
        }
        // If we didn't find it in our cache, then attempt to find it in the
        // chained delegate
        return delegate.getEntry(key);
    }

    /**
     * Get all of the {@code jarcache.json} resources that exist on the
     * classpath
     *
     * @return A cached list of jarcache.json classpath resources as
     *         {@link URL}s
     * @throws IOException
     *             If there was an IO error while scanning the classpath
     */
    private List<URL> getResources() throws IOException {
        final ClassLoader cl = getClassLoader();

        // ConcurrentHashMap doesn't support null keys, so substitute a pseudo
        // key
        final Object key = cl == null ? NULL_CLASS_LOADER : cl;

        // computeIfAbsent requires unchecked exceptions for the creation
        // process, so we
        // cannot easily use it directly, instead using get and putIfAbsent
        List<URL> newValue = cachedResourceList.get(key);
        if (newValue != null) {
            return newValue;
        }

        if (cl != null) {
            newValue = Collections
                    .unmodifiableList(Collections.list(cl.getResources(JARCACHE_JSON)));
        } else {
            newValue = Collections.unmodifiableList(
                    Collections.list(ClassLoader.getSystemResources(JARCACHE_JSON)));
        }

        final List<URL> oldValue = cachedResourceList.putIfAbsent(key, newValue);
        // We are not synchronising access to the ConcurrentMap, so if there
        // were
        // multiple classpath scans, we always choose the first one
        return oldValue != null ? oldValue : newValue;
    }

    protected JsonNode getJarCache(URL url) throws IOException {
        try {
            return jarCaches.get(url);
        } catch (final ExecutionException e) {
            throw new IOException("Failed to retrieve jar cache for URL: " + url, e);
        }
    }

    protected HttpCacheEntry cacheEntry(URI requestedUri, URL baseURL, JsonNode cacheNode)
            throws MalformedURLException, IOException {
        final URL classpath = new URL(baseURL, cacheNode.get("X-Classpath").asText());
        log.debug("Cache hit for: {}", requestedUri);
        log.trace("Parsed cache entry: {}", cacheNode);

        final List<Header> responseHeaders = new ArrayList<Header>();
        if (!cacheNode.has(HTTP.DATE_HEADER)) {
            responseHeaders
                    .add(new BasicHeader(HTTP.DATE_HEADER, DateUtils.formatDate(new Date())));
        }
        if (!cacheNode.has(HeaderConstants.CACHE_CONTROL)) {
            responseHeaders.add(new BasicHeader(HeaderConstants.CACHE_CONTROL,
                    HeaderConstants.CACHE_CONTROL_MAX_AGE + "=" + Integer.MAX_VALUE));
        }
        final Resource resource = new JarCacheResource(classpath);
        final Iterator<String> fieldNames = cacheNode.fieldNames();
        while (fieldNames.hasNext()) {
            final String headerName = fieldNames.next();
            final JsonNode header = cacheNode.get(headerName);
            if (header != null) {
                responseHeaders.add(new BasicHeader(headerName, header.asText()));
            }
        }

        return new HttpCacheEntry(new Date(), new Date(),
                new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK"),
                responseHeaders.toArray(new Header[0]), resource);
    }

    @Override
    public void removeEntry(String key) throws IOException {
        delegate.removeEntry(key);
    }

    @Override
    public void updateEntry(String key, HttpCacheUpdateCallback callback)
            throws IOException, HttpCacheUpdateException {
        delegate.updateEntry(key, callback);
    }

    public CacheConfig getCacheConfig() {
        return cacheConfig;
    }

}
