===================================
JSONLD-Java Jena integration module
===================================

This module integrates JSONLD-Java with Jena 2.11.0 or later.

There are several levels of integration, detailed under Usage below.

USAGE
=====

From Maven
----------

    <dependency>
        <groupId>com.github.jsonld-java</groupId>
        <artifactId>jsonld-java-jena</artifactId>
        <version>0.3.0</version>
    </dependency>

(Adjust for most recent <version>, as found in ``pom.xml``).


Initialization
--------------
JenaJSONLD must be initialized so that the readers and writers are registered with Jena. You would typically
do this from within a static {} block, although there is no danger in calling this several times:

    import com.github.jsonldjava.jena.*;    
    static {
        JenaJSONLD.init();       
    }


Parse JSON-LD (newer RIOT reader)
---------------------------------
        JenaJSONLD.init(); // Only needed once       
        String url = "http://json-ld.org/test-suite/tests/expand-0002-in.jsonld";
        // Detects language based on extension (ideally content type)
        Model model = RDFDataMgr.loadModel(url);
        
        // or explicit with base URI,  Lang and any supported source
        InputStream inStream = new ByteArrayInputStream("{}".getBytes("UTF-8"));        
        RDFDataMgr.read(model, inStream, "http://example.com/", JenaJSONLD.JSONLD);
        
        
        RDFDataMgr.write(System.out, model, Lang.TURTLE);
        // <http://example.com/id1>
        //    a                           <http://example.com/t1> ;
        //    <http://example.com/term1>  "v1"^^<http://www.w3.org/2001/XMLSchema#string> ;
        //    <http://example.com/term2>  "v2"^^<http://example.com/t2> ;
        //    <http://example.com/term3>  "v3"@en ;
        //    <http://example.com/term4>  4 ;
        //    <http://example.com/term5>  51 , 50 .        
    }
       

Write JSON-LD (newer RIOT writer)
---------------------------------

        JenaJSONLD.init(); // Only needed once       

        Model model = ModelFactory.createDefaultModel();
        Resource resource = model.createResource("http://example.com/test");
        Property property = model.createProperty("http://example.com/value");
        model.add(resource, property, "Test");
        
        RDFDataMgr.write(System.out, model, JenaJSONLD.JSONLD);        
        // {
        //    "@context" : {
        //      "value" : {
        //        "@id" : "http://example.com/value",
        //        "@type" : "@id"
        //      }
        //    },
        //    "@id" : "http://example.com/test",
        //    "http://example.com/value" : "Test"
        // }

Or more compact:
        RDFDataMgr.write(System.out, model, JenaJSONLD.JSONLD_FORMAT_FLAT);
        // "@context":{"value":{"@id":"http://example.com/value","@type":"@id"}},"@id":"http://example.com/test","http://example.com/value":"Test"}


Datasets are also supported:

        Dataset dataset = DatasetFactory.createMem();
        dataset.addNamedModel("http://example.com/graph", model);
        RDFDataMgr.write(System.out, dataset, JenaJSONLD.JSONLD);
        // {
        //    "@graph" : [ {
        //      "@id" : "http://example.com/test",
        //      "http://example.com/value" : "Test"
        //    } ],
        //    "@id" : "http://example.com/graph"
        // }


Note that Jena's RDFDataMgr.write() does not currently support passing the 
base URI parameter, although this is supported by the underlying writer.



Parse JSON-LD (classic Jena RDFReader)
--------------------------------------
        JenaJSONLD.init(); // Only needed once

        String url = "http://json-ld.org/test-suite/tests/expand-0002-in.jsonld";
        Model model = ModelFactory.createDefaultModel();
        model.read(url, "JSON-LD");
        
        model.write(System.out, "TURTLE", "http://example.com/");
        // @base          <http://example.com/> .
        //    <id1>   a                           <t1> ;
        //            <term1>                     "v1"^^<http://www.w3.org/2001/XMLSchema#string> ;
        //            <term2>                     "v2"^^<t2> ;
        //            <term3>                     "v3"@en ;
        //            <term4>                     4 ;
        //            <term5>                     51 , 50 .       

Notes: 
* Jena's classic reader factory looks up implementation using Class.forName()
  and com.github.jsonldjava.jena.JenaJSONLD must therefore be in the same 
  classloader (e.g. on the classpath) as Jena - this does not work in OSGi.
* The optional baseURI parameter to read() is supported. If the base is unknown, 
  "" generally works fine, although Jena might not be able to serialise graphs
  with relative URI references (e.g. <term1>) as RDF/XML



Write JSON-LD (classic Jena)
----------------------------
        JenaJSONLD.init(); // Only needed once       

        Model model = ModelFactory.createDefaultModel();
        Resource resource = model.createResource("http://example.com/test");
        Property property = model.createProperty("http://example.com/value");
        model.add(resource, property, "Test");        
        model.write(System.out, "JSON-LD");
        // {
        //    "@context" : {
        //      "value" : {
        //        "@id" : "http://example.com/value",
        //        "@type" : "@id"
        //      }
        //    },
        //    "@id" : "http://example.com/test",
        //    "http://example.com/value" : "Test"
        //  }
 
Or made relative from a base URI(notice the relative @id below):

        model.write(System.out, "JSON-LD", "http://example.com/");
        // {
        //    "@context" : {
        //      "value" : {
        //        "@id" : "http://example.com/value",
        //        "@type" : "@id"
        //      }
        //    },
        //    "@id" : "test",
        //    "http://example.com/value" : "Test"
        // }        

Notes:      
* The optional base URI parameter can be used to set a base that URIs are to be made
  relative from (without including @base in the JSONLD)
* Same classpath considerations as for "Parse JSON-LD (classic Jena RDFReader)" above        


Jena model to JSON-LD objects
-----------------------------

        JenaJSONLD.init(); // Only needed once
        Model model = ModelFactory.createDefaultModel();
        Resource resource = model.createResource("http://example.com/test");
        Property property = model.createProperty("http://example.com/value");
        model.add(resource, property, "Test");

        Options options = new Options();
        options.format = "application/ld+json";
        Object json = JSONLD.fromRDF(model, options);
        String jsonStr = JSONUtils.toPrettyString(json);
        System.out.println(jsonStr);
        // [ {
        //    "@id" : "http://example.com/test",
        //    "http://example.com/value" : [ {
        //      "@value" : "Test"
        //    } ]
        //  } ]


JenaRDFParser
-------------

This internal class is used by JSONLD-Java to "parse" an existing Jena model and generate JSON-LD. 

The JenaRDFParser expects input as an instance of `com.hp.hpl.jena.rdf.model.Model` containing the entire graph.

See [JenaRDFParserTest.java](./src/test/java/com/github/jsonldjava/jena/JenaRDFParserTest.java) for example Usage.


JenaTripleCallback
------------------

This internal class is used by JSONLD-Java to create an existing Jena model from an existing JSON-LD. 

The JenaTripleCallback returns an instance of `com.hp.hpl.jena.rdf.model.Model`

See [JenaTripleCallbackTest.java](./src/test/java/com/github/jsonldjava/jena/JenaTripleCallbackTest.java) for example Usage.
