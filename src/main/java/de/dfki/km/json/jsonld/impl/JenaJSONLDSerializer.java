package de.dfki.km.json.jsonld.impl;

import java.util.Map;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class JenaJSONLDSerializer extends de.dfki.km.json.jsonld.JSONLDSerializer {

    // private static final Logger LOG = LoggerFactory.getLogger(
    // JenaJSONLDSerializer.class );

    public String getID(Resource r) {
        String rval = null;
        if (r.isAnon()) {
            rval = getNameForBlankNode(r.getId().toString());
        } else {
            rval = r.getURI();
        }
        return rval;
    }

    public void importModel(Model model) {

        // add the prefixes to the context
        Map<String, String> nsPrefixMap = model.getNsPrefixMap();
        for (String prefix : nsPrefixMap.keySet()) {
            setPrefix(prefix, nsPrefixMap.get(prefix));
        }

        // iterate over the list of subjects and add the edges to the json-ld
        // document
        ResIterator subjects = model.listSubjects();
        while (subjects.hasNext()) {

            Resource subject = subjects.next();
            String subj = getID(subject);

            StmtIterator statements = model.listStatements(subject, (Property) null, (RDFNode) null);
            while (statements.hasNext()) {
                Statement statement = statements.next();
                Property predicate = statement.getPredicate();
                RDFNode object = statement.getObject();

                if (object.isLiteral()) {
                    Literal literal = object.asLiteral();
                    String value = literal.getLexicalForm();
                    String datatypeURI = literal.getDatatypeURI();
                    String language = literal.getLanguage();

                    triple(subj, predicate.getURI(), value, datatypeURI, language);
                } else {
                    Resource resource = object.asResource();
                    String res = getID(resource);

                    triple(subj, predicate.getURI(), res);
                }
            }

        }
    }

    public void importResource(Resource r) {
        Model model = r.getModel();
        if (model == null) {
            return;
        }
        importModel(model);
    }
}
