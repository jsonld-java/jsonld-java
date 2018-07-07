package com.github.jsonldjava.core;

import static com.github.jsonldjava.core.JsonLdConsts.RDF_LANGSTRING;
import static com.github.jsonldjava.core.JsonLdConsts.XSD_STRING;
import static com.github.jsonldjava.core.Regex.HEX;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RDFDatasetUtils {

    public static String toNQuads(RDFDataset dataset) {
        final StringBuilder output = new StringBuilder(256);
        toNQuads(dataset, output);
        return output.toString();
    }

    public static void toNQuads(RDFDataset dataset, StringBuilder output) {
        final List<String> quads = new ArrayList<String>();
        for (String graphName : dataset.graphNames()) {
            final List<RDFDataset.Quad> triples = dataset.getQuads(graphName);
            if ("@default".equals(graphName)) {
                graphName = null;
            }
            for (final RDFDataset.Quad triple : triples) {
                quads.add(toNQuad(triple, graphName));
            }
        }
        Collections.sort(quads);
        for (final String quad : quads) {
            output.append(quad);
        }
    }

    static String toNQuad(RDFDataset.Quad triple, String graphName, String bnode) {
        final StringBuilder output = new StringBuilder(256);
        toNQuad(triple, graphName, bnode, output);
        return output.toString();
    }

    static void toNQuad(RDFDataset.Quad triple, String graphName, String bnode,
            StringBuilder output) {
        final RDFDataset.Node s = triple.getSubject();
        final RDFDataset.Node p = triple.getPredicate();
        final RDFDataset.Node o = triple.getObject();

        // subject is an IRI or bnode
        if (s.isIRI()) {
            output.append("<");
            escape(s.getValue(), output);
            output.append(">");
        }
        // normalization mode
        else if (bnode != null) {
            output.append(bnode.equals(s.getValue()) ? "_:a" : "_:z");
        }
        // normal mode
        else {
            output.append(s.getValue());
        }

        if (p.isIRI()) {
            output.append(" <");
            escape(p.getValue(), output);
            output.append("> ");
        }
        // otherwise it must be a bnode (TODO: can we only allow this if the
        // flag is set in options?)
        else {
            output.append(" ");
            escape(p.getValue(), output);
            output.append(" ");
        }

        // object is IRI, bnode or literal
        if (o.isIRI()) {
            output.append("<");
            escape(o.getValue(), output);
            output.append(">");
        } else if (o.isBlankNode()) {
            // normalization mode
            if (bnode != null) {
                output.append(bnode.equals(o.getValue()) ? "_:a" : "_:z");
            }
            // normal mode
            else {
                output.append(o.getValue());
            }
        } else {
            output.append("\"");
            escape(o.getValue(), output);
            output.append("\"");
            if (RDF_LANGSTRING.equals(o.getDatatype())) {
                output.append("@").append(o.getLanguage());
            } else if (!XSD_STRING.equals(o.getDatatype())) {
                output.append("^^<");
                escape(o.getDatatype(), output);
                output.append(">");
            }
        }

        // graph
        if (graphName != null) {
            if (graphName.indexOf("_:") != 0) {
                output.append(" <");
                escape(graphName, output);
                output.append(">");
            } else if (bnode != null) {
                output.append(" _:g");
            } else {
                output.append(" ").append(graphName);
            }
        }

        output.append(" .\n");
    }

    static String toNQuad(RDFDataset.Quad triple, String graphName) {
        return toNQuad(triple, graphName, null);
    }

    final private static Pattern UCHAR_MATCHED = Pattern
            .compile("\\u005C(?:([tbnrf\\\"'])|(?:u(" + HEX + "{4}))|(?:U(" + HEX + "{8})))");

    public static String unescape(String str) {
        String rval = str;
        if (str != null) {
            final Matcher m = UCHAR_MATCHED.matcher(str);
            while (m.find()) {
                String uni = m.group(0);
                if (m.group(1) == null) {
                    final String hex = m.group(2) != null ? m.group(2) : m.group(3);
                    final int v = Integer.parseInt(hex, 16);// hex =
                    // hex.replaceAll("^(?:00)+",
                    // "");
                    if (v > 0xFFFF) {
                        // deal with UTF-32
                        // Integer v = Integer.parseInt(hex, 16);
                        final int vt = v - 0x10000;
                        final int vh = vt >> 10;
                        final int v1 = vt & 0x3FF;
                        final int w1 = 0xD800 + vh;
                        final int w2 = 0xDC00 + v1;

                        final StringBuilder b = new StringBuilder();
                        b.appendCodePoint(w1);
                        b.appendCodePoint(w2);
                        uni = b.toString();
                    } else {
                        uni = Character.toString((char) v);
                    }
                } else {
                    final char c = m.group(1).charAt(0);
                    switch (c) {
                    case 'b':
                        uni = "\b";
                        break;
                    case 'n':
                        uni = "\n";
                        break;
                    case 't':
                        uni = "\t";
                        break;
                    case 'f':
                        uni = "\f";
                        break;
                    case 'r':
                        uni = "\r";
                        break;
                    case '\'':
                        uni = "'";
                        break;
                    case '\"':
                        uni = "\"";
                        break;
                    case '\\':
                        uni = "\\";
                        break;
                    default:
                        // do nothing
                        continue;
                    }
                }
                final String pat = Pattern.quote(m.group(0));
                // final String x = Integer.toHexString(uni.charAt(0));
                rval = rval.replaceAll(pat, uni);
            }
        }
        return rval;
    }

    /**
     * Escapes the given string according to the N-Quads escape rules
     *
     * @param str
     *            The string to escape
     * @param rval
     *            The {@link StringBuilder} to append to.
     */
    public static void escape(String str, StringBuilder rval) {
        for (int i = 0; i < str.length(); i++) {
            final char hi = str.charAt(i);
            if (hi <= 0x8 || hi == 0xB || hi == 0xC || (hi >= 0xE && hi <= 0x1F)
                    || (hi >= 0x7F && hi <= 0xA0) || // 0xA0 is end of
                    // non-printable latin-1
                    // supplement
                    // characters
                    ((hi >= 0x24F // 0x24F is the end of latin extensions
                            && !Character.isHighSurrogate(hi))
                    // TODO: there's probably a lot of other characters that
                    // shouldn't be escaped that
                    // fall outside these ranges, this is one example from the
                    // json-ld tests
                    )) {
                rval.append(String.format("\\u%04x", (int) hi));
            } else if (Character.isHighSurrogate(hi)) {
                final char lo = str.charAt(++i);
                final int c = (hi << 10) + lo + (0x10000 - (0xD800 << 10) - 0xDC00);
                rval.append(String.format("\\U%08x", c));
            } else {
                switch (hi) {
                case '\b':
                    rval.append("\\b");
                    break;
                case '\n':
                    rval.append("\\n");
                    break;
                case '\t':
                    rval.append("\\t");
                    break;
                case '\f':
                    rval.append("\\f");
                    break;
                case '\r':
                    rval.append("\\r");
                    break;
                // case '\'':
                // rval += "\\'";
                // break;
                case '\"':
                    rval.append("\\\"");
                    // rval += "\\u0022";
                    break;
                case '\\':
                    rval.append("\\\\");
                    break;
                default:
                    // just put the char as is
                    rval.append(hi);
                    break;
                }
            }
        }
        // return rval;
    }

    private static class Regex {
        // define partial regexes
        // final public static Pattern IRI =
        // Pattern.compile("(?:<([^:]+:[^>]*)>)");
        final public static Pattern IRI = Pattern.compile("(?:<([^>]*)>)");
        final public static Pattern BNODE = Pattern.compile("(_:(?:[A-Za-z][A-Za-z0-9]*))");
        final public static Pattern PLAIN = Pattern.compile("\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"");
        final public static Pattern DATATYPE = Pattern.compile("(?:\\^\\^" + IRI + ")");
        final public static Pattern LANGUAGE = Pattern.compile("(?:@([a-z]+(?:-[a-zA-Z0-9]+)*))");
        final public static Pattern LITERAL = Pattern
                .compile("(?:" + PLAIN + "(?:" + DATATYPE + "|" + LANGUAGE + ")?)");
        final public static Pattern WS = Pattern.compile("[ \\t]+");
        final public static Pattern WSO = Pattern.compile("[ \\t]*");
        final public static Pattern EOLN = Pattern.compile("(?:\r\n)|(?:\n)|(?:\r)");
        final public static Pattern EMPTY = Pattern.compile("^" + WSO + "$");

        // define quad part regexes
        final public static Pattern SUBJECT = Pattern.compile("(?:" + IRI + "|" + BNODE + ")" + WS);
        final public static Pattern PROPERTY = Pattern.compile(IRI.pattern() + WS.pattern());
        final public static Pattern OBJECT = Pattern
                .compile("(?:" + IRI + "|" + BNODE + "|" + LITERAL + ")" + WSO);
        final public static Pattern GRAPH = Pattern
                .compile("(?:\\.|(?:(?:" + IRI + "|" + BNODE + ")" + WSO + "\\.))");

        // full quad regex
        final public static Pattern QUAD = Pattern
                .compile("^" + WSO + SUBJECT + PROPERTY + OBJECT + GRAPH + WSO + "$");
    }

    /**
     * Parses RDF in the form of N-Quads.
     *
     * @param input
     *            the N-Quads input to parse.
     *
     * @return an RDF dataset.
     * @throws JsonLdError
     *             If there was an error parsing the N-Quads document.
     */
    public static RDFDataset parseNQuads(String input) throws JsonLdError {
        // build RDF dataset
        final RDFDataset dataset = new RDFDataset();

        // split N-Quad input into lines
        final String[] lines = Regex.EOLN.split(input);
        int lineNumber = 0;
        for (final String line : lines) {
            lineNumber++;

            // skip empty lines
            if (Regex.EMPTY.matcher(line).matches()) {
                continue;
            }

            // parse quad
            final Matcher match = Regex.QUAD.matcher(line);
            if (!match.matches()) {
                throw new JsonLdError(JsonLdError.Error.SYNTAX_ERROR,
                        "Error while parsing N-Quads; invalid quad. line:" + lineNumber);
            }

            // get subject
            RDFDataset.Node subject;
            if (match.group(1) != null) {
                subject = new RDFDataset.IRI(unescape(match.group(1)));
            } else {
                subject = new RDFDataset.BlankNode(unescape(match.group(2)));
            }

            // get predicate
            final RDFDataset.Node predicate = new RDFDataset.IRI(unescape(match.group(3)));

            // get object
            RDFDataset.Node object;
            if (match.group(4) != null) {
                object = new RDFDataset.IRI(unescape(match.group(4)));
            } else if (match.group(5) != null) {
                object = new RDFDataset.BlankNode(unescape(match.group(5)));
            } else {
                final String language = unescape(match.group(8));
                final String datatype = match.group(7) != null ? unescape(match.group(7))
                        : match.group(8) != null ? RDF_LANGSTRING : XSD_STRING;
                final String unescaped = unescape(match.group(6));
                object = new RDFDataset.Literal(unescaped, datatype, language);
            }

            // get graph name ('@default' is used for the default graph)
            String name = "@default";
            if (match.group(9) != null) {
                name = unescape(match.group(9));
            } else if (match.group(10) != null) {
                name = unescape(match.group(10));
            }

            final RDFDataset.Quad triple = new RDFDataset.Quad(subject, predicate, object, name);

            // initialise graph in dataset
            if (!dataset.containsKey(name)) {
                final List<RDFDataset.Quad> tmp = new ArrayList<RDFDataset.Quad>();
                tmp.add(triple);
                dataset.put(name, tmp);
            }
            // add triple if unique to its graph
            else {
                final List<RDFDataset.Quad> triples = (List<RDFDataset.Quad>) dataset.get(name);
                if (!triples.contains(triple)) {
                    triples.add(triple);
                }
            }
        }

        return dataset;
    }
}
