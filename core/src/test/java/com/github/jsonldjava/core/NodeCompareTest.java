package com.github.jsonldjava.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.List;

import org.junit.Test;

import com.github.jsonldjava.core.RDFDataset.Literal;
import com.github.jsonldjava.core.RDFDataset.Node;
import com.github.jsonldjava.core.RDFDataset.Quad;

public class NodeCompareTest {

    @Test
    public void literalSameValue() throws Exception {
        Literal l1 = new RDFDataset.Literal("Same", null, null);
        Literal l2 = new RDFDataset.Literal("Same", null, null);
        assertEquals(l1, l2);
        assertEquals(0, l1.compareTo(l2));
    }

    @Test
    public void literalDifferentValue() throws Exception {
        Literal l1 = new RDFDataset.Literal("Same", null, null);
        Literal l2 = new RDFDataset.Literal("Different", null, null);
        assertNotEquals(l1, l2);
        assertNotEquals(0, l1.compareTo(l2));
    }

    @Test
    public void literalSameValuSameLang() throws Exception {
        Literal l1 = new RDFDataset.Literal("Same", JsonLdConsts.RDF_LANGSTRING, "en");
        Literal l2 = new RDFDataset.Literal("Same", JsonLdConsts.RDF_LANGSTRING, "en");
        assertEquals(l1, l2);
        assertEquals(0, l1.compareTo(l2));
    }
    
    @Test
    public void literalDifferentValueSameLang() throws Exception {
        Literal l1 = new RDFDataset.Literal("Same", JsonLdConsts.RDF_LANGSTRING, "en");
        Literal l2 = new RDFDataset.Literal("Different", JsonLdConsts.RDF_LANGSTRING, "en");
        assertNotEquals(l1, l2);
        assertNotEquals(0, l1.compareTo(l2));
    }

    @Test
    public void literalSameValueDifferentLang() throws Exception {
        Literal l1 = new RDFDataset.Literal("Same", JsonLdConsts.RDF_LANGSTRING, "en");
        Literal l2 = new RDFDataset.Literal("Same", JsonLdConsts.RDF_LANGSTRING, "no");
        assertNotEquals(l1, l2);
        assertNotEquals(0, l1.compareTo(l2));
    }    
    
    @Test
    public void literalSameValueSameType() throws Exception {
        Literal l1 = new RDFDataset.Literal("1", JsonLdConsts.XSD_INTEGER, null);
        Literal l2 = new RDFDataset.Literal("1", JsonLdConsts.XSD_INTEGER, null);
        assertEquals(l1, l2);
        assertEquals(0, l1.compareTo(l2));
    }

    @Test
    public void literalSameValueDifferentType() throws Exception {
        Literal l1 = new RDFDataset.Literal("1", JsonLdConsts.XSD_INTEGER, null);
        Literal l2 = new RDFDataset.Literal("1", JsonLdConsts.XSD_STRING, null);
        assertNotEquals(l1, l2);
        assertNotEquals(0, l1.compareTo(l2));
    }
    

    
    @Test
    public void literalsInDataset() throws Exception {
        RDFDataset dataset = new RDFDataset();
        dataset.addQuad("http://example.com/p", "http://example.com/p", "Same", null, null, "http://example.com/g1");
        dataset.addQuad("http://example.com/p", "http://example.com/p", "Different", null, null, "http://example.com/g1");
        List<Quad> quads = dataset.getQuads("http://example.com/g1");
        Quad q1 = quads.get(0);
        Quad q2 = quads.get(1);
        assertNotEquals(q1, q2);
        assertNotEquals(0, q1.compareTo(q2));
        assertNotEquals(0, q1.getObject().compareTo(q2.getObject()));
    }

    @Test
    public void iriDifferentLiteral() throws Exception {
        Node iri = new RDFDataset.IRI("http://example.com/");
        Node literal = new RDFDataset.Literal("http://example.com/", null, null);
        assertNotEquals(iri, literal);
        assertNotEquals(0, iri.compareTo(literal));
    }

    @Test
    public void iriDifferentIri() throws Exception {
        Node iri = new RDFDataset.IRI("http://example.com/");
        Node other = new RDFDataset.IRI("http://example.com/other");
        assertNotEquals(iri, other);
        assertNotEquals(0, iri.compareTo(other));
    }
    
    @Test
    public void iriSameIri() throws Exception {
        Node iri = new RDFDataset.IRI("http://example.com/same");
        Node same = new RDFDataset.IRI("http://example.com/same");
        assertEquals(iri, same);
        assertEquals(0, iri.compareTo(same));
    }
        
    @Test
    public void iriDifferentBlankNode() throws Exception {
        // We'll use a relative IRI to avoid :-issues
        Node iri = new RDFDataset.IRI("b1");
        Node bnode = new RDFDataset.BlankNode("b1");
        assertNotEquals(iri, bnode);
        assertNotEquals(0, iri.compareTo(bnode));
    }

    @Test
    public void literalDifferentIri() throws Exception {
        Node literal = new RDFDataset.Literal("http://example.com/", null, null);
        Node iri = new RDFDataset.IRI("http://example.com/");
        assertNotEquals(literal, iri);
        assertNotEquals(0, literal.compareTo(iri));
    }
    
    @Test
    public void literalDifferentBlankNode() throws Exception {
        // We'll use a relative IRI to avoid :-issues
        Node literal = new RDFDataset.Literal("b1", null, null);
        Node bnode = new RDFDataset.BlankNode("b1");
        assertNotEquals(literal, bnode);
        assertNotEquals(0, literal.compareTo(bnode));
    }


    
    
}
