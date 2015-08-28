/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jsonldjava.clerezza;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.serializedform.SerializingProvider;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

/**
 * A {@link org.apache.clerezza.rdf.core.serializedform.SerializingProvider} for
 * JSON-LD (application/ld+json) based on the java-jsonld library
 * 
 * @author Rupert Westenthaler
 */
@Component(immediate = true, policy = ConfigurationPolicy.OPTIONAL)
@Service
@SupportedFormat(value = { "application/ld+json", "application/json" })
public class JsonLdSerializingProvider implements SerializingProvider {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final String MODE_EXPAND = "expand";
    private static final String MODE_FLATTEN = "flatten";
    private static final String MODE_COMPACT = "compact";

    @Property(value = "", options = {
            @PropertyOption(value = "%mode.option.none", name = ""),
            @PropertyOption(value = "%mode.option.flatten", name = "flatten"),
            @PropertyOption(value = "%mode.option.compact", name = "compact"),
            @PropertyOption(value = "%mode.option.expand", name = MODE_EXPAND) })
    private static final String PROP_MODE = "mode";

    @Property(boolValue = false)
    private static final String PROP_USE_RDF_TYPE = "useRdfTye";

    @Property(boolValue = false)
    private static final String PROP_USE_NATIVE_TYPES = "useNativeTypes";

    @Property(boolValue = true)
    private static final String PROP_PRETTY_PRINT = "prettyPrint";

    // TODO: make configurable or read the whole prefix.cc list from a file and
    // search for really used namespaces while parsing the triples in the
    // ClerezzaRDFParser
    private static Map<String, String> DEFAULT_NAMESPACES;
    static {
        // core ontologies, top from prefixcc and some stanbol specific
        final Map<String, String> ns = new LinkedHashMap<String, String>();
        // core schemas
        ns.put("xsd", "http://www.w3.org/2001/XMLSchema#");
        ns.put("owl", "http://www.w3.org/2002/07/owl#");
        ns.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        ns.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        // well known ontologies
        ns.put("skos", "http://www.w3.org/2004/02/skos/core#");
        ns.put("geo", "http://www.w3.org/2003/01/geo/wgs84_pos#");
        ns.put("dc", "http://purl.org/dc/elements/1.1/");
        ns.put("foaf", "http://xmlns.com/foaf/0.1/");
        ns.put("ma", "http://www.w3.org/ns/ma-ont#");
        // big datasets
        ns.put("dbo", "http://dbpedia.org/ontology/");
        ns.put("dbp", "http://dbpedia.org/property/");
        ns.put("yago", "http://yago-knowledge.org/resource/");
        ns.put("fb", "http://rdf.freebase.com/ns/");
        ns.put("geonames", "http://www.geonames.org/ontology#");
        // stanbol specific
        ns.put("fise", "http://fise.iks-project.eu/ontology/");
        ns.put("enhancer", "http://stanbol.apache.org/ontology/enhancer/enhancer#");
        ns.put("entityhub", "http://stanbol.apache.org/ontology/entityhub/entityhub#");

        DEFAULT_NAMESPACES = Collections.unmodifiableMap(ns);
    }

    private JsonLdOptions opts = null;
    private String mode;

    private boolean prettyPrint;

    @Override
    public void serialize(OutputStream serializedGraph, TripleCollection tc, String formatIdentifier) {
        final ClerezzaRDFParser serializer = new ClerezzaRDFParser();
        try {
            final long start = System.currentTimeMillis();
            Object output = JsonLdProcessor.fromRDF(tc, serializer);

            if (MODE_EXPAND.equalsIgnoreCase(mode)) {
                logger.debug(" - mode: {}", MODE_EXPAND);
                output = JsonLdProcessor.expand(output, opts);
            }
            if (MODE_FLATTEN.equalsIgnoreCase(mode)) {
                logger.debug(" - mode: {}", MODE_FLATTEN);
                // TODO: Allow inframe config
                final Object inframe = null;
                output = JsonLdProcessor.flatten(output, inframe, opts);
            }
            if (MODE_COMPACT.equalsIgnoreCase(mode)) {
                logger.debug(" - mode: {}", MODE_COMPACT);
                // TODO: collect namespaces used in the triples in the
                // ClerezzaRDFParser
                final Map<String, Object> localCtx = new HashMap<String, Object>();
                localCtx.put("@context", DEFAULT_NAMESPACES);
                output = JsonLdProcessor.compact(output, localCtx, opts);
            }
            final Writer writer = new OutputStreamWriter(serializedGraph, UTF8);
            logger.debug(" - prettyPrint: {}", prettyPrint);
            if (prettyPrint) {
                JsonUtils.writePrettyPrint(writer, output);
            } else {
                JsonUtils.write(writer, output);
            }
            if (logger.isDebugEnabled()) {
                logger.debug(" - serialized {} triples in {}ms", serializer.getCount(),
                        System.currentTimeMillis() - start);
            }
        } catch (final JsonLdError e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (final IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Activate
    protected void activate(ComponentContext ctx) {
        opts = new JsonLdOptions();
        @SuppressWarnings("unchecked")
        final Dictionary<String, Object> config = ctx.getProperties();
        // boolean properties
        opts.setUseRdfType(getState(config.get(PROP_USE_RDF_TYPE), false));
        opts.setUseNativeTypes(getState(config.get(PROP_USE_NATIVE_TYPES), false));
        prettyPrint = getState(config.get(PROP_PRETTY_PRINT), true);
        // parse the string mode
        final Object value = config.get(PROP_MODE);
        mode = value == null ? null : value.toString();
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        opts = null;
        mode = null;
        prettyPrint = false;
    }

    /**
     * @param value
     */
    private boolean getState(Object value, boolean defaultState) {
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        } else if (value != null) {
            return Boolean.parseBoolean(value.toString());
        } else {
            return defaultState;
        }
    }

}
