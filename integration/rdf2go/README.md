JSONLD-Java RDF2Go Integration module
=====================================

USAGE
=====

From Maven
----------

    <dependency>
        <groupId>com.github.jsonld-java</groupId>
        <artifactId>jsonld-java-rdf2go</artifactId>
        <version>0.3-SNAPSHOT</version>
    </dependency>

Serializing RDF into JSON-LD using RDF2GoRDFParser
--------------------------------------------------

    ModelSet modelSet = ...; // also works with a Model
    RDF2GoRDFParser parser = new RDF2GoRDFParser();
    Object json = JSONLD.fromRDF(modelSet, parser);

Parsing JSON-LD, and convert it into a ModelSet
-----------------------------------------------

    RDF2GoTripleCallback callback = new RDF2GoTripleCallback();
    ModelSet model = (ModelSet) JSONLD.toRDF(input, callback);
