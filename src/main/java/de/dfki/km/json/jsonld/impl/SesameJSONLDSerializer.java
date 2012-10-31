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

import de.dfki.km.json.jsonld.JSONLDProcessingError;

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
        String graph = nextStatement.getContext() == null ? null : nextStatement.getContext().stringValue();
        
        if (object instanceof Literal) {
            Literal literal = (Literal) object;
            String value = literal.getLabel();
            URI datatypeURI = literal.getDatatype();
            String language = literal.getLanguage();
            
            String datatype;
            
            if (datatypeURI == null) {
                datatype = null;
            } else {
                datatype = datatypeURI.stringValue();
            }
            
            triple(subject.stringValue(), predicate.stringValue(), value, datatype, language, graph);
        } else {
            triple(subject.stringValue(), predicate.stringValue(), object.stringValue(), graph);
        }
    }

	@Override
	public void parse(Object input) throws JSONLDProcessingError {
		if (input instanceof Statement) {
			handleStatement((Statement)input);
		}
	}

}
