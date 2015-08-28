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
import java.io.InputStream;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.serializedform.ParsingProvider;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

/**
 * A {@link org.apache.clerezza.rdf.core.serializedform.ParsingProvider} for
 * JSON-LD (application/ld+json) based on the java-jsonld library
 * 
 * @author Rupert Westenthaler
 * 
 */
@Component(immediate = true, policy = ConfigurationPolicy.OPTIONAL)
@Service
@SupportedFormat("application/ld+json")
public class JsonLdParsingProvider implements ParsingProvider {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void parse(MGraph target, InputStream serializedGraph, String formatIdentifier,
            UriRef baseUri) {
        // The callback will add parsed triples to the target MGraph
        final ClerezzaTripleCallback ctc = new ClerezzaTripleCallback();
        ctc.setMGraph(target);
        Object input;
        int startSize = 0;
        if (logger.isDebugEnabled()) {
            startSize = target.size();
        }
        final long start = System.currentTimeMillis();
        try {
            input = JsonUtils.fromInputStream(serializedGraph, "UTF-8");
        } catch (final IOException e) {
            logger.error("Unable to read from the parsed input stream", e);
            throw new RuntimeException(e.getMessage(), e);
        }
        try {
            JsonLdProcessor.toRDF(input, ctc);
        } catch (final JsonLdError e) {
            logger.error("Unable to parse JSON-LD from the parsed input stream", e);
            throw new RuntimeException(e.getMessage(), e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug(" - parsed {} triples in {}ms", target.size() - startSize,
                    System.currentTimeMillis() - start);
        }
    }

}
