Note: this is the documentation for the current unstable development branch. [For the stable release documentation see here](https://github.com/jsonld-java/jsonld-java/blob/b8dc62201bb192875ed92e0156e26c94bc38ba82/README.md).

JSONLD-JAVA
===========

This is a Java implementation of the JSON-LD specification (http://json-ld.org/).

USAGE
=====

From Maven
----------

    <dependency>
        <groupId>com.github.jsonld-java</groupId>
        <artifactId>jsonld-java</artifactId>
        <version>0.3</version>
    </dependency>

Code example
------------

    // Open a valid json(-ld) input file
    InputStream inputStream = new FileInputStream("input.json");
    // Read the file into an Object (The type of this object will be a List, Map, String, Boolean,
    // Number or null depending on the root object in the file).
    Object jsonObject = JSONUtils.fromInputStream(inputStream);
    // Create a context JSON map containing prefixes and definitions
    Map context = new HashMap();
    // Customise context...
    // Create an instance of JsonLdOptions with the standard JSON-LD options
    JsonLdOptions options = new JsonLdOptions();
    // Customise options...
    // Call whichever JSONLD function you want! (e.g. compact)
    Object compact = JsonLdProcessor.compact(jsonObject, context, options);
    // Print out the result (or don't, it's your call!)
    System.out.println(JSONUtils.toPrettyString(normalized));

Processor options
-----------------

The Options specified by the [JSON-LD API Specification](http://json-ld.org/spec/latest/json-ld-api/#jsonldoptions) are accessible via the `com.github.jsonldjava.core.JsonLdOptions` class, and each `JsonLdProcessor.*` function has an optional input to take an instance of this class.

RDF implementation specific code
--------------------------------

All code specific to various RDF implementations (e.g. jena, sesame, etc) are stored in the [integration modules](./integration). Readmes for how to use these modules should be present in their respective folders.

PLAYGROUND
----------

This is a simple application which provides command line access to JSON-LD functions

### Initial setup

    chmod +x ./jsonldplayground

### Usage

run the following to get usage details:

    ./jsonldplayground

For Developers
--------------

### Compiling & Packaging

`jsonld-java` uses maven to compile. From the base `jsonld-java` module run `mvn install -DskipTests=true` to install the jar into your local maven repository.


### Running tests

    mvn test

or

    mvn test -pl core

to run only core package tests

### Implementation Reports for JSONLD-Java conformance with JSONLD-1.0

The Implementation Reports documenting the conformance of JSONLD-Java with JSONLD-1.0 are available at:

https://github.com/jsonld-java/jsonld-java/tree/master/core/reports

### Regenerating Implementation Report

Implementation Reports conforming to the [JSON-LD Implementation Report](http://json-ld.org/test-suite/reports/#instructions-for-submitting-implementation-reports) document can be regenerated using the following command:

    mvn test -pl core -Dtest=JsonLdProcessorTest -Dreport.format=<format>

Current possible values for `<format>` include JSON-LD (`application/ld+json` or `jsonld`), NQuads (`text/plain`, `nquads`, `ntriples`, `nq` or `nt`) and Turtle (`text/turtle`, `turtle` or `ttl`). `*` can be used to generate reports in all available formats.

CHANGELOG
=========

### 2014-02-06

* Release version 0.3
* Bump to Sesame-2.7.10

### 2014-01-29

* Updated to final Recommendation
* Namespaces supported by Sesame integration module
* Initial implementation of remote document loading
* Bump to Jackson-2.3.1

### 2013-11-22

* updated jena writer

### 2013-11-07

* Integration packages renamed com.github.jsonldjava.sesame, 
  com.github.jsonldjava.jena etc. (Issue #76)  

### 2013-10-07

* Matched class names to Spec
 - Renamed `JSONLDException` to `JsonLdError`
 - Renamed `JSONLDProcessor` to `JsonLdApi`
 - Renamed `JSONLD` to `JsonLdProcessor`
 - Renamed `ActiveContext` to `Context`
 - Renamed `Options` to `JsonLdOptions`
* All context related utility functions moved to be members of the `Context` class

### 2013-09-30
* Fixed JSON-LD to Jena to handle of BNodes

### 2013-09-02

* Add RDF2Go integration
* Bump Sesame and Clerezza dependency versions

### 2013-06-18

* Bump to version 0.2
* Updated Turtle integration
* Added Caching of contexts loaded from URI
* Added source formatting eclipse config
* Fixed up seasame integration package names
* Replaced depreciated Jackson code

### 2013-05-19

* Added Turtle RDFParser and TripleCallback
* Changed Maven groupIds to `com.github.jsonld-java` to match github domain.
* Released version 0.1

### 2013-05-16

* Updated core code to match [JSON-LD 1.0 Processing Algorithms and API / W3C Editor's Draft 14 May 2013](http://json-ld.org/spec/latest/json-ld-api/)
* Deprecated JSONLDSerializer in favor of the RDFParser interface to better represent the purpose of the interface and better fit in with the updated core code.
* Updated the JSONLDTripleCallback to better fit with the updated code.
* Updated the Playground tool to support updated core code.

### 2013-05-07

* Changed base package names to com.github.jsonldjava
* Reverted version to 0.1-SNAPSHOT to allow version incrementing pre 1.0 while allowing a 1.0 release when the json-ld spec is finalised.
* Turned JSONLDTripleCallback into an interface.

### 2013-04-18

* Updated to Sesame 2.7.0, Jena 2.10.0, Jackson 2.1.4
* Fixing a character encoding issue in the JSONLDProcessorTests
* Bumping to 1.0.1 to reflect dependency changes

### 2012-10-30

* Brought the implementation up to date with the reference implementation (minus the normalization stuff)
* Changed entry point for the functions to the static functions in the JSONLD class
* Changed the JSONLDSerializer to an abstract class, requiring the implementation of a "parse" function. The JSONLDSerializer is now passed to the JSONLD.fromRDF function.
* Added JSONLDProcessingError class to handle errors more efficiently


Considerations for 1.0 release / optimisations
=========

* The `Context` class is a `Map` and many of the options are stored as values of the map. These could be made into variables, whice should speed things up a bit (the same with the termDefinitions variable inside the Context).
* some sort of document loader interface (with a mockup for testing) is required
