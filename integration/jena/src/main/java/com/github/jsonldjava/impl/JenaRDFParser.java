package com.github.jsonldjava.impl;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.RDFDataset;
import com.github.jsonldjava.core.JsonLdError.Error;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class JenaRDFParser implements com.github.jsonldjava.core.RDFParser {

    // name generator
    Iterator<String> _ng = new Iterator<String>() {
        int i = 0;

        @Override
        public void remove() {
            i++;
        }

        @Override
        public String next() {
            return "_:t" + i++;
        }

        @Override
        public boolean hasNext() {
            return true;
        }
    };
    Map<String, String> _bns = new LinkedHashMap<String, String>();

    protected String getNameForBlankNode(String node) {
        if (!_bns.containsKey(node)) {
            _bns.put(node, _ng.next());
        }
        return _bns.get(node);
    }

    public void setPrefix(String fullUri, String prefix) {
        // TODO: graphs?
        // _context.put(prefix, fullUri);
    }

    public String getID(Resource r) {
        String rval = null;
        if (r.isAnon()) {
            rval = getNameForBlankNode(r.getId().toString());
        } else {
            rval = r.getURI();
        }
        return rval;
    }

    public void importModel(RDFDataset result, Model model) {

        // TODO: figure out what to do with this, as setPrefix itself currently
        // does nothing
        // add the prefixes to the context
        final Map<String, String> nsPrefixMap = model.getNsPrefixMap();
        for (final String prefix : nsPrefixMap.keySet()) {
            result.setNamespace(prefix, nsPrefixMap.get(prefix));
        }

        // iterate over the list of subjects and add the edges to the json-ld
        // document
        final ResIterator subjects = model.listSubjects();
        while (subjects.hasNext()) {
            final Resource subject = subjects.next();
            importResource(result, subject);
        }
    }

    public void importResource(RDFDataset result, Resource subject) {
        final String subj = getID(subject);
        final StmtIterator statements = subject.getModel().listStatements(subject, (Property) null,
                (RDFNode) null);
        while (statements.hasNext()) {
            final Statement statement = statements.next();
            final Property predicate = statement.getPredicate();
            final RDFNode object = statement.getObject();

            if (object.isLiteral()) {
                final Literal literal = object.asLiteral();
                final String value = literal.getLexicalForm();
                final String datatypeURI = literal.getDatatypeURI();
                String language = literal.getLanguage();
                if ("".equals(language)) {
                    language = null;
                }

                result.addTriple(subj, predicate.getURI(), value, datatypeURI, language);
            } else {
                final Resource resource = object.asResource();
                final String res = getID(resource);

                result.addTriple(subj, predicate.getURI(), res);
            }
        }
    }

    @Override
    public RDFDataset parse(Object input) throws JsonLdError {
        final RDFDataset result = new RDFDataset();
        // allow null input so we can use importModel and importResource before
        // calling fromRDF
        if (input == null) {
            return result;
        }
        if (input instanceof Resource) {
            importResource(result, (Resource) input);
        } else if (input instanceof Model) {
            importModel(result, (Model) input);
        } else {
            throw new JsonLdError(Error.INVALID_INPUT, "Jena Serializer expects Model or resource input");
        }
        return result;
    }
}
