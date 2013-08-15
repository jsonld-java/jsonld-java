JSONLD-Java Jena Integration module
===================================

USAGE
=====

From Maven
----------

    <dependency>
        <groupId>com.github.jsonld-java</groupId>
        <artifactId>jsonld-java-jena</artifactId>
        <version>0.3</version>
    </dependency>


JenaJSONLDReader
----------------
        JenaJSONLDReader.registerWithJena();

        String url = "http://json-ld.org/test-suite/tests/expand-0002-in.jsonld";
        Model model = ModelFactory.createDefaultModel();
        model.read(url, "JSON-LD");
        model.write(System.out, "TURTLE", "");

        // <http://example.com/id1>
        //       a       <http://example.com/t1> ;
        //       <http://example.com/term1>
        //               "v1"^^<http://www.w3.org/2001/XMLSchema#string> ;

Notes: 
* registerWithJena() only needs to be called once, but there is no harm
  in calling it multiple times.
* Jena's reader factory looks up implementation using Class.forName()
  and JenaJSONLDReader must therefore be in the same classloader (e.g.
  on the classpath) as Jena - this does not work in OSGi.
* Remember to use the read() method that takes both a base and lang. 
* If the base is unknown, "" generally works fine, although Jena might
  not be able to serialise graphs with relative URI references as
  RDF/XML

JenaJSONLDWriter
----------------
        JenaJSONLDWriter.registerWithJena();

        Model model = ModelFactory.createDefaultModel();
        Resource resource = model.createResource("http://example.com/test");
        Property property = model.createProperty("http://example.com/value");
        model.add(resource, property, "Test");
        
        StringWriter writer = new StringWriter();
        model.write(writer, "JSON-LD");
        
        String json = writer.toString();
        System.out.println(json);

        // [ {
        //   "@id" : "http://example.com/test",
        //   "http://example.com/value" : [ {
        //     "@value" : "Test"
        //   } ]
        // } ]
        
* Same register and classpath considerations as for JenaJSONLDReader        


JenaRDFParser
-------------

The JenaRDFParser expects input as an instance of `com.hp.hpl.jena.rdf.model.Model` containing the entire graph.

See [JenaRDFParserTest.java](./src/test/java/com/github/jsonldjava/impl/JenaRDFParserTest.java) for example Usage.

JenaTripleCallback
------------------

The JenaTripleCallback returns an instance of `com.hp.hpl.jena.rdf.model.Model`

See [JenaTripleCallbackTest.java](./src/test/java/com/github/jsonldjava/impl/JenaTripleCallbackTest.java) for example Usage.
