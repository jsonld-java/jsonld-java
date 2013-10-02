package com.github.jsonldjava.impl;

import java.util.Map;

import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.ModelSet;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.DatatypeLiteral;
import org.ontoware.rdf2go.model.node.LanguageTagLiteral;
import org.ontoware.rdf2go.model.node.Literal;
import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.Resource;
import org.ontoware.rdf2go.model.node.URI;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.RDFDataset;
import com.github.jsonldjava.core.RDFParser;
import com.github.jsonldjava.core.JsonLdError.Error;

/**
 * Implementation of {@link RDFParser} which serializes the contents of a
 * {@link ModelSet} or {@link Model} into a JSON-LD document.
 * 
 * @author Ismael Rivera
 */
public class RDF2GoRDFParser implements RDFParser {

    private void importModel(RDFDataset result, Model model) {
        // add prefixes/namespaces
        final Map<String, String> nsPrefixMap = model.getNamespaces();
        for (final String prefix : nsPrefixMap.keySet()) {
            result.setNamespace(prefix, nsPrefixMap.get(prefix));
        }

        // add all statements from model
        final URI context = model.getContextURI();
        final ClosableIterator<Statement> statements = model.iterator();
        while (statements.hasNext()) {
            handleStatement(result, statements.next(), context);
        }
        statements.close();
    }

    private void importModelSet(RDFDataset result, ModelSet modelSet, URI... contexts) {
        final ClosableIterator<Model> models = modelSet.getModels();
        while (models.hasNext()) {
            importModel(result, models.next());
        }
        models.close();
    }

    private void handleStatement(RDFDataset result, Statement statement, URI context) {
        final Resource subject = statement.getSubject();
        final URI predicate = statement.getPredicate();
        final Node object = statement.getObject();

        if (object instanceof DatatypeLiteral) {
            final DatatypeLiteral literal = (DatatypeLiteral) object;
            addStatement(result, context, subject, predicate, literal.getValue(),
                    literal.getDatatype());
        } else if (object instanceof LanguageTagLiteral) {
            final LanguageTagLiteral literal = (LanguageTagLiteral) object;
            addStatement(result, context, subject, predicate, literal.getValue(),
                    literal.getLanguageTag());
        } else if (object instanceof Literal) {
            final Literal literal = (Literal) object;
            addStatement(result, context, subject, predicate, literal.getValue());
        } else {
            addStatement(result, context, subject, predicate, object.asURI());
        }
    }

    private void addStatement(RDFDataset result, URI context, Resource subject, URI predicate,
            URI object) {
        if (context == null) {
            result.addTriple(subject.toString(), predicate.toString(), object.toString());
        } else {
            result.addQuad(subject.toString(), predicate.toString(), object.toString(),
                    context.toString());
        }
    }

    private void addStatement(RDFDataset result, URI context, Resource subject, URI predicate,
            String value) {
        if (context == null) {
            result.addTriple(subject.toString(), predicate.toString(), value, null, null);
        } else {
            result.addQuad(subject.toString(), predicate.toString(), value, null, null,
                    context.toString());
        }
    }

    private void addStatement(RDFDataset result, URI context, Resource subject, URI predicate,
            String value, URI datatype) {
        if (context == null) {
            result.addTriple(subject.toString(), predicate.toString(), value, datatype.toString(),
                    null);
        } else {
            result.addQuad(subject.toString(), predicate.toString(), value, datatype.toString(),
                    null, context.toString());
        }
    }

    private void addStatement(RDFDataset result, URI context, Resource subject, URI predicate,
            String value, String language) {
        if (context == null) {
            result.addTriple(subject.toString(), predicate.toString(), value, null, language);
        } else {
            result.addQuad(subject.toString(), predicate.toString(), value, null, language,
                    context.toString());
        }
    }

    @Override
    public RDFDataset parse(Object input) throws JsonLdError {
        final RDFDataset result = new RDFDataset();

        // empty dataset if no input given
        if (input == null) {
            return result;
        }

        if (input instanceof ModelSet) {
            importModelSet(result, (ModelSet) input);
        } else if (input instanceof Model) {
            importModel(result, (Model) input);
        } else {
            throw new JsonLdError(Error.INVALID_INPUT,
                    "RDF2Go parser expects a Model or ModelSet object as input");
        }

        return result;
    }

}