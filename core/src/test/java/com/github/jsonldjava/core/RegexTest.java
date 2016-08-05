package com.github.jsonldjava.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

public class RegexTest {
    @Test
    public void test_TRICKY_UTF_CHARS() throws IOException {
        // make sure all the characters in the range are matched
        for (int i = 0x10000; i <= 0xeffff; i++) {
            final char[] u1 = Character.toChars(i);
            // char[] u2 = Character.toChars(0xeffff);
            final String test = Character.toString(u1[0]) + Character.toString(u1[1]);// +
            // Character.toString(u2[0])
            // +
            // Character.toString(u2[1]);
            // Matcher matcher = Regex.TRICKY_UTF_CHARS.matcher(test);
            // while (matcher.find()) {
            // String s = matcher.group(0);
            // for (int i = 0; i < s.length(); i++) {
            // System.out.print("0x" + Integer.toHexString(s.codePointAt(i)) +
            // " ");
            // }
            // System.out.println();
            // }
            assertTrue(test.matches("" + Regex.TRICKY_UTF_CHARS + ""));
        }
    }

    @Test
    public void test_PN_CHARS_BASE() throws IOException {
        final String test = "\u0041\u005A\u0061\u007A\u00C0\u00D6\u00D8\u00F6\u00F8\u02FF\u0370\u037D\u037F\u1FFF\u200C\u200D\u2070\u218F\u2C00\u2FEF\u3001\uD7FF\uF900\uFDCF\uFDF0\uFFFD\uD800\uDC00\uDB7F\uDFFF";
        assertTrue(test.matches("^(?:" + Regex.PN_CHARS_BASE + ")+$"));
        assertFalse("_".matches("^" + Regex.PN_CHARS_BASE + "$"));
    }

    @Test
    public void test_PN_CHARS_U() {
        final String test = "\u0041\u005A\u0061__\u007A\u00C0\u00D6\u00D8\u00F6\u00F8\u02FF\u0370\u037D\u037F\u1FFF\u200C\u200D\u2070\u218F\u2C00\u2FEF\u3001\uD7FF\uF900\uFDCF\uFDF0\uFFFD\uD800\uDC00\uDB7F\uDFFF";
        assertTrue(test.matches("^(?:" + Regex.PN_CHARS_U + ")+$"));
    }

    @Test
    public void test_PN_CHARS() {
        final String test = "\u0041\u005A\u0061_-_\u007A\u00C0\u00D6\u00D8\u00F6\u00F8\u02FF\u0370\u037D\u037F\u1FFF\u200C\u200D\u2070\u218F\u2C00\u2FEF\u3001\uD7FF\uF900\uFDCF\uFDF0\uFFFD\uD800\uDC00\uDB7F\uDFFF";
        assertTrue(test.matches("^(?:" + Regex.PN_CHARS + ")+$"));
    }

    @Test
    public void test_PN_PREFIX() {
        final String testT = "\u0041\u005A\u0061.\u007A\u00C0\u00D6\u00D8\u00F6\u00F8\u02FF\u0370\u037D\u037F\u1FFF\u200C\u200D\u2070\u218F\u2C00\u2FEF\u3001\uD7FF\uF900\uFDCF\uFDF0\uFFFD\uD800\uDC00\uDB7F\uDFFF";
        final String testF = "\u0041\u005A\u0061.\u007A\u00C0\u00D6\u00D8\u00F6\u00F8\u02FF\u0370\u037D\u037F\u1FFF\u200C\u200D\u2070\u218F\u2C00\u2FEF\u3001\uD7FF\uF900\uFDCF\uFDF0\uFFFD\uD800\uDC00\uDB7F\uDFFF.";
        assertTrue(testT.matches("^" + Regex.PN_PREFIX + "$"));
        assertFalse(testF.matches("^" + Regex.PN_PREFIX + "$"));
        assertFalse("_:b".matches("^" + Regex.PN_PREFIX + "$"));
    }

    @Test
    public void test_HEX() {
        assertTrue("0123456789ABCDEFabcdef".matches("^" + Regex.HEX + "+$"));
        assertFalse("0123456789ABCDEFabcdefg".matches("^" + Regex.HEX + "+$"));
    }

    @Test
    public void test_PN_LOCAL_ESC() {
        assertTrue("\\&\\!\\_\\~".matches("^(?:" + Regex.PN_LOCAL_ESC + ")+$"));
    }

    @Test
    public void test_PERCENT() {
        assertTrue("%A0".matches("^" + Regex.PERCENT + "$"));
    }

    @Test
    public void test_PLX() {
        assertTrue("%A0".matches("^" + Regex.PLX + "$"));
        assertTrue("\\&\\!\\_\\~%A0".matches("^(?:" + Regex.PLX + ")+$"));
    }

    @Test
    public void test_PN_LOCAL() {
        assertTrue(":abcdef.:%FF\\#".matches("^" + Regex.PN_LOCAL + "$"));
    }

    @Test
    public void test_PNAME_NS() {
        assertTrue(":".matches("^" + Regex.PNAME_NS + "$"));
        assertTrue("abc:".matches("^" + Regex.PNAME_NS + "$"));
        assertTrue("\u00F8\u02FF\u0370\u037D:".matches("^" + Regex.PNAME_NS + "$"));
    }

    @Test
    public void test_PNAME_LN() {
        assertTrue(":p".matches("^" + Regex.PNAME_LN + "$"));
        assertTrue("abc:def".matches("^" + Regex.PNAME_LN + "$"));
        assertTrue("\u00F8\u02FF\u0370\u037D:\u00F8\u02FF\u0370\u037D"
                .matches("^" + Regex.PNAME_LN + "$"));
    }

    @Test
    public void test_UCHAR() {
        assertTrue("\\u0ABC".matches("^" + Regex.UCHAR + "$"));
        assertTrue("\\U0123ABCD".matches("^" + Regex.UCHAR + "$"));
    }

    @Test
    public void test_ECHAR() {
        assertTrue("\\t".matches("^" + Regex.ECHAR + "$"));
        assertTrue("\\\"".matches("^" + Regex.ECHAR + "$"));
        assertTrue("\\\\".matches("^" + Regex.ECHAR + "$"));
    }

    @Test
    public void test_IRIREF() {
        final Matcher matcher = Regex.IRIREF.matcher("<http://www.google.com/test#hello>");
        assertTrue(matcher.matches());
        assertEquals(1, matcher.groupCount());
        assertNotNull(matcher.group(1));
        assertEquals("http://www.google.com/test#hello", matcher.group(1));
    }

    @Test
    public void test_BLANK_NODE_LABEL() {
        assertTrue("_:abcd".matches("^" + Regex.BLANK_NODE_LABEL + "$"));
        assertTrue("_:abcd\u007A".matches("^" + Regex.BLANK_NODE_LABEL + "$"));
        assertTrue("_:\u00C0\u0041\u005A\u0061\u007A".matches("^" + Regex.BLANK_NODE_LABEL + "$"));
        assertTrue("_:abcd\u00C0".matches("^" + Regex.BLANK_NODE_LABEL + "$"));

        final Matcher matcher = Regex.BLANK_NODE_LABEL.matcher("_:\u00F8\u02FF\u0370\u037D");
        assertTrue(matcher.matches());
        assertEquals(1, matcher.groupCount());
        assertEquals("\u00F8\u02FF\u0370\u037D", matcher.group(1));

    }

    @Test
    public void test_STRING_LITERAL_QUOTE() {
        final Matcher matcher = Regex.STRING_LITERAL_QUOTE
                .matcher("\"IRI with four digit numeric escape (\\\\u)\" ;");
        assertTrue(matcher.find());

        assertTrue("\"dffhjkasdhfskldhfoiw'eu\\\"fhowleifh \u00F8\u02FF\u0370\u037D\""
                .matches("^" + Regex.STRING_LITERAL_QUOTE + "$"));
        assertFalse("\"dffhjkasdhfs\nkldhfoiw\\\"'eufhowleifh \u00F8\u02FF\u0370\u037D\""
                .matches("^" + Regex.STRING_LITERAL_QUOTE + "$"));
    }

    @Test
    public void test_STRING_LITERAL_SINGLE_QUOTE() {
        assertTrue("'dffhjkasdhfskldhf\\'oiweu\"fhowleifh \u00F8\u02FF\u0370\u037D'"
                .matches("^" + Regex.STRING_LITERAL_SINGLE_QUOTE + "$"));
        assertFalse("\"dffhjkasdhfs\nkldhfoiw\\\"'eufhowleifh \u00F8\u02FF\u0370\u037D\""
                .matches("^" + Regex.STRING_LITERAL_SINGLE_QUOTE + "$"));
    }

    @Test
    public void test_STRING_LITERAL_LONG_SINGLE_QUOTE() {
        assertTrue("'''dffhjkasdhfsk\nldhfoiw\"'eufhowleifh \u00F8\u02FF\u0370\u037D'''"
                .matches("^" + Regex.STRING_LITERAL_LONG_SINGLE_QUOTE + "$"));
    }

    @Test
    public void test_STRING_LITERAL_LONG_QUOTE() {
        assertTrue("\"\"\"dffhjkasdhfsk\nldhfoiw\"'eufhowleifh \u00F8\u02FF\u0370\u037D\"\"\""
                .matches("^" + Regex.STRING_LITERAL_LONG_QUOTE + "$"));
    }

    @Test
    public void test_LANGTAG() {
        assertTrue("@en".matches("^" + Regex.LANGTAG + "$"));
        assertTrue("@abc-def".matches("^" + Regex.LANGTAG + "$"));
    }

    @Test
    public void test_unescape() {
        String r = RDFDatasetUtils.unescape("\\u007A");
        assertTrue("\u007A".equals(r));

        r = RDFDatasetUtils.unescape("\\U000F0000");
        assertTrue("\uDB80\uDC00".equals(r));

        r = RDFDatasetUtils.unescape("\\U00010000");
        assertTrue("\uD800\uDC00".equals(r));

        r = RDFDatasetUtils.unescape("\\U00100000");
        assertTrue("\uDBC0\uDC00".equals(r));

        r = RDFDatasetUtils.unescape("\\t");
        assertTrue("\t".equals(r));

        r = RDFDatasetUtils.unescape("\\t\\u007A\\U000F0000\\U00010000\\n");
        assertTrue("\t\u007A\uDB80\uDC00\uD800\uDC00\n".equals(r));

        r = RDFDatasetUtils.unescape(
                "http://a.example/AZaz\u00c0\u00d6\u00d8\u00f6\u00f8\u02ff\u0370\u037d\u0384\u1ffe\u200c\u200d\u2070\u2189\u2c00\u2fd5\u3001\ud7fb\ufa0e\ufdc7\ufdf0\uffef");
        assertTrue(
                "http://a.example/AZaz\u00c0\u00d6\u00d8\u00f6\u00f8\u02ff\u0370\u037d\u0384\u1ffe\u200c\u200d\u2070\u2189\u2c00\u2fd5\u3001\ud7fb\ufa0e\ufdc7\ufdf0\uffef"
                        .equals(r));

        r = RDFDatasetUtils.unescape(
                "http://a.example/AZaz\\u00c0\\u00d6\\u00d8\\u00f6\\u00f8\\u02ff\\u0370\\u037d\\u0384\\u1ffe\\u200c\\u200d\\u2070\\u2189\\u2c00\\u2fd5\\u3001\\ud7fb\\ufa0e\\ufdc7\\ufdf0\\uffef\\U00010000\\U000e01ef");
        assertTrue(
                "http://a.example/AZaz\u00c0\u00d6\u00d8\u00f6\u00f8\u02ff\u0370\u037d\u0384\u1ffe\u200c\u200d\u2070\u2189\u2c00\u2fd5\u3001\ud7fb\ufa0e\ufdc7\ufdf0\uffef\uD800\uDC00\uDB40\uDDEF"
                        .equals(r));
    }

    @Test
    public void testDoubleRegex() throws Exception {
        assertTrue(Pattern.matches("^(\\+|-)?([0-9]+(\\.[0-9]*)?|\\.[0-9]+)([Ee](\\+|-)?[0-9]+)?$",
                "1.1E-1"));
    }
}
