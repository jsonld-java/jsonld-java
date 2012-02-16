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

    // Create a JenaTripleCallback object
    JenaTripleCallback callback = new JenaTripleCallback();
    // Optionally add your Jena Model to the callback (a default Model will be created if you don't
    // run this).
    callback.setJenaModel(jenaModel);
    // call the triples function of the processor
    processor.triples(jsonObject, callback);
    // If you didn't use your own Jena Model, get the resulting one with:
    Model m = callback.getJenaModel();

### Adding JSON-LD triples to Sesame

    // Create a SesameTripleCallback object
    SesameTripleCallback callback = new SesameTripleCallback();
    // Optionally add your Sesame Graph to the callback (a default Graph will be created if you don't
    // run this).
    callback.setStorageGraph(storageGraph);
    // call the triples function of the processor
    processor.triples(jsonObject, callback);
    // If you didn't use your own Sesame graph, get the resulting one with:
    Graph output = callback.getStorageGraph();

### Serializing a Jena Model to JSON-LD

    // Create an instance of the Jena serializer
    JenaJSONLDSerializer serializer = new JenaJSONLDSerializer();
    // import the Jena Model
    serializer.importModel(model);
    // grab the resulting JSON-LD map
    Map<String,Object> jsonld = serializer.asObject();

### Serializing to JSON-LD from other sources

    // Create an instance of the serializer
    JSONLDSerializer serializer = new JSONLDSerializer();
    // Optionally Add and extra prefix->uri mappinds you want (e.g. the following line)
    serializer.setPrefix("http://xmlns.com/foaf/0.1/", "foaf");
    // for each triple you have where the object is a literal
    // (if datatypeURI is null, a plain literal will be assumed and language may be null or an empty string)
    serializer.triple(subjectURI, perdicateURI, value, datatypeURI, language);
    // for each triple you have where the object is an URI
    serializer.triple(subjectURI, predicateURI, objectURI);
    // grab the resulting JSON-LD map
    Map<String,Object> jsonld = serializer.asObject();

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


TODO
====

*   Make sure Jena Implementation is correct (i.e. write some real tests)
*   Tests for the serializations
*   Implement frame.
*   As the code is almost a direct translation from the javascript and python implementation, there is probably a lot of optimization work to do.