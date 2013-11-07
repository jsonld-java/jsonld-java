JSONLD-JAVA INTEGRATION MODULES
===============================

This is the base package to provide JSON-LD integration with other Java RDF libraries.

CREATING AN INTEGRATION MODULE
==============================

Fork the jsonld-java project
----------------------------

If you're creating a module for a RDF library that isn't already supported by the jsonld-java integration modules you can create your module directly in the jsonld-java project. This will allow other people who may be interested in your module to find it easier and allow it to be released along with the core code and updated by the comunity.

See https://help.github.com/articles/fork-a-repo for details on forking a repository.

Create module in Eclipse
------------------------

### Install m2e

Make sure you have [m2e](http://eclipse.org/m2e/) installed.

### Import `jsonld-java` project into Eclipse

 * `File` -> `Import`
 * Select `Existing Maven Projects`
 * `Browse` to the directory you cloned `jsonld-java` to
 * `Select All`
 * `Finish`

### Create new Maven Module

 * Right click on the `jsonld-java-integration` project and select `New` -> `Project`
 * Select `Maven Module`
 * Enter a `Module Name` which matches the RDF Library you're integrating (e.g. `jena`) 
 * `Next` -> `Next` (you should now be at the `Specify Archetype parameters` page
 * Change `Package` to `com.github.jsonldjava.YOURMODULE`
 * `Finish`

### Clean up automatically generated pom.xml

Make the generated pom.xml match the one listed below.

### Remove generated code

Delete the App.java and AppTest.java files.

Create module manually
----------------------

### Create folder for your module

After cloning your fork of jsonld-java, create a new directory for your module under `/jsonld-java/integration/<your module>`.

### Create pom.xml for your module

Here is the basic outline for what your module's pom.xml should look like

	<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
	
		<parent>
			<artifactId>jsonld-java-integration</artifactId>
			<groupId>com.github.jsonld-java</groupId>
			<version>0.1-SNAPSHOT</version>
		</parent>
		<modelVersion>4.0.0</modelVersion>
		<artifactId>jsonld-java-{your module}</artifactId>
		<name>JSONLD Java :: {your module name}</name>
		<description>JSON-LD Java integration module for {RDF Library your module integrates}</description>
		<packaging>jar</packaging>

		<developers>
			<developer>
				<name>{YOU}</name>
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

	JSONLD.registerRDFParser("format/identifier", new YourRDFParser());
	Object jsonld = JSONLD.fromRDF(yourInput, new Options("") {{ format = "format/identifier" }});

or pass an instance of your `RDFParser` into the `fromRDF` function

	Object jsonld = JSONLD.fromRDF(yourInput, new YourRDFParser());

### JSONLDTripleCallback

A JSONLD triple callback is a class that can populate your framework's
RDF model from JSON-LD - being called for each triple (technically quad).

Pass an instance of your `TripleCallback` to `JSONLD.toRDF`

	Object yourOutput = JSONLD.toRDF(jsonld, new YourTripleCallback());


Integrate with your framework
-----------------------------
Your framework might have its own system of readers and writers, where
you should register JSON-LD as a supported format. Remember that here
the "parse" direction is opposite of above, a 'reader' in e.g. Jena will
be a class that can parse JSON-LD and populate a Jena model.



Write Tests
-----------

It's helpful to have a test or two for your implementations to make sure they work and continue to work with future versions.

Write README.md
---------------

Write a `README.md` file under `jsonld-java/integration/<your module>/` with instrutions on how to use your module.

Submit your module
------------------

Once you've `commit`ted your code, and `push`ed it into your github fork you can issue a [Pull Request](https://help.github.com/articles/using-pull-requests) so that we can pull your new module into the jsonld-java codebase.
