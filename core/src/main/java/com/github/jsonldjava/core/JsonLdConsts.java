package com.github.jsonldjava.core;

/**
 * URI Constants used in the JSON-LD parser.
 */
public final class JsonLdConsts {

    public static final String RDF_SYNTAX_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static final String RDF_SCHEMA_NS = "http://www.w3.org/2000/01/rdf-schema#";
    public static final String XSD_NS = "http://www.w3.org/2001/XMLSchema#";

    public static final String XSD_ANYTYPE = XSD_NS + "anyType";
    public static final String XSD_BOOLEAN = XSD_NS + "boolean";
    public static final String XSD_DOUBLE = XSD_NS + "double";
    public static final String XSD_INTEGER = XSD_NS + "integer";
    public static final String XSD_FLOAT = XSD_NS + "float";
    public static final String XSD_DECIMAL = XSD_NS + "decimal";
    public static final String XSD_ANYURI = XSD_NS + "anyURI";
    public static final String XSD_STRING = XSD_NS + "string";

    public static final String RDF_TYPE = RDF_SYNTAX_NS + "type";
    public static final String RDF_FIRST = RDF_SYNTAX_NS + "first";
    public static final String RDF_REST = RDF_SYNTAX_NS + "rest";
    public static final String RDF_NIL = RDF_SYNTAX_NS + "nil";
    public static final String RDF_PLAIN_LITERAL = RDF_SYNTAX_NS + "PlainLiteral";
    public static final String RDF_XML_LITERAL = RDF_SYNTAX_NS + "XMLLiteral";
    public static final String RDF_OBJECT = RDF_SYNTAX_NS + "object";
    public static final String RDF_LANGSTRING = RDF_SYNTAX_NS + "langString";
    public static final String RDF_LIST = RDF_SYNTAX_NS + "List";

    public static final String TEXT_TURTLE = "text/turtle";
    public static final String APPLICATION_NQUADS = "application/n-quads"; // https://www.w3.org/TR/n-quads/#sec-mediatype

    public static final String FLATTENED = "flattened";
    public static final String COMPACTED = "compacted";
    public static final String EXPANDED = "expanded";

    public static final String ID = "@id";
    public static final String DEFAULT = "@default";
    public static final String GRAPH = "@graph";
    public static final String CONTEXT = "@context";
    public static final String PRESERVE = "@preserve";
    public static final String EXPLICIT = "@explicit";
    public static final String OMIT_DEFAULT = "@omitDefault";
    public static final String EMBED_CHILDREN = "@embedChildren";
    public static final String EMBED = "@embed";
    public static final String LIST = "@list";
    public static final String LANGUAGE = "@language";
    public static final String INDEX = "@index";
    public static final String SET = "@set";
    public static final String TYPE = "@type";
    public static final String REVERSE = "@reverse";
    public static final String VALUE = "@value";
    public static final String NULL = "@null";
    public static final String NONE = "@none";
    public static final String CONTAINER = "@container";
    public static final String BLANK_NODE_PREFIX = "_:";
    public static final String VOCAB = "@vocab";
    public static final String BASE = "@base";
    public static final String REQUIRE_ALL = "@requireAll";

    public enum Embed {
        ALWAYS, NEVER, LAST, LINK;
    }
}