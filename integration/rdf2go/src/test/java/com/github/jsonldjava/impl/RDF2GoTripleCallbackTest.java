package com.github.jsonldjava.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.model.ModelSet;
import org.ontoware.rdf2go.model.Statement;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.utils.JSONUtils;

/**
 * Unit tests for {@link RDF2GoTripleCallback}.
 * 
 * @author Ismael Rivera
 */
public class RDF2GoTripleCallbackTest {

    @Test
    public void testToRDF() throws JsonParseException, JsonMappingException, JsonLdError {
        final String inputstring = "{ `@id`:`http://nonexistent.com/abox#Document1823812`, `@type`:`http://nonexistent.com/tbox#Document` }"
                .replace('`', '"');
        final String expectedString = "null - http://nonexistent.com/abox#Document1823812 - http://www.w3.org/1999/02/22-rdf-syntax-ns#type - http://nonexistent.com/tbox#Document";
        final Object input = JSONUtils.fromString(inputstring);

        final RDF2GoTripleCallback callback = new RDF2GoTripleCallback();

        final ModelSet model = (ModelSet) JsonLdProcessor.toRDF(input, callback);

        // contains only one statement (type)
        final ClosableIterator<Statement> statements = model.iterator();
        final Statement stmt = statements.next();
        assertEquals(expectedString, stmt.getContext() + " - " + stmt.toString());
        assertFalse("Deserialized RDF contains more triples than expected", statements.hasNext());
    }

}
