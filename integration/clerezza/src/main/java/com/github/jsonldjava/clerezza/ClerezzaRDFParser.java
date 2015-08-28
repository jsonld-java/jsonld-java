package com.github.jsonldjava.clerezza;

import java.util.HashMap;
import java.util.Map;

import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.PlainLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.TypedLiteral;
import org.apache.clerezza.rdf.core.UriRef;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.core.RDFDataset;
import com.github.jsonldjava.core.RDFParser;

/**
 * Converts a Clerezza {@link TripleCollection} to the {@link RDFDataset} used
 * by the {@link JsonLdProcessor}
 * 
 * @author Rupert Westenthaler
 * 
 */
public class ClerezzaRDFParser implements RDFParser {

    private static String RDF_LANG_STRING = "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString";

    private long count = 0;

    @Override
    public RDFDataset parse(Object input) throws JsonLdError {
        count = 0;
        final Map<BNode, String> bNodeMap = new HashMap<BNode, String>(1024);
        final RDFDataset result = new RDFDataset();
        if (input instanceof TripleCollection) {
            for (final Triple t : ((TripleCollection) input)) {
                handleStatement(result, t, bNodeMap);
            }
        }
        bNodeMap.clear(); // help gc
        return result;
    }

    private void handleStatement(RDFDataset result, Triple t, Map<BNode, String> bNodeMap) {
        final String subject = getResourceValue(t.getSubject(), bNodeMap);
        final String predicate = getResourceValue(t.getPredicate(), bNodeMap);
        final Resource object = t.getObject();

        if (object instanceof Literal) {

            final String value = ((Literal) object).getLexicalForm();
            final String language;
            final String datatype;
            if (object instanceof TypedLiteral) {
                language = null;
                datatype = getResourceValue(((TypedLiteral) object).getDataType(), bNodeMap);
            } else if (object instanceof PlainLiteral) {
                // we use RDF 1.1 literals so we do set the RDF_LANG_STRING
                // datatype
                datatype = RDF_LANG_STRING;
                final Language l = ((PlainLiteral) object).getLanguage();
                if (l == null) {
                    language = null;
                } else {
                    language = l.toString();
                }
            } else {
                throw new IllegalStateException("Unknown Literal class "
                        + object.getClass().getName());
            }
            result.addTriple(subject, predicate, value, datatype, language);
            count++;
        } else {
            result.addTriple(subject, predicate, getResourceValue((NonLiteral) object, bNodeMap));
            count++;
        }

    }

    /**
     * The count of processed triples (not thread save)
     * 
     * @return the count of triples processed by the last {@link #parse(Object)}
     *         call
     */
    public long getCount() {
        return count;
    }

    private String getResourceValue(NonLiteral nl, Map<BNode, String> bNodeMap) {
        if (nl == null) {
            return null;
        } else if (nl instanceof UriRef) {
            return ((UriRef) nl).getUnicodeString();
        } else if (nl instanceof BNode) {
            String bNodeId = bNodeMap.get(nl);
            if (bNodeId == null) {
                bNodeId = Integer.toString(bNodeMap.size());
                bNodeMap.put((BNode) nl, bNodeId);
            }
            return new StringBuilder("_:b").append(bNodeId).toString();
        } else {
            throw new IllegalStateException("Unknwon NonLiteral type " + nl.getClass().getName()
                    + "!");
        }
    }
}
