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

### running tests

    mvn test

Note that these currently fail due to the lack of an implementation of frame.

### code example

    // Open a valid json(-ld) input file
    InputStream inputStream = new FileInputStream("input.json");
    // Read the file into an Object (The type of this object will be a List, Map, String, Boolean
    // or Number depending on the root object in the file).
    Object jsonObject = JSONUtils.fromInputStream(inputStream);
    // Create a JSONLDProcessor
    JSONLDProcessor processor = new JSONLDProcessor();
    // Call whichever JSONLD function you want! (e.g. normalize)
    Object normalized = processor.normalize(jsonObject);
    // Print out the result (or don't, it's your call!)
    System.out.println(JSONUtils.toString(normalized));

### Adding JSON-LD triples to Jena

    // Create a JenaTripleCallbackObject
    JenaTripleCallback callback = new JenaTripleCallback();
    // Optionally add your jena model to the callback (a default model will be created if you don't
    // run this).
    callback.setJenaModel(jenaModel);
    // call the triples function of the processor
    processor.triples(jsonObject, callback);
    // If you didn't use your own jena model, get the resulting one with:
    Model m = callback.getJenaModel();

RDF2JSONLD
----------

This is a simple function which takes an input file in rdfxml or n3 and outputs it in JSON-LD

### initial setup

    chmod +x rdf2jsonld
    mvn -quiet compile
    mvn -quiet dependency:build-classpath -Dmdep.outputFile=target/cp.txt

### usage

    ./rdf2jsonld <options> <input>
        input: a filename or URL to the rdf input (in rdfxml or n3)
        options:
                -expand : expand the jsonld output
                -normalize : normalize the jsonld output

NOTES
=====

*   JSONLDTripleCallback copies the simple (s,p,o) inputs used by jsonld.js and PyLD rather than the (s,p,type,o,datatype,language) inputs in the specification

TODO
====

*   Make sure Jena Implementation is correct (i.e. write some real tests)
*   Implement compact and frame.
*   As the code is almost a direct translation from the javascript and python implementation, there is probably a lot of optimization work to do.
*   Look into more standard ways of instantiating a default implementation of an interface (i.e. I'm not completely happy with the package structure currently, and i'm not so happy with having to instantiate d.d.k.j.jsonld.impl.JSONLDProcessor, it just looks messy).