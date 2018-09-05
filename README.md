**JSONLD-Java is looking for a maintainer**

JSONLD-JAVA
===========

This is a Java implementation of the [JSON-LD 1.0 specification](https://www.w3.org/TR/2014/REC-json-ld-20140116/) and the [JSON-LD-API 1.0 specification](https://www.w3.org/TR/2014/REC-json-ld-api-20140116/).

[![Build Status](https://travis-ci.org/jsonld-java/jsonld-java.svg?branch=master)](https://travis-ci.org/jsonld-java/jsonld-java) [![Coverage Status](https://coveralls.io/repos/jsonld-java/jsonld-java/badge.svg?branch=master)](https://coveralls.io/r/jsonld-java/jsonld-java?branch=master)

USAGE
=====

From Maven
----------

    <dependency>
        <groupId>com.github.jsonld-java</groupId>
        <artifactId>jsonld-java</artifactId>
        <version>0.12.1</version>
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

The Options specified by the [JSON-LD API Specification](https://json-ld.org/spec/latest/json-ld-api/#the-jsonldoptions-type) are accessible via the `com.github.jsonldjava.core.JsonLdOptions` class, and each `JsonLdProcessor.*` function has an optional input to take an instance of this class.

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

### Loading contexts from classpath

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

```
Date: Wed, 19 Mar 2014 13:25:08 GMT
Cache-Control: max-age=2147483647
```

The mechanism for loading `jarcache.json` relies on 
[Thread.currentThread().getContextClassLoader()](http://docs.oracle.com/javase/7/docs/api/java/lang/Thread.html#getContextClassLoader%28%29)
to locate resources from the classpath - if you are running on a command line,
within a framework (e.g. OSGi) or Servlet container (e.g. Tomcat) this should
normally be set correctly. If not, try:

```java
ClassLoader oldContextCL = Thread.currentThread().getContextClassLoader();
try { 
    Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
    JsonLdProcessor.expand(input);   // or any other JsonLd operation
} finally { 
    // Restore, in case the current thread was doing something else
    // with the context classloader before calling our method
    Thread.currentThread().setContextClassLoader(oldContextCL);
}
```

To disable all remote document fetching, when using the default DocumentLoader, set the 
following Java System Property to "true" using:

```java
System.setProperty("com.github.jsonldjava.disallowRemoteContextLoading", "true");
```

You can also use the constant provided in DocumentLoader for the same purpose:

```java
System.setProperty(DocumentLoader.DISALLOW_REMOTE_CONTEXT_LOADING, "true");
```

Note that if you override DocumentLoader you should also support this setting for consistency and security.

### Loading contexts from a string

Your application might be parsing JSONLD documents which reference external `@context` IRIs
that are not available as file URIs on the classpath. In this case, the `jarcache.json`
approch will not work. Instead you can inject the literal context file strings through
the `JsonLdOptions` object, as follows:

```java
// Inject a context document into the options as a literal string
DocumentLoader dl = new DocumentLoader();
JsonLdOptions options = new JsonLdOptions();
// ... the contents of "contexts/example.jsonld"
String jsonContext = "{ \"@contxt\": { ... } }";
dl.addInjectedDoc("http://www.example.com/context",  jsonContext);
options.setDocumentLoader(dl);

InputStream inputStream = new FileInputStream("input.json");
Object jsonObject = JsonUtils.fromInputStream(inputStream);
Map context = new HashMap();
Object compact = JsonLdProcessor.compact(jsonObject, context, options);
System.out.println(JsonUtils.toPrettyString(compact));
```

### Customizing the Apache HttpClient

To customize the HTTP behaviour (e.g. to disable the cache or provide
[authentication
credentials)](https://hc.apache.org/httpcomponents-client-ga/tutorial/html/authentication.html),
you may want to create and configure your own `CloseableHttpClient` instance, which can
be passed to a `DocumentLoader` instance using `setHttpClient()`. This document
loader can then be inserted into `JsonLdOptions` using `setDocumentLoader()`
and passed as an argument to `JsonLdProcessor` arguments.  

Example of inserting a credential provider (e.g. to load a `@context` protected
by HTTP Basic Auth):

```java
Object input = JsonUtils.fromInputStream(..);
DocumentLoader documentLoader = new DocumentLoader();
        
CredentialsProvider credsProvider = new BasicCredentialsProvider();
credsProvider.setCredentials(
        new AuthScope("localhost", 443),
        new UsernamePasswordCredentials("username", "password"));
       
CacheConfig cacheConfig = CacheConfig.custom().setMaxCacheEntries(1000)
        .setMaxObjectSize(1024 * 128).build();

CloseableHttpClient httpClient = CachingHttpClientBuilder
        .create()
        // allow caching
        .setCacheConfig(cacheConfig)
        // Wrap the local JarCacheStorage around a BasicHttpCacheStorage
        .setHttpCacheStorage(
                new JarCacheStorage(null, cacheConfig, new BasicHttpCacheStorage(
                        cacheConfig)))....
		
        // Add in the credentials provider
        .setDefaultCredentialsProvider(credsProvider);
        // When you are finished setting the properties, call build
        .build();

documentLoader.setHttpClient(httpClient);
        
JsonLdOptions options = new JsonLdOptions();
options.setDocumentLoader(documentLoader);
// .. and any other options        
Object rdf = JsonLdProcessor.toRDF(input, options);
```

PLAYGROUND
----------

The [jsonld-java-tools](https://github.com/jsonld-java/jsonld-java-tools) repository contains a simple application which provides command line access to JSON-LD functions

### Initial clone and setup

```bash
git clone git@github.com:jsonld-java/jsonld-java-tools.git
chmod +x ./jsonldplayground
```

### Usage

run the following to get usage details:

```bash
./jsonldplayground --help
```

For Developers
--------------

### Compiling & Packaging

`jsonld-java` uses maven to compile. From the base `jsonld-java` module run `mvn clean install` to install the jar into your local maven repository.

### Running tests

```bash
mvn test
```

or

```bash
mvn test -pl core
```

to run only core package tests

### Code style

The JSONLD-Java project uses custom Eclipse formatting and cleanup style guides to ensure that Pull Requests are fairly simple to merge.

These guides can be found in the /conf directory and can be installed in Eclipse using "Properties>Java Code Style>Formatter", followed by "Properties>Java Code Style>Clean Up" for each of the modules making up the JSONLD-Java project.

If you don't use Eclipse, then don't worry, your pull requests can be cleaned up by a repository maintainer prior to merging, but it makes the initial check easier if the modified code uses the conventions.

### Submitting Pull Requests

Once you have made a change to fix a bug or add a new feature, you should commit and push the change to your fork.

Then, you can open a pull request to merge your change into the master branch of the main repository.

Implementation Reports for JSONLD-Java conformance with JSONLD-1.0
==================================================================

The Implementation Reports documenting the conformance of JSONLD-Java with JSONLD-1.0 are available at:

https://github.com/jsonld-java/jsonld-java/tree/master/core/reports

### Regenerating Implementation Report

Implementation Reports conforming to the [JSON-LD Implementation Report](http://json-ld.org/test-suite/reports/#instructions-for-submitting-implementation-reports) document can be regenerated using the following command:

```bash
mvn test -pl core -Dtest=JsonLdProcessorTest -Dreport.format=<format>
```

Current possible values for `<format>` include JSON-LD (`application/ld+json` or `jsonld`), NQuads (`text/plain`, `nquads`, `ntriples`, `nq` or `nt`) and Turtle (`text/turtle`, `turtle` or `ttl`). `*` can be used to generate reports in all available formats.

Integration of JSONLD-Java with other Java packages
===================================================

This is the base package for JSONLD-Java. Integration with other Java packages are done in separate repositories.

Existing integrations
---------------------

* [Eclipse RDF4J](https://github.com/eclipse/rdf4j)
* [Apache Jena](https://github.com/apache/jena/)
* [RDF2GO](https://github.com/jsonld-java/jsonld-java-rdf2go)
* [Apache Clerezza](https://github.com/jsonld-java/jsonld-java-clerezza)

Creating an integration module
------------------------------

### Create a repository for your module

Create a GitHub repository for your module under your user account, or have a JSONLD-Java maintainer create one in the jsonld-java organisation.

Create maven module
-------------------

### Create pom.xml for your module

Here is the basic outline for what your module's pom.xml should look like

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">

  <parent>
    <groupId>com.github.jsonld-java</groupId>
    <artifactId>jsonld-java-parent</artifactId>
    <version>0.12.1-SNAPSHOT</version>
  </parent>
  <modelVersion>4.0.0</modelVersion>
  <artifactId>jsonld-java-{your module}</artifactId>
  <name>JSONLD Java :: {your module name}</name>
  <description>JSON-LD Java integration module for {RDF Library your module integrates}</description>
  <packaging>jar</packaging>

  <developers>
    <developer>
      <name>{YOU}</name>
      <email>{YOUR EMAIL ADDRESS}</email>
    </developer>
  </developers>

  <dependencies>
    <dependency>
      <groupId>${project.groupId}</groupId>
      <artifactId>jsonld-java</artifactId>
      <version>${project.version}</version>
      <type>jar</type> 
      <scope>compile</scope> 
    </dependency>
    <dependency>
      <groupId>${project.groupId}</groupId>
      <artifactId>jsonld-java</artifactId>
      <version>${project.version}</version>
      <type>test-jar</type>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-jdk14</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>
</project>
```

Make sure you edit the following:
 * `project/artifactId` : set this to `jsonld-java-{module id}`, where `{module id}` usually represents the RDF library you're integrating (e.g. `jsonld-java-jena`)
 * `project/name` : set this to `JSONLD Java :: {Module Name}`, wher `{module name}` is usually the name of the RDF library you're integrating.
 * `project/description`
 * `project/developers/developer/...` : Give youself credit by filling in the developer field. At least put your `<name>` in ([see here for all available options](http://maven.apache.org/pom.html#Developers)).
 * `project/dependencies/...` : remember to add any dependencies your project needs

### Import into your favorite editor

For Example: Follow the first few steps in the section above to import the whole `jsonld-java` project or only your new module into eclipse.

Create RDFParser Implementation
-------------------------------

The interface `com.github.jsonldjava.core.RDFParser` is used to parse RDF from the library into the JSONLD-Java internal RDF format. See the documentation in [`RDFParser.java`](../core/src/main/java/com/github/jsonldjava/core/RDFParser.java) for details on how to implement this interface.

Create TripleCallback Implementation
------------------------------------

The interface `com.github.jsonldjava.core.JSONLDTripleCallback` is used to generate a representation of the JSON-LD input in the RDF library. See the documentation in [`JSONLDTripleCallback.java`](../core/src/main/java/com/github/jsonldjava/core/JSONLDTripleCallback.java) for details on how to implement this interface.

Using your Implementations
--------------------------

### RDFParser

A JSONLD RDF parser is a class that can parse your frameworks' RDF model
and generate JSON-LD.

There are two ways to use your `RDFParser` implementation.

Register your parser with the `JSONLD` class and set `options.format` when you call `fromRDF`

```java
JSONLD.registerRDFParser("format/identifier", new YourRDFParser());
Object jsonld = JSONLD.fromRDF(yourInput, new Options("") {{ format = "format/identifier" }});
```

or pass an instance of your `RDFParser` into the `fromRDF` function

```java
Object jsonld = JSONLD.fromRDF(yourInput, new YourRDFParser());
```

### JSONLDTripleCallback

A JSONLD triple callback is a class that can populate your framework's
RDF model from JSON-LD - being called for each triple (technically quad).

Pass an instance of your `TripleCallback` to `JSONLD.toRDF`

```java
Object yourOutput = JSONLD.toRDF(jsonld, new YourTripleCallback());
```

Integrate with your framework
-----------------------------
Your framework might have its own system of readers and writers, where
you should register JSON-LD as a supported format. Remember that here
the "parse" direction is opposite of above, a 'reader' may be a class 
that can parse JSON-LD and populate an RDF Graph.

Write Tests
-----------

It's helpful to have a test or two for your implementations to make sure they work and continue to work with future versions.

Write README.md
---------------

Write a `README.md` file with instrutions on how to use your module.

Submit your module
------------------

Once you've `commit`ted your code, and `push`ed it into your github fork you can issue a [Pull Request](https://help.github.com/articles/using-pull-requests) so that we can add a reference to your module in this README file.

Alternatively, we can also host your repository in the jsonld-java organisation to give it more visibility.

CHANGELOG
=========

### 2018-09-05
* handle omit graph flag (Patch by @eroux)
* Release 0.12.1
* Make pruneBlankNodeIdentifiers false by default in 1.0 mode and always true in 1.1 mode (Patch by @eroux)
* Fix issue with blank node identifier pruning when @id is aliased (Patch by @eroux)
* Allow wildcard {} for @id in framing (Patch by @eroux)

### 2018-07-07
* Fix tests setup for schema.org with HttpURLConnection that break because of the inability of HttpURLConnection to redirect from HTTP to HTTPS

### 2018-04-08
* Release 0.12.0
* Encapsulate RemoteDocument and make it immutable

### 2018-04-03
* Fix performance issue caused by not caching schema.org and others that use ``Cache-Control: private`` (Patch by @HansBrende)
* Cache classpath scans for jarcache.json to fix a similar performance issue
* Add internal shaded dependency on Google Guava to use maintained soft and weak reference maps rather than adhoc versions
* Make JsonLdError a RuntimeException to improve its use in closures
* Bump minor version to 0.12 to reflect the API incompatibility caused by JsonLdError and protected field change and hiding in JarCacheStorage

### 2018-01-25
* Fix resource leak in JsonUtils.fromURL on unsuccessful requests (Patch by @plaplaige)

### 2017-11-15
* Ignore UTF BOM (Patch by @christopher-johnson)

### 2017-08-26
* Release 0.11.1
* Fix @embed:@always support (Patch by @dr0i)

### 2017-08-24
* Release 0.11.0

### 2017-08-22
* Add implicit "flag only" subframe to fix incomplete list recursion (Patch by @christopher-johnson)
* Support pruneBlankNodeIdentifiers framing option in 1.1 mode (Patch by @fsteeg and @eroux)
* Support new @embed values (Patch by @eroux)

### 2017-07-11
* Add injection of contexts directly into DocumentLoader (Patch by @ryankenney)
* Fix N-Quads content type (Patch by @NicolasRouquette)
* Add JsonUtils.fromJsonParser (Patch by @dschulten)

### 2017-02-16
* Make literals compare consistently (Patch by @stain)
* Release 0.10.0

### 2017-01-09
* Propagate causes for JsonLdError instances where they were caused by other Exceptions
* Remove schema.org hack as it appears to work again now...
* Remove deprecated and unused APIs
* Bump version to 0.10.0-SNAPSHOT per the removed/changed APIs

### 2016-12-23
* Release 0.9.0
* Fixes schema.org support that is broken with Apache HTTP Client but works with java.net.URL

### 2016-05-20
* Fix reported NPE in JsonLdApi.removeDependents

### 2016-05-18
* Release 0.8.3
* Fix @base in remote contexts corrupting the local context

### 2016-04-23
* Support @default inside of sets for framing

### 2016-02-29
* Fix ConcurrentModificationException in the implementation of the Framing API

### 2016-02-17
* Re-release version 0.8.2 with the refactoring work actually in it. 0.8.1 is identical in functionality to 0.8.0
* Release version 0.8.1
* Refactor JSONUtils and DocumentLoader to move most of the static logic into JSONUtils, and deprecate the DocumentLoader versions

### 2016-02-10
* Release version 0.8.0

### 2015-11-19
* Replace deprecated HTTPClient code with the new builder pattern
* Chain JarCacheStorage to any other HttpCacheStorage to simplify the way local caching is performed
* Bump version to 0.8.0-SNAPSHOT as some interface method parameters changed, particularly, DocumentLoader.setHttpClient changed to require CloseableHttpClient that was introduced in HttpClient-4.3

### 2015-11-16
* Bump dependencies to latest versions, particularly HTTPClient that is seeing more use on 4.5/4.4 than the 4.2 series that we have used so far
* Performance improvements for serialisation to N-Quads by replacing string append and replace with StringBuilder
* Support setting a system property, com.github.jsonldjava.disallowRemoteContextLoading, to "true" to disable remote context loading.

### 2015-09-30
* Release 0.7.0

### 2015-09-27
* Move Tools, Clerezza and RDF2GO modules out to separate repositories. The Tools repository had a circular build dependency with Sesame, while the other modules are best located and managed in separate repositories

### 2015-08-25
* Remove Sesame-2.7 module in favour of sesame-rio-jsonld for Sesame-2.8 and 4.0
* Fix bug where parsing did not fail if content was present after the end of a full JSON top level element

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

