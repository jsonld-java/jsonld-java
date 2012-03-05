package de.dfki.km.json.jsonld.impl;

import java.util.Iterator;

import org.openrdf.model.Graph;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SesameJSONLDSerializer extends de.dfki.km.json.jsonld.JSONLDSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(SesameJSONLDSerializer.class);

    public void importGraph(Graph model, Resource... contexts) {
        Iterator<Statement> statements = model.match(null, null, null, contexts);
        while (statements.hasNext()) {
            handleStatement(statements.next());
        }
    }

    public void handleStatement(Statement nextStatement) {
        Resource subject = nextStatement.getSubject();
        URI predicate = nextStatement.getPredicate();
        Value object = nextStatement.getObject();

        if (object instanceof Literal) {
            Literal literal = (Literal) object;
            String value = literal.getLabel();
            URI datatypeURI = literal.getDatatype();
            String language = literal.getLanguage();

            triple(subject.stringValue(), predicate.stringValue(), value, datatypeURI.stringValue(), language);
        } else {
            triple(subject.stringValue(), predicate.stringValue(), object.stringValue());
        }
    }

}
