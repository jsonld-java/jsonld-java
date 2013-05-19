JSONLD-Java Jena Integration module
===================================

USAGE
=====

From Maven
----------

    <dependency>
        <groupId>com.github.jsonld-java</groupId>
        <artifactId>jsonld-java-jena</artifactId>
        <version>0.1</version>
    </dependency>

JenaRDFParser
-------------

The JenaRDFParser expects input as an instance of `com.hp.hpl.jena.rdf.model.Model` containing the entire graph.

See [JenaRDFParserTest.java](./src/test/java/com/github/jsonldjava/impl/JenaRDFParserTest.java) for example Usage.

JenaTripleCallback
------------------

The JenaTripleCallback returns an instance of `com.hp.hpl.jena.rdf.model.Model`

See [JenaTripleCallbackTest.java](./src/test/java/com/github/jsonldjava/impl/JenaTripleCallbackTest.java) for example Usage.
