/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.jsonldjava.jena;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.jena.atlas.lib.InternalErrorException;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.riot.ReaderRIOT;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.lang.LabelToNode;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.SyntaxLabels;

import com.github.jsonldjava.core.JSONLD;
import com.github.jsonldjava.core.JSONLDProcessingError;
import com.github.jsonldjava.core.JSONLDTripleCallback;
import com.github.jsonldjava.core.Options;
import com.github.jsonldjava.core.RDFDataset;
import com.github.jsonldjava.utils.JSONUtils;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.util.Context;

public class JsonLDReader implements ReaderRIOT {
    @Override
    public void read(InputStream in, String baseURI, ContentType ct,
            final StreamRDF output, Context context) {
        try {
            Object jsonObject = JSONUtils.fromInputStream(in);
            JSONLDTripleCallback callback = new JSONLDTripleCallback() {

                @Override
                // public Object call(Map<String, Object> dataset) {
                public Object call(RDFDataset dataset) {
                    for (String gn : dataset.keySet()) {
                        Object x = dataset.get(gn);
                        if ("@default".equals(gn)) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> triples = (List<Map<String, Object>>) x;
                            for (Map<String, Object> t : triples) {
                                Node s = createNode(t, "subject");
                                Node p = createNode(t, "predicate");
                                Node o = createNode(t, "object");
                                Triple triple = Triple.create(s, p, o);
                                output.triple(triple);
                            }
                        } else {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> quads = (List<Map<String, Object>>) x;
                            Node g = NodeFactory.createURI(gn); // Bnodes?
                            for (Map<String, Object> q : quads) {
                                Node s = createNode(q, "subject");
                                Node p = createNode(q, "predicate");
                                Node o = createNode(q, "object");
                                output.quad(Quad.create(g, s, p, o));
                            }

                        }

                    }
                    return null;
                }
            };
            Options options = new Options(baseURI);
            JSONLD.toRDF(jsonObject, callback, options);
        } catch (IOException e) {
            throw new RiotException("Could not read JSONLD: " + e, e);
        } catch (JSONLDProcessingError e) {
            throw new RiotException("Could not parse JSONLD: " + e, e);
        }
    }

    private LabelToNode labels = SyntaxLabels.createLabelToNode();

    public static String LITERAL = "literal";
    public static String BLANK_NODE = "blank node";
    public static String IRI = "IRI";

    private Node createNode(Map<String, Object> tripleMap, String key) {
        @SuppressWarnings("unchecked")
        Map<String, Object> x = (Map<String, Object>) (tripleMap.get(key));
        return createNode(x);
    }

    // See RDFParser
    private Node createNode(Map<String, Object> map) {
        String type = (String) map.get("type");
        String lex = (String) map.get("value");
        if (type.equals(IRI))
            return NodeFactory.createURI(lex);
        else if (type.equals(BLANK_NODE))
            return labels.get(null, lex);
        else if (type.equals(LITERAL)) {
            String lang = (String) map.get("language");
            String datatype = (String) map.get("datatype");
            if (lang == null && datatype == null)
                return NodeFactory.createLiteral(lex);
            if (lang != null)
                return NodeFactory.createLiteral(lex, lang, null);
            RDFDatatype dt = NodeFactory.getType(datatype);
            return NodeFactory.createLiteral(lex, dt);
        } else
            throw new InternalErrorException(
                    "Node is not a IRI, bNode or a literal: " + type);
        // /*
        // * "value" : The value of the node.
        // * "subject" can be an IRI or blank node id.
        // * "predicate" should only ever be an IRI
        // * "object" can be and IRI or blank node id, or a literal value
        // (represented as a string)
        // * "type" : "IRI" if the value is an IRI or "blank node" if the value
        // is a blank node.
        // * "object" can also be "literal" in the case of literals.
        // * The value of "object" can also contain the following optional
        // key-value pairs:
        // * "language" : the language value of a string literal
        // * "datatype" : the datatype of the literal. (if not set will default
        // to XSD:string, if set to null, null will be used). */
        // System.out.println(map.get("value")) ;
        // System.out.println(map.get("type")) ;
        // System.out.println(map.get("language")) ;
        // System.out.println(map.get("datatype")) ;
        // return null ;
    }


}
