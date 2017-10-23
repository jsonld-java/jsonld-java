package com.github.jsonldjava.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import com.github.jsonldjava.core.RDFDataset.Quad;

public class QuadCompareTest {

    Quad q = new Quad("http://example.com/s1", "http://example.com/p1", "http://example.com/o1",
            "http://example.com/g1");

    @Test
    public void compareToNull() throws Exception {
        assertNotEquals(0, q.compareTo(null));
    }

    @Test
    public void compareToSame() throws Exception {
        final Quad q2 = new Quad("http://example.com/s1", "http://example.com/p1",
                "http://example.com/o1", "http://example.com/g1");
        assertEquals(0, q.compareTo(q2));
        // Should still compare equal, even if extra attributes are added
        q2.put("example", "value");
        assertEquals(0, q.compareTo(q2));
    }

    @Test
    public void compareToDifferentGraph() throws Exception {
        final Quad q2 = new Quad("http://example.com/s1", "http://example.com/p1",
                "http://example.com/o1", "http://example.com/other");
        assertNotEquals(0, q.compareTo(q2));
    }

    @Test
    public void compareToDifferentSubject() throws Exception {
        final Quad q2 = new Quad("http://example.com/other", "http://example.com/p1",
                "http://example.com/o1", "http://example.com/g1");
        assertNotEquals(0, q.compareTo(q2));
    }

    @Test
    public void compareToDifferentPredicate() throws Exception {
        final Quad q2 = new Quad("http://example.com/s1", "http://example.com/other",
                "http://example.com/o1", "http://example.com/g1");
        assertNotEquals(0, q.compareTo(q2));
    }

    @Test
    public void compareToDifferentObject() throws Exception {
        final Quad q2 = new Quad("http://example.com/s1", "http://example.com/p1",
                "http://example.com/other", "http://example.com/g1");
        assertNotEquals(0, q.compareTo(q2));
    }

    @Test
    public void compareToDifferentObjectType() throws Exception {
        final Quad q2 = new Quad("http://example.com/s1", "http://example.com/p1",
                "http://example.com/other", null, null, // literal
                "http://example.com/g1");
        assertNotEquals(0, q.compareTo(q2));
    }

}
