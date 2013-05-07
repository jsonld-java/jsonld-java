package de.dfki.km.json.jsonld.impl;

import java.util.Iterator;

import org.openrdf.model.BNode;
import org.openrdf.model.Graph;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import de.dfki.km.json.jsonld.JSONLDProcessingError;

public class SesameJSONLDSerializer extends
	de.dfki.km.json.jsonld.JSONLDSerializer {

    @SuppressWarnings("deprecation")
    public void importGraph(Graph model, Resource... contexts) {
	Iterator<Statement> statements = model
		.match(null, null, null, contexts);
	while (statements.hasNext()) {
	    handleStatement(statements.next());
	}
    }

    public void handleStatement(Statement nextStatement) {
	String subject = getResourceValue(nextStatement.getSubject());
	String predicate = getResourceValue(nextStatement.getPredicate());
	Value object = nextStatement.getObject();
	String graph = getResourceValue(nextStatement.getContext());

	if (object instanceof Literal) {
	    Literal literal = (Literal) object;
	    String value = literal.getLabel();
	    String language = literal.getLanguage();

	    String datatype = getResourceValue(literal.getDatatype());

	    triple(subject, predicate, value, datatype, language, graph);
	} else {
	    triple(subject, predicate, getResourceValue((Resource) object),
		    graph);
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
    public void parse(Object input) throws JSONLDProcessingError {
	if (input instanceof Statement) {
	    handleStatement((Statement) input);
	} else if (input instanceof Graph) {
	    for (Statement nextStatement : (Graph) input) {
		handleStatement(nextStatement);
	    }
	}
    }

}
