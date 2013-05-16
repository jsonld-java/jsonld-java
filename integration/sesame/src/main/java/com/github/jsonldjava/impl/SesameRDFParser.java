package com.github.jsonldjava.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openrdf.model.BNode;
import org.openrdf.model.Graph;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import com.github.jsonldjava.core.JSONLDProcessingError;
import com.github.jsonldjava.core.RDFDatasetUtils;


public class SesameRDFParser implements
com.github.jsonldjava.core.RDFParser {

	@SuppressWarnings("deprecation")
	public void importGraph(Graph model, Resource... contexts) {
		Map<String,Object> result = RDFDatasetUtils.getInitialRDFDatasetResult();
		Iterator<Statement> statements = model
				.match(null, null, null, contexts);
		while (statements.hasNext()) {
			handleStatement(result, statements.next());
		}
		
		// TODO: return something? i'm leaving this to Ansell to fix up to match his requirements
	}

	public void handleStatement(Map<String,Object> result, Statement nextStatement) {
		// TODO: from a basic look at the code it seems some of these could be null
		// null values for IRIs will probably break things further down the line
		// and i'm not sure yet if this should be something handled later on, or
		// something that should be checked here
		final String subject = getResourceValue(nextStatement.getSubject());
		final String predicate = getResourceValue(nextStatement.getPredicate());
		final Value object = nextStatement.getObject();
		final String graphName = getResourceValue(nextStatement.getContext());
		
		if (object instanceof Literal) {
			Literal literal = (Literal) object;
			String value = literal.getLabel();
			String language = literal.getLanguage();

			String datatype = getResourceValue(literal.getDatatype());
			
			RDFDatasetUtils.addTripleToRDFDatasetResult(result, graphName, 
					RDFDatasetUtils.generateTriple(subject, predicate, value, datatype, language));
			
		} else {
			RDFDatasetUtils.addTripleToRDFDatasetResult(result, graphName, 
					RDFDatasetUtils.generateTriple(subject, predicate, getResourceValue((Resource)object)));			
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
	public Map<String,Object> parse(Object input) throws JSONLDProcessingError {
		Map<String,Object> result = RDFDatasetUtils.getInitialRDFDatasetResult();
		if (input instanceof Statement) {
			handleStatement(result, (Statement) input);
		} else if (input instanceof Graph) {
			for (Statement nextStatement : (Graph) input) {
				handleStatement(result, nextStatement);
			}
		}
		return result;
	}

}
