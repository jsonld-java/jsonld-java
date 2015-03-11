Note: this is the documentation for the current unstable development branch. [For the stable release documentation see here](https://github.com/jsonld-java/jsonld-java/blob/v0.5.1/README.md)

JSONLD-JAVA
===========

This is a Java implementation of the [JSON-LD specification](http://www.w3.org/TR/json-ld/) and the [JSON-LD-API specification](http://www.w3.org/TR/json-ld-api/).

[![Build Status](https://travis-ci.org/jsonld-java/jsonld-java.svg?branch=master)](https://travis-ci.org/jsonld-java/jsonld-java) [![Coverage Status](https://coveralls.io/repos/jsonld-java/jsonld-java/badge.svg?branch=master)](https://coveralls.io/r/jsonld-java/jsonld-java?branch=master)

USAGE
=====

From Maven
----------

    <dependency>
        <groupId>com.github.jsonld-java</groupId>
        <artifactId>jsonld-java</artifactId>
        <version>0.6.0-SNAPSHOT</version>
    </dependency>

Code example
------------
```java
// Open a valid json(-ld) input file
InputStream inputStream = new FileInputStream("input.json");
// Read the file into an Object (The type of this object will be a List, Map, String, Boolean,
// Number or null depending on the root object in the file).
Object jsonObject = JsonUtils.fromInputStream(inputStream);
// Create a context JSON map containing prefixes and definitions
Map context = new HashMap();
// Customise context...
// Create an instance of JsonLdOptions with the standard JSON-LD options
JsonLdOptions options = new JsonLdOptions();
// Customise options...
// Call whichever JSONLD function you want! (e.g. compact)
Object compact = JsonLdProcessor.compact(jsonObject, context, options);
// Print out the result (or don't, it's your call!)
System.out.println(JsonUtils.toPrettyString(compact));
```
Processor options
-----------------

The Options specified by the [JSON-LD API Specification](http://json-ld.org/spec/latest/json-ld-api/#jsonldoptions) are accessible via the `com.github.jsonldjava.core.JsonLdOptions` class, and each `JsonLdProcessor.*` function has an optional input to take an instance of this class.


Controlling network traffic
---------------------------

Parsing JSON-LD will normally follow any external `@context` declarations.
Loading these contexts from the network may in some cases not be desirable, or
might require additional proxy configuration or authentication.

JSONLD-Java uses the [Apache HTTPComponents Client](https://hc.apache.org/httpcomponents-client-ga/index.html) for these network connections,
based on the [SystemDefaultHttpClient](http://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/org/apache/http/impl/client/SystemDefaultHttpClient.html) which reads
standard Java properties like `http.proxyHost`. 

The default HTTP Client is wrapped with a
[CachingHttpClient](https://hc.apache.org/httpcomponents-client-ga/httpclient-cache/apidocs/org/apache/http/impl/client/cache/CachingHttpClient.html) to provide a 
small memory-based cache (1000 objects, max 128 kB each) of regularly accessed contexts.


### Loading contexts from classpath/JAR

Your application might be parsing JSONLD documents which always use the same
external `@context` IRIs. Although the default HTTP cache (see above) will
avoid repeated downloading of the same contexts, your application would still
initially be vulnerable to network connectivity.

To bypass this issue, and even facilitate parsing of such documents in an
offline state, it is possible to provide a 'warmed' cache populated
from the classpath, e.g. loaded from a JAR.

In your application, simply add a resource `jarcache.json` to the root of your
classpath together with the JSON-LD contexts to embed. (Note that you might
have to recursively embed any nested contexts).

The syntax of `jarcache.json` is best explained by example:
```javascript
[
  {
    "Content-Location": "http://www.example.com/context",
    "X-Classpath": "contexts/example.jsonld",
    "Content-Type": "application/ld+json"
  },
  {
    "Content-Location": "http://data.example.net/other",
    "X-Classpath": "contexts/other.jsonld",
    "Content-Type": "application/ld+json"
  }
]
```
(See also [core/src/test/resources/jarcache.json](core/src/test/resources/jarcache.json)).

This will mean that any JSON-LD document trying to import the `@context` 
`http://www.example.com/context` will instead be given
`contexts/example.jsonld` loaded as a classpath resource. 

The `X-Classpath` location is an IRI reference resolved relative to the
location of the `jarcache.json` - so if you have multiple JARs with a
`jarcache.json` each, then the `X-Classpath` will be resolved within the
corresponding JAR (minimizing any conflicts).

Additional HTTP headers (such as `Content-Type` above) can be included,
although these are generally ignored by JSONLD-Java. 

Unless overridden in `jarcache.json`, this `Cache-Control` header is
automatically injected together with the current `Date`, meaning that the
resource loaded from the JAR will effectively never expire (the real HTTP
server will never be consulted by the Apache HTTP client):

    Date: Wed, 19 Mar 2014 13:25:08 GMT
    Cache-Control: max-age=2147483647

The mechanism for loading `jarcache.json` relies on 
[Thread.currentThread().getContextClassLoader()](http://docs.oracle.com/javase/7/docs/api/java/lang/Thread.html#getContextClassLoader%28%29)
to locate resources from the classpath - if you are running on a command line,
within a framework (e.g. OSGi) or Servlet container (e.g. Tomcat) this should
normally be set correctly. If not, try:

        ClassLoader oldContextCL = Thread.currentThread().getContextClassLoader();
        try { 
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            JsonLdProcessor.expand(input);   // or any other JsonLd operation
        } finally { 
	    // Restore, in case the current thread was doing something else
	    // with the context classloader before calling our method
            Thread.currentThread().setContextClassLoader(oldContextCL);
        }



### Customizing the Apache HttpClient

To customize the HTTP behaviour (e.g. to disable the cache or provide
[authentication
credentials)](https://hc.apache.org/httpcomponents-client-ga/tutorial/html/authentication.html),
you may want to create and configure your own `HttpClient` instance, which can
be passed to a `DocumentLoader` instance using `setHttpClient()`. This document
loader can then be inserted into `JsonLdOptions` using `setDocumentLoader()`
and passed as an argument to `JsonLdProcessor` arguments.  

Example of inserting a credential provider (e.g. to load a `@context` protected
by HTTP Basic Auth):
 
        Object input = JsonUtils.fromInputStream(..);
        DocumentLoader documentLoader = new DocumentLoader();
        
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope("localhost", 443),
                new UsernamePasswordCredentials("username", "password"));
        
        DefaultHttpClient httpClient = new SystemDefaultHttpClient();
        httpClient.setCredentialsProvider(credsProvider);

        documentLoader.setHttpClient(httpClient);
        
        JsonLdOptions options = new JsonLdOptions();
        options.setDocumentLoader(documentLoader);
        // .. and any other options        
        Object rdf = JsonLdProcessor.toRDF(input, options);

Note that if you override the DocumentLoader HTTP Client, this would also
disable the JAR Cache (see above), unless reinitiated:

	JarCacheStorage jarCache = new JarCacheStorage();
	httpClient = new CachingHttpClient(httpClient, jarCache, jarCache.getCacheConfig());
        documentLoader.setHttpClient(httpClient);


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

### Code style

The JSONLD-Java project uses custom Eclipse formatting and cleanup style guides to ensure that Pull Requests are fairly simple to merge.

These guides can be found in the /conf directory and can be installed in Eclipse using "Properties>Java Code Style>Formatter", followed by "Properties>Java Code Style>Clean Up" for each of the modules making up the JSONLD-Java project.

If you don't use Eclipse, then don't worry, your pull requests can be cleaned up by a repository maintainer prior to merging, but it makes the initial check easier if the modified code uses the conventions.

### Submitting Pull Requests

Once you have made a change to fix a bug or add a new feature, you should commit and push the change to your fork.

Then, you can open a pull request to merge your change into the master branch of the main repository.

CHANGELOG
=========

### 2015-03-12
* Compact context arrays if they contain a single element during compaction
* Bump to Sesame-2.7.15

### 2015-03-01
* Use jopt-simple for the playground cli to simplify the coding and improve error messages
* Allow RDF parsing and writing using all of the available Sesame Rio parsers through the playground cli
* Make the httpclient dependency OSGi compliant

### 2014-12-31
* Fix locale sensitive serialisation of XSD double/decimal typed literals to always be Locale.US
* Bump to Sesame-2.7.14
* Bump to Clerezza-0.14

### 2014-11-14
* Fix identification of integer, boolean, and decimal in RDF-JSONLD with useNativeTypes
* Release 0.5.1

### 2014-10-29
* Add OSGi metadata to Jar files
* Bump to Sesame-2.7.13

### 2014-07-14
* Release version 0.5.0
* Fix Jackson parse exceptions being propagated through Sesame without wrapping as RDFParseExceptions

### 2014-07-02
* Fix use of Java-7 API so we are still Java-6 compatible
* Ensure that Sesame RDFHandler endRDF and startRDF are called in SesameTripleCallback

### 2014-06-30
* Release version 0.4.2
* Bump to Sesame-2.7.12
* Remove Jena integration module, as it is now maintained by Jena team in their repository

### 2014-04-22
* Release version 0.4
* Bump to Sesame-2.7.11
* Bump to Jackson-2.3.3
* Bump to Jena-2.11.1

### 2014-03-26
* Bump RDF2GO to version 5.0.0

### 2014-03-24
* Allow loading remote @context from bundled JAR cache
* Support JSON array in @context with toRDF 
* Avoid exception on @context with default @language and unmapped key

### 2014-02-24
* Javadoc some core classes, JsonLdProcessor, JsonLdApi, and JsonUtils
* Rename some core classes for consistency, particularly JSONUtils to JsonUtils and JsonLdTripleCallback
* Fix for a Context constructor that wasn't taking base into account

### 2014-02-20
* Fix JsonLdApi mapping options in framing algorithm (Thanks Scott Blomquist @sblom)

### 2014-02-06

* Release version 0.3
* Bump to Sesame-2.7.10
* Fix Jena module to use new API

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
