package com.github.jsonldjava.impl;

import java.util.Set;

import org.openrdf.model.BNode;
import org.openrdf.model.Graph;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.XMLSchema;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.RDFDataset;

public class SesameRDFParser implements com.github.jsonldjava.core.RDFParser {

    public void setPrefix(String fullUri, String prefix) {
        // TODO: graphs?
        // _context.put(prefix, fullUri);
    }

    public void handleStatement(RDFDataset result, Statement nextStatement) {
        // TODO: from a basic look at the code it seems some of these could be
        // null
        // null values for IRIs will probably break things further down the line
        // and i'm not sure yet if this should be something handled later on, or
        // something that should be checked here
        final String subject = getResourceValue(nextStatement.getSubject());
        final String predicate = getResourceValue(nextStatement.getPredicate());
        final Value object = nextStatement.getObject();
        final String graphName = getResourceValue(nextStatement.getContext());

        if (object instanceof Literal) {
            final Literal literal = (Literal) object;
            final String value = literal.getLabel();
            final String language = literal.getLanguage();

            String datatype = getResourceValue(literal.getDatatype());

            // In RDF-1.1, Language Literals internally have the datatype
            // rdf:langString
            if (language != null && datatype == null) {
                datatype = RDF.LANGSTRING.stringValue();
            }

            // In RDF-1.1, RDF-1.0 Plain Literals are now Typed Literals with
            // type xsd:String
            if (language == null && datatype == null) {
                datatype = XMLSchema.STRING.stringValue();
            }

            result.addQuad(subject, predicate, value, datatype, language, graphName);

        } else {
            result.addQuad(subject, predicate, getResourceValue((Resource) object), graphName);
        }
    }

    private String getResourceValue(Resource subject) {
        if (subject == null) {
            return null;
        } else if (subject instanceof URI) {
            return subject.stringValue();
        } else if (subject instanceof BNode) {
            return "_:" + subject.stringValue();
        }

        throw new IllegalStateException("Did not recognise resource type: "
                + subject.getClass().getName());
    }

    @Override
    public RDFDataset parse(Object input) throws JsonLdError {
        final RDFDataset result = new RDFDataset();
        if (input instanceof Statement) {
            handleStatement(result, (Statement) input);
        } else if (input instanceof Graph) {
            if (input instanceof Model) {
                final Set<Namespace> namespaces = ((Model) input).getNamespaces();
                for (final Namespace nextNs : namespaces) {
                    setPrefix(nextNs.getName(), nextNs.getPrefix());
                }
            }

            for (final Statement nextStatement : (Graph) input) {
                handleStatement(result, nextStatement);
            }
        }
        return result;
    }

}
