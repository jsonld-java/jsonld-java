package com.github.jsonldjava.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;

import org.junit.Test;

import com.github.jsonldjava.impl.TurtleRDFParser.Regex;

public class TurtleRegexTests {

    private void printMatcher(Matcher matcher) {
        if (matcher.matches()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                System.out.println(matcher.group(i));
            }
        }
    }

    @Test
    public void test_PREFIX_ID() {
        Matcher matcher = Regex.PREFIX_ID.matcher("@prefix : <http://www.google.com/test#> .");
        assertTrue(matcher.matches());
        assertEquals(2, matcher.groupCount());
        assertNotNull(matcher.group(1));
        assertNotNull(matcher.group(2));
        assertEquals("", matcher.group(1));
        assertEquals("http://www.google.com/test#", matcher.group(2));

        matcher = Regex.PREFIX_ID.matcher("@prefix abcdef: <http://www.google.com/test#> .");
        assertTrue(matcher.matches());
        assertEquals(2, matcher.groupCount());
        assertNotNull(matcher.group(1));
        assertNotNull(matcher.group(2));
        assertEquals("abcdef", matcher.group(1));
        assertEquals("http://www.google.com/test#", matcher.group(2));
    }

    @Test
    public void test_BASE() {
        final Matcher matcher = Regex.BASE.matcher("@base <http://www.google.com/test#> .");
        assertTrue(matcher.matches());
        assertEquals(1, matcher.groupCount());
        assertNotNull(matcher.group(1));
        assertEquals("http://www.google.com/test#", matcher.group(1));
    }

    @Test
    public void test_SPARQL_PREFIX() {
        Matcher matcher = Regex.SPARQL_PREFIX.matcher("PREFix : <http://www.google.com/test#>");
        assertTrue(matcher.matches());
        assertEquals(2, matcher.groupCount());
        assertNotNull(matcher.group(1));
        assertNotNull(matcher.group(2));
        assertEquals("", matcher.group(1));
        assertEquals("http://www.google.com/test#", matcher.group(2));

        matcher = Regex.SPARQL_PREFIX.matcher("prefIX abcdef: <http://www.google.com/test#>");
        assertTrue(matcher.matches());
        assertEquals(2, matcher.groupCount());
        assertNotNull(matcher.group(1));
        assertNotNull(matcher.group(2));
        assertEquals("abcdef", matcher.group(1));
        assertEquals("http://www.google.com/test#", matcher.group(2));
    }

    @Test
    public void test_SPARQL_BASE() {
        final Matcher matcher = Regex.SPARQL_BASE.matcher("BaSe <http://www.google.com/test#>");
        assertTrue(matcher.matches());
        assertEquals(1, matcher.groupCount());
        assertNotNull(matcher.group(1));
        assertEquals("http://www.google.com/test#", matcher.group(1));
    }

    @Test
    public void test_DIRECTIVE() {
        Matcher matcher = Regex.DIRECTIVE.matcher("@prefix : <http://www.google.com/test#> .");
        assertTrue(matcher.matches());
        assertEquals(6, matcher.groupCount());
        assertEquals("", matcher.group(1));
        assertEquals("http://www.google.com/test#", matcher.group(2));

        matcher = Regex.DIRECTIVE.matcher("@prefix abc: <http://www.google.com/test#> .");
        assertTrue(matcher.matches());
        assertEquals(6, matcher.groupCount());
        assertEquals("abc", matcher.group(1));
        assertEquals("http://www.google.com/test#", matcher.group(2));

        matcher = Regex.DIRECTIVE.matcher("@base <http://www.google.com/test#> .");
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertNull(matcher.group(2));
        assertEquals("http://www.google.com/test#", matcher.group(3));

        matcher = Regex.DIRECTIVE.matcher("PREFix : <http://www.google.com/test#>");
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertNull(matcher.group(2));
        assertNull(matcher.group(3));
        assertEquals("", matcher.group(4));
        assertEquals("http://www.google.com/test#", matcher.group(5));

        matcher = Regex.DIRECTIVE.matcher("PREFix abc: <http://www.google.com/test#>");
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertNull(matcher.group(2));
        assertNull(matcher.group(3));
        assertEquals("abc", matcher.group(4));
        assertEquals("http://www.google.com/test#", matcher.group(5));

        matcher = Regex.DIRECTIVE.matcher("BASE <http://www.google.com/test#>");
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertNull(matcher.group(2));
        assertNull(matcher.group(3));
        assertNull(matcher.group(4));
        assertNull(matcher.group(5));
        assertEquals("http://www.google.com/test#", matcher.group(6));
    }

    @Test
    public void test_PREFIXED_NAME() {
        assertTrue("abc:def".matches("" + Regex.PREFIXED_NAME));
        assertTrue(":def".matches("" + Regex.PREFIXED_NAME));
    }

    @Test
    public void test_IRI() {
        Matcher matcher = Regex.IRI.matcher("<http://www.google.com/test#hello>");
        assertTrue(matcher.matches());
        assertEquals(4, matcher.groupCount());
        assertEquals("http://www.google.com/test#hello", matcher.group(1));
        assertNull(matcher.group(2));
        assertNull(matcher.group(3));
        assertNull(matcher.group(4));

        matcher = Regex.IRI.matcher("abc:def");
        assertTrue(matcher.matches());
        assertEquals(4, matcher.groupCount());
        assertNull(matcher.group(1));
        assertEquals("abc", matcher.group(2));
        assertEquals("def", matcher.group(3));
        assertNull(matcher.group(4));

        matcher = Regex.IRI.matcher("hij:");
        assertTrue(matcher.matches());
        assertEquals(4, matcher.groupCount());
        assertNull(matcher.group(1));
        assertNull(matcher.group(2));
        assertNull(matcher.group(3));
        assertEquals("hij", matcher.group(4));
    }

    @Test
    public void test_ANON() {
        assertTrue("[ ]".matches("^" + Regex.ANON + "$"));
    }

    @Test
    public void test_BLANK_NODE() {
        Matcher matcher = Regex.BLANK_NODE.matcher("_:b0");
        assertTrue(matcher.matches());
        assertEquals(1, matcher.groupCount());
        assertEquals("b0", matcher.group(1));

        matcher = Regex.BLANK_NODE.matcher("[ ]");
        assertTrue(matcher.matches());
        assertEquals(1, matcher.groupCount());
        assertEquals(null, matcher.group(1));
    }

    @Test
    public void test_STRING() {
        assertTrue("\"dffhjkasdhfskldhfoiw'eu\\\"fhowleifh\u00F8\u02FF\u0370\u037D\""
                .matches("^" + Regex.STRING + "$"));
        assertFalse("\"dffhjkasdhfs\nkldhfoiw\\\"'eufhowleifh \u00F8\u02FF\u0370\u037D\""
                .matches("^" + Regex.STRING + "$"));
        assertTrue("'dffhjkasdhfskldh\\'foiweu\"fhowleifh \u00F8\u02FF\u0370\u037D'"
                .matches("^" + Regex.STRING + "$"));
        assertFalse("\"dffhjkasdhfs\nkldhfoiw\\\"'eufhowleifh \u00F8\u02FF\u0370\u037D\""
                .matches("^" + Regex.STRING + "$"));
        assertTrue("'''dffhjkasdhfsk\nldhfoiw\"'eufhowleifh \u00F8\u02FF\u0370\u037D'''"
                .matches("^" + Regex.STRING + "$"));
        assertTrue("\"\"\"dffhjkasdhfsk\nldhfoiw\"'eufhowleifh \u00F8\u02FF\u0370\u037D\"\"\""
                .matches("^" + Regex.STRING + "$"));

        Matcher matcher = Regex.STRING.matcher("'''x''y'''");
        assertTrue(matcher.find());
        assertEquals("'''x''y'''", matcher.group(1));

        matcher = Regex.STRING.matcher("'''" + (char) 0xA + "''' .");
        assertTrue(matcher.find());
        assertEquals("'''" + (char) 0xA + "'''", matcher.group(1));

        matcher = Regex.STRING.matcher("'''\f'''");
        assertTrue(matcher.find());
        assertEquals("'''\f'''", matcher.group(1));

        assertFalse("'''x'''y'''".matches("^" + Regex.STRING + "$"));
        assertFalse("\"\"\"x\"\"\"y\"\"\"".matches("^" + Regex.STRING + "$"));
    }

    @Test
    public void test_BOOLEAN_LITERAL() {
        assertTrue("true".matches("^" + Regex.BOOLEAN_LITERAL + "$"));
        assertTrue("false".matches("^" + Regex.BOOLEAN_LITERAL + "$"));
    }

    @Test
    public void test_RDF_LITERAL() {
        Matcher matcher = Regex.RDF_LITERAL.matcher("\"hello\"@en");
        assertTrue(matcher.matches());
        assertEquals(6, matcher.groupCount());
        assertEquals("\"hello\"", matcher.group(1));
        assertEquals("en", matcher.group(2));
        assertNull(matcher.group(3));
        assertNull(matcher.group(4));
        assertNull(matcher.group(5));
        assertNull(matcher.group(6));

        matcher = Regex.RDF_LITERAL.matcher("\"123\"^^xsd:integer");
        assertTrue(matcher.matches());
        assertEquals(6, matcher.groupCount());
        assertEquals("\"123\"", matcher.group(1));
        assertNull(matcher.group(2));
        assertNull(matcher.group(3));
        assertEquals("xsd", matcher.group(4));
        assertEquals("integer", matcher.group(5));
        assertNull(matcher.group(6));

        matcher = Regex.RDF_LITERAL.matcher("\"123\"^^<http://fake/type>");
        assertTrue(matcher.matches());
        assertEquals(6, matcher.groupCount());
        assertEquals("\"123\"", matcher.group(1));
        assertNull(matcher.group(2));
        assertEquals("http://fake/type", matcher.group(3));
        assertNull(matcher.group(4));
        assertNull(matcher.group(5));
        assertNull(matcher.group(6));

        matcher = Regex.RDF_LITERAL.matcher("\"123\"^^def:");
        assertTrue(matcher.matches());
        assertEquals(6, matcher.groupCount());
        assertEquals("\"123\"", matcher.group(1));
        assertNull(matcher.group(2));
        assertNull(matcher.group(3));
        assertNull(matcher.group(4));
        assertNull(matcher.group(5));
        assertEquals("def", matcher.group(6));
    }

    @Test
    public void test_NUMERIC_LITERAL() {
        Matcher matcher = Regex.NUMERIC_LITERAL.matcher("3E1");
        assertTrue(matcher.matches());
        assertEquals(3, matcher.groupCount());
        assertEquals("3E1", matcher.group(1));

        matcher = Regex.NUMERIC_LITERAL.matcher("-5.1E-10000");
        assertTrue(matcher.matches());
        assertEquals("-5.1E-10000", matcher.group(1));

        matcher = Regex.NUMERIC_LITERAL.matcher("2.01");
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertEquals("2.01", matcher.group(2));

        matcher = Regex.NUMERIC_LITERAL.matcher("123");
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertNull(matcher.group(2));
        assertEquals("123", matcher.group(3));

        matcher = Regex.NUMERIC_LITERAL.matcher("-1");
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertNull(matcher.group(2));
        assertEquals("-1", matcher.group(3));
    }

    @Test
    public void test_SUBJECT() {
        Matcher matcher = Regex.SUBJECT.matcher("<http://www.google.com/test#hello>");
        assertTrue(matcher.matches());
        assertEquals(5, matcher.groupCount());
        assertEquals("http://www.google.com/test#hello", matcher.group(1));

        matcher = Regex.SUBJECT.matcher("abc:def");
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertEquals("abc", matcher.group(2));
        assertEquals("def", matcher.group(3));

        matcher = Regex.SUBJECT.matcher("hij:");
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertNull(matcher.group(2));
        assertNull(matcher.group(3));
        assertEquals("hij", matcher.group(4));

        matcher = Regex.SUBJECT.matcher("_:b0");
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertNull(matcher.group(2));
        assertNull(matcher.group(3));
        assertNull(matcher.group(4));
        assertEquals("b0", matcher.group(5));

        // a subject match without any matching groups should == an anonymous
        // subject
        matcher = Regex.SUBJECT.matcher("[ ]");
        assertTrue(matcher.matches());
    }

    @Test
    public void test_PREDICATE() {
        Matcher matcher = Regex.PREDICATE.matcher("<http://www.google.com/test#hello>");
        assertTrue(matcher.matches());
        assertEquals(4, matcher.groupCount());
        assertEquals("http://www.google.com/test#hello", matcher.group(1));

        matcher = Regex.PREDICATE.matcher("abc:def");
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertEquals("abc", matcher.group(2));
        assertEquals("def", matcher.group(3));

        matcher = Regex.PREDICATE.matcher("hij:");
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertNull(matcher.group(2));
        assertNull(matcher.group(3));
        assertEquals("hij", matcher.group(4));

        // match rdf:type shorthand
        matcher = Regex.PREDICATE.matcher("a ");
        assertTrue(matcher.matches());
        assertEquals("a ", matcher.group(0));
        assertNull(matcher.group(1));
        assertNull(matcher.group(2));
        assertNull(matcher.group(3));
        assertNull(matcher.group(4));
    }

    @Test
    public void test_LITERAL() {
        Matcher matcher = Regex.LITERAL.matcher("\"hello\"@en");
        assertTrue(matcher.matches());
        assertEquals(10, matcher.groupCount());
        assertEquals("\"hello\"", matcher.group(1));
        assertEquals("en", matcher.group(2));

        matcher = Regex.LITERAL.matcher("\"123\"^^xsd:integer");
        assertTrue(matcher.matches());
        assertEquals("\"123\"", matcher.group(1));
        assertNull(matcher.group(2));
        assertNull(matcher.group(3));
        assertEquals("xsd", matcher.group(4));
        assertEquals("integer", matcher.group(5));

        matcher = Regex.LITERAL.matcher("\"123\"^^<http://fake/type>");
        assertTrue(matcher.matches());
        assertEquals("\"123\"", matcher.group(1));
        assertNull(matcher.group(2));
        assertEquals("http://fake/type", matcher.group(3));

        matcher = Regex.LITERAL.matcher("\"123\"^^def:");
        assertTrue(matcher.matches());
        assertEquals("\"123\"", matcher.group(1));
        assertNull(matcher.group(2));
        assertNull(matcher.group(3));
        assertNull(matcher.group(4));
        assertNull(matcher.group(5));
        assertEquals("def", matcher.group(6));

        matcher = Regex.LITERAL.matcher("123");
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertNull(matcher.group(2));
        assertNull(matcher.group(3));
        assertNull(matcher.group(4));
        assertNull(matcher.group(5));
        assertNull(matcher.group(6));
        assertNull(matcher.group(7));
        assertNull(matcher.group(8));
        assertEquals("123", matcher.group(9));

        matcher = Regex.LITERAL.matcher("-1");
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertNull(matcher.group(2));
        assertNull(matcher.group(3));
        assertNull(matcher.group(4));
        assertNull(matcher.group(5));
        assertNull(matcher.group(6));
        assertNull(matcher.group(7));
        assertNull(matcher.group(8));
        assertEquals("-1", matcher.group(9));

        matcher = Regex.LITERAL.matcher("2.01");
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertNull(matcher.group(2));
        assertNull(matcher.group(3));
        assertNull(matcher.group(4));
        assertNull(matcher.group(5));
        assertNull(matcher.group(6));
        assertNull(matcher.group(7));
        assertEquals("2.01", matcher.group(8));
        assertNull(matcher.group(9));

        matcher = Regex.LITERAL.matcher("3E1");
        assertTrue(matcher.matches());
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertNull(matcher.group(2));
        assertNull(matcher.group(3));
        assertNull(matcher.group(4));
        assertNull(matcher.group(5));
        assertNull(matcher.group(6));
        assertEquals("3E1", matcher.group(7));

        matcher = Regex.LITERAL.matcher("-5.1E-10000");
        assertTrue(matcher.matches());
        assertTrue(matcher.matches());
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertNull(matcher.group(2));
        assertNull(matcher.group(3));
        assertNull(matcher.group(4));
        assertNull(matcher.group(5));
        assertNull(matcher.group(6));
        assertEquals("-5.1E-10000", matcher.group(7));

        matcher = Regex.LITERAL.matcher("true");
        assertTrue(matcher.matches());
        assertTrue(matcher.matches());
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertNull(matcher.group(2));
        assertNull(matcher.group(3));
        assertNull(matcher.group(4));
        assertNull(matcher.group(5));
        assertNull(matcher.group(6));
        assertNull(matcher.group(7));
        assertNull(matcher.group(8));
        assertNull(matcher.group(9));
        assertEquals("true", matcher.group(10));

    }

    @Test
    public void test_OBJECT() {
        // IRIs should be at position 1
        Matcher matcher = Regex.OBJECT.matcher("<>");
        assertTrue(matcher.matches());
        assertEquals(15, matcher.groupCount());
        assertEquals("", matcher.group(1));

        matcher = Regex.OBJECT.matcher("<http://test>");
        assertTrue(matcher.matches());
        assertEquals("http://test", matcher.group(1));

        // prefixed names should be at pos 2 + 3
        matcher = Regex.OBJECT.matcher("abc:def");
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertEquals("abc", matcher.group(2));
        assertEquals("def", matcher.group(3));

        matcher = Regex.OBJECT.matcher(":def");
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertEquals("", matcher.group(2));
        assertEquals("def", matcher.group(3));

        // prefixes only should be at pos 4
        matcher = Regex.OBJECT.matcher("hij:");
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertNull(matcher.group(2));
        assertNull(matcher.group(3));
        assertEquals("hij", matcher.group(4));

        // blank node ids should be at pos 5
        matcher = Regex.OBJECT.matcher("_:b0");
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertNull(matcher.group(2));
        assertNull(matcher.group(3));
        assertNull(matcher.group(4));
        assertEquals("b0", matcher.group(5));

        // strings should be in position 6
        matcher = Regex.OBJECT.matcher("\"hello world\"");
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertNull(matcher.group(2));
        assertNull(matcher.group(3));
        assertNull(matcher.group(4));
        assertNull(matcher.group(5));
        assertEquals("\"hello world\"", matcher.group(6));
        assertNull(matcher.group(7));
        assertNull(matcher.group(8));
        assertNull(matcher.group(9));
        assertNull(matcher.group(11));

        // language taged strings should be at position 6 + 7 for langtag
        matcher = Regex.OBJECT.matcher("\"hello\"@en");
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertNull(matcher.group(2));
        assertNull(matcher.group(3));
        assertNull(matcher.group(4));
        assertNull(matcher.group(5));
        assertEquals("\"hello\"", matcher.group(6));
        assertEquals("en", matcher.group(7));

        // literals with IRI datatype should be at pos 6 + 8
        matcher = Regex.OBJECT.matcher("\"123\"^^<http://test/type>");
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertNull(matcher.group(2));
        assertNull(matcher.group(3));
        assertNull(matcher.group(4));
        assertNull(matcher.group(5));
        assertEquals("\"123\"", matcher.group(6));
        assertNull(matcher.group(7));
        assertEquals("http://test/type", matcher.group(8));

        // literals with ns:name datatype should be at pos 6 + 9 + 10
        matcher = Regex.OBJECT.matcher("\"123\"^^xsd:integer");
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertNull(matcher.group(2));
        assertNull(matcher.group(3));
        assertNull(matcher.group(4));
        assertNull(matcher.group(5));
        assertEquals("\"123\"", matcher.group(6));
        assertNull(matcher.group(7));
        assertNull(matcher.group(8));
        assertEquals("xsd", matcher.group(9));
        assertEquals("integer", matcher.group(10));

        // literals with ns: datatype should be at pos 6 + 11
        matcher = Regex.OBJECT.matcher("\"123\"^^def:");
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertNull(matcher.group(2));
        assertNull(matcher.group(3));
        assertNull(matcher.group(4));
        assertNull(matcher.group(5));
        assertEquals("\"123\"", matcher.group(6));
        assertNull(matcher.group(7));
        assertNull(matcher.group(8));
        assertNull(matcher.group(9));
        assertNull(matcher.group(10));
        assertEquals("def", matcher.group(11));

        // double literals should be at pos 12
        matcher = Regex.OBJECT.matcher("1.234E-10");
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertNull(matcher.group(2));
        assertNull(matcher.group(3));
        assertNull(matcher.group(4));
        assertNull(matcher.group(5));
        assertNull(matcher.group(6));
        assertNull(matcher.group(7));
        assertNull(matcher.group(8));
        assertNull(matcher.group(9));
        assertNull(matcher.group(10));
        assertNull(matcher.group(11));
        assertEquals("1.234E-10", matcher.group(12));

        // decimal literals should be at pos 13
        matcher = Regex.OBJECT.matcher("12.34");
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertNull(matcher.group(2));
        assertNull(matcher.group(3));
        assertNull(matcher.group(4));
        assertNull(matcher.group(5));
        assertNull(matcher.group(6));
        assertNull(matcher.group(7));
        assertNull(matcher.group(8));
        assertNull(matcher.group(9));
        assertNull(matcher.group(10));
        assertNull(matcher.group(11));
        assertNull(matcher.group(12));
        assertEquals("12.34", matcher.group(13));

        // integer literals should be at pos 14
        matcher = Regex.OBJECT.matcher("1234");
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertNull(matcher.group(2));
        assertNull(matcher.group(3));
        assertNull(matcher.group(4));
        assertNull(matcher.group(5));
        assertNull(matcher.group(6));
        assertNull(matcher.group(7));
        assertNull(matcher.group(8));
        assertNull(matcher.group(9));
        assertNull(matcher.group(10));
        assertNull(matcher.group(11));
        assertNull(matcher.group(12));
        assertNull(matcher.group(13));
        assertEquals("1234", matcher.group(14));

        // boolean literals should be at pos 15
        matcher = Regex.OBJECT.matcher("false");
        assertTrue(matcher.matches());
        assertNull(matcher.group(1));
        assertNull(matcher.group(2));
        assertNull(matcher.group(3));
        assertNull(matcher.group(4));
        assertNull(matcher.group(5));
        assertNull(matcher.group(6));
        assertNull(matcher.group(7));
        assertNull(matcher.group(8));
        assertNull(matcher.group(9));
        assertNull(matcher.group(10));
        assertNull(matcher.group(11));
        assertNull(matcher.group(12));
        assertNull(matcher.group(13));
        assertNull(matcher.group(14));
        assertEquals("false", matcher.group(15));

        matcher = Regex.OBJECT.matcher("\"IRI with four digit numeric escape (\\\\u)\" ;");
        assertTrue(matcher.find());
    }
}
