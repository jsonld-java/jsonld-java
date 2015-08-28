Note: this is the documentation for the current unstable development branch. [For the stable release documentation see here](https://github.com/jsonld-java/jsonld-java/blob/v0.3/integration/clerezza/README.md)

JSONLD-Java Clerezza Integration module
=======================================

This module provide a `ParsingProvider`and `SerializingProvider` for Apache Clerezza. Those Providers plug into the Clerezza `Parser` and `Serializer` service infrastructure. Meaning that adding this bundle will allow Clerezza to parse and serialize JSON-LD.

USAGE
=====

Maven Dependency
----------------

    <dependency>
        <groupId>com.github.jsonld-java</groupId>
        <artifactId>jsonld-java-clerezza</artifactId>
        <version>0.6.1-SNAPSHOT</version>
    </dependency>

(Adjust for most recent <version>, as found in ``pom.xml``).

From OSGI
---------

Assuming the above Bundle is active in the OSGI Environment one can simple inject the `Serializer` and/or `Parser` service.

    @Reference
    private Serializer serializer;

    @Reference
    private Parser parser;


Normal Java
-----------

Both the `Parser` and `Serializer` also support `java.util.ServiceLoader`. So when running outside an OSGI environment one can use the `getInstance()` to obtain an instance.

    Serializer serializer = Serializer.getInstance();

    Parser parser = Parser.getInstance();

Supported Formats
-----------------

The JSON-LD parser implementation supports `application/ld+json`. The serializer supports both `application/ld+json` and `application/json`.

The rational behind this is that the parser can not parse any JSON however the Serializer does generate valid JSON.
