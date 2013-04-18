JSONLD-JAVA
===========

This is a Java implementation of the JSON-LD specification (http://json-ld.org/).

The code is based off the jsonld.js and PyLD implementations found on the json-ld homepage.

USAGE
=====

In Java
-------

### compiling

jsonld-java uses maven to compile

    mvn compile

### packaging

Either run `mvn jar:jar` and grab the generated jar file from `target/jsonld-java-1.0.0-SNAPSHOT.jar` or run `mvn install -Dmaven.test.skip=true` to install the jar into your local maven repository.

### in your project (maven specific)

    <dependency>
        <groupId>dfki.km.json</groupId>
        <artifactId>jsonld-java</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>

Note that the dependencies for the various rdf libraries are marked as optional, and will not be included unless specifically stated in your project's pom (this means that you will get a `ClassNotFoundException` for anything in the `de.dfki.km.jsonld.impl` package unless you specifically specify their dependencies in your pom).

E.g. if you wish to use the `JenaJSONLDSerializer` or the `JenaTripleCallback` add the following to your project's pom.

    <dependency>
        <groupId>com.hp.hpl.jena</groupId>
		<artifactId>jena</artifactId>
        <version>2.6.4</version>
    </dependency>

or if you want to use the Sesame libraries:

    <dependency>
        <groupId>org.openrdf.sesame</groupId>
		<artifactId>sesame-model</artifactId>
		<version>2.6.4</version>
    </dependency>

This also allows you to specify the version or this library you wish to use (however, if you don't use the same version it's not garanteed that the api will be the same which may cause some problems).

### running tests

    mvn test

Note that these currently fail due to the lack of an implementation of frame.

### code example

    // Open a valid json(-ld) input file
    InputStream inputStream = new FileInputStream("input.json");
    // Read the file into an Object (The type of this object will be a List, Map, String, Boolean,
    // Number or null depending on the root object in the file).
    Object jsonObject = JSONUtils.fromInputStream(inputStream);
    // Call whichever JSONLD function you want! (e.g. normalize)
    Object compact = JSONLD.compact(jsonObject);
    // Print out the result (or don't, it's your call!)
    System.out.println(JSONUtils.toString(normalized));

### Adding JSON-LD triples to Jena

    // Create a JenaTripleCallback object
    JenaTripleCallback callback = new JenaTripleCallback();
    // Optionally add your Jena Model to the callback (a default Model will be created if you don't
    // run this).
    callback.setJenaModel(jenaModel);
    // call the toRDF function
    JSONLD.toRDF(jsonObject, callback);
    // If you didn't use your own Jena Model, get the resulting one with:
    Model m = callback.getJenaModel();

### Adding JSON-LD triples to Sesame

    // Create a SesameTripleCallback object
    SesameTripleCallback callback = new SesameTripleCallback();
    // Optionally add your Sesame Graph to the callback (a default Graph will be created if you don't
    // run this).
    callback.setStorageGraph(storageGraph);
    // call the toRDF function
    JSONLD.toRDF(jsonObject, callback);
    // If you didn't use your own Sesame graph, get the resulting one with:
    Graph output = callback.getStorageGraph();

### Serializing a Jena Model to JSON-LD

    // Create an instance of the Jena serializer
    JenaJSONLDSerializer serializer = new JenaJSONLDSerializer();
    // call the fromRDF function (passing it the Jena Model)
    Object json = JSONLD.fromRDF(model, serializer);

### Serializing to JSON-LD from other sources

    // Create an instance of the serializer
    JSONLDSerializer serializer = new JSONLDSerializer() {
        // implement the parse function
        public void parse(Object input) throws JSONLDProcessingError {
            ...
            // for each triple you have where the object is a literal
            // (if datatypeURI is null, a plain literal will be assumed and language may be null or an empty string)
            triple(subjectURI, perdicateURI, value, datatypeURI, language);
            // for each triple you have where the object is an URI
            triple(subjectURI, predicateURI, objectURI);
            // if the triple belongs to a specific graph, include the graphURI at the end of the triple function call
            triple(subjectURI, perdicateURI, value, datatypeURI, language, graphURI);
            // or
            triple(subjectURI, predicateURI, objectURI, graphURI);
       }
       // if you need to do any post-processing of the resulting JSON-LD, override the finalize function
       public Object finalize(Object json) {
           ...
           return newjson;
       }
   }

See the [NQuad Serializer](https://github.com/tristan/jsonld-java/tree/master/src/main/java/de/dfki/km/json/jsonld/impl/NQuadJSONLDSerializer.java) for a specific example of a parse implementation

### Processor options

The Options specified by the [JSON-LD API Specification](http://json-ld.org/spec/latest/json-ld-api/#jsonldoptions) are accessible via the `JSONLDProcessor.Options` class, and each `JSONLD.*` function has an optional input to take an instance of this class.

Non-Specification Extras
------------------------

### Ignore Keys

The `JSONLDProcessor.Options' class has been extended with a `ignoreKey(String)` function. This function allows you to specify any keys that should not be treated as JSON-LD, and thus not processed. Any keys specified to be ignored will still be present in resulting objects, but will be exactly the same as their original. The only exception is toRDF, which simply skips over the keys as it doesn't make sense to generate triples for objects that aren't RDF.

### Simplify

The function `simplify(Object)` goes over all the keys in the input and attempts to make "SimpleName":"URI" mappings for each key and generates a new context for the input  based on these mappings. The resulting output should look like a very basic JSON document except for the @ keys.

RDF2JSONLD
----------

This is a simple function which takes an input file in rdfxml or n3 and outputs it in JSON-LD

### initial setup

	mvn -quiet clean package
    chmod +x ./target/appassembler/bin/rdf2jsonld

### usage

    ./target/appassembler/bin/rdf2jsonld <options> <input>
        input: a filename or URL to the rdf input (in rdfxml or n3)
        options:
                -expand : expand the jsonld output
                -normalize : normalize the jsonld output

NOTES
=====

compact-0018 still fails (it doesn't pass in the javascript version either)
fixing this requires fixing the term rank algorithm
This test is currently ignored in the test suite.

TODO
====

*   Make sure Jena Implementation is correct (i.e. write some real tests)
*   Tests for the serializations
*   Implement normalization.
*   As the code is almost a direct translation from the javascript and python implementation, there is probably a lot of optimization work to do.

OPEN ISSUES
===========

### RDF-Graph normalization

As the current [RDF Graph Normalization Specification](http://json-ld.org/spec/latest/rdf-graph-normalization/) is currently out of date and marked as "[Do Not Implement](http://json-ld.org/spec/latest/rdf-graph-normalization/#normalization)" and the javascript reference implementation is quite long, I've decided to skip it for the moment and wait for it to stabalize.

### fromRDF and JSONLDSerializer API

I've changed the Serialization API to more closely resemble the javascript reference impelementation and the fromRDF method listed in the [JSON-LD API Specification](http://json-ld.org/spec/latest/json-ld-api/#methods). I'm personally not happy with how it works from a Java perspective and would be interested to hear other peoples opinions and suggestions for improving it ([a ticket for discussion here](https://github.com/tristan/jsonld-java/issues/28)).

### ignored Keys

If an ignore key is included in a value object (i.e. an object containing a `@value` key), they will be ignored, but like any other key that is invalid in a value context it will be removed in the resulting object. I'm not yet sure if I should leave it like this or not (things get a bit complicated in compaction algorithms that can break down the value object into a plain string or integer).

CHANGELOG
=========

### 18.04.2013

* Updated to Sesame 2.7.0, Jena 2.10.0, Jackson 2.1.4
* Fixing a character encoding issue in the JSONLDProcessorTests
* Bumping to 1.0.1 to reflect dependency changes

### 30.10.2012

* Brought the implementation up to date with the reference implementation (minus the normalization stuff)
* Changed entry point for the functions to the static functions in the JSONLD class
* Changed the JSONLDSerializer to an abstract class, requiring the implementation of a "parse" function. The JSONLDSerializer is now passed to the JSONLD.fromRDF function.
* Added JSONLDProcessingError class to handle errors more efficiently