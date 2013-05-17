JSONLD-JAVA
===========

This is a Java implementation of the JSON-LD specification (http://json-ld.org/).

The code is mostly based off the jsonld.js implementation found on the json-ld homepage.

USAGE
=====

In Java
-------

### compiling

jsonld-java uses maven to compile

    mvn compile

### packaging

run `mvn install -DskipTests=true` to install the jar into your local maven repository.

### in your project (maven specific)

    <dependency>
        <groupId>com.github.jsonld-java</groupId>
        <artifactId>jsonld-java</artifactId>
        <version>0.1-SNAPSHOT</version>
    </dependency>

### RDF implementation specific code

All code specific to various RDF implementations (e.g. jena, sesame, etc) are stored in the [integration modules](./tree/master/integration). Readmes for how to use these modules should be present in their respective folders.

### running tests

    mvn test

or

    mvn test -pl core

to run only core package tests

### Generating Implementation Report

Implementation Reports conforming to the [JSON-LD Implementation Report](http://json-ld.org/test-suite/reports/#instructions-for-submitting-implementation-reports) document can be generated using the following command:

    mvn test -pl core -Dtest=JSONLDProcessorTest -Dreport.format=<format>

Current possible values for `<format>` include JSON-LD (`application/ld+json` or `jsonld`), NQuads (`text/plain`, `nquads`, `ntriples`, `nq` or `nt`) and Turtle (`text/turtle`, `turtle` or `ttl`). `*` can be used to generate reports in all available formats.

### code example

    // Open a valid json(-ld) input file
    InputStream inputStream = new FileInputStream("input.json");
    // Read the file into an Object (The type of this object will be a List, Map, String, Boolean,
    // Number or null depending on the root object in the file).
    Object jsonObject = JSONUtils.fromInputStream(inputStream);
    // Call whichever JSONLD function you want! (e.g. compact)
    Object compact = JSONLD.compact(jsonObject);
    // Print out the result (or don't, it's your call!)
    System.out.println(JSONUtils.toString(normalized));

### Processor options

The Options specified by the [JSON-LD API Specification](http://json-ld.org/spec/latest/json-ld-api/#jsonldoptions) are accessible via the `com.github.jsonldjava.core.Options` class, and each `JSONLD.*` function has an optional input to take an instance of this class.

PLAYGROUND
----------

This is a simple function which takes an input file in rdfxml or n3 and outputs it in JSON-LD

### initial setup

    mvn package -pl tools
    chmod +x ./tools/target/appassembler/bin/jsonldplayground

### usage

run the following to get usage details:

    ./tools/target/appassembler/bin/jsonldplayground

CHANGELOG
=========

### 16.05.2013

* Updated core code to match [JSON-LD 1.0 Processing Algorithms and API / W3C Editor's Draft 14 May 2013](http://json-ld.org/spec/latest/json-ld-api/)
* Deprecated JSONLDSerializer in favor of the RDFParser interface to better represent the purpose of the interface and better fit in with the updated core code.
* Updated the JSONLDTripleCallback to better fit with the updated code.
* Updated the Playground tool to support updated core code.

### 07.05.2013

* Changed base package names to com.github.jsonldjava
* Reverted version to 0.1-SNAPSHOT to allow version incrementing pre 1.0 while allowing a 1.0 release when the json-ld spec is finalised.
* Turned JSONLDTripleCallback into an interface.

### 18.04.2013

* Updated to Sesame 2.7.0, Jena 2.10.0, Jackson 2.1.4
* Fixing a character encoding issue in the JSONLDProcessorTests
* Bumping to 1.0.1 to reflect dependency changes

### 30.10.2012

* Brought the implementation up to date with the reference implementation (minus the normalization stuff)
* Changed entry point for the functions to the static functions in the JSONLD class
* Changed the JSONLDSerializer to an abstract class, requiring the implementation of a "parse" function. The JSONLDSerializer is now passed to the JSONLD.fromRDF function.
* Added JSONLDProcessingError class to handle errors more efficiently