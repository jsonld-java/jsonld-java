Note: this is the documentation for the current unstable development branch. [For the stable release documentation see here](https://github.com/jsonld-java/jsonld-java/blob/v0.3/integration/clerezza/README.md)

JSONLD-Java Clerezza Integration module
=======================================

USAGE
=====

From Maven
----------

    <dependency>
        <groupId>com.github.jsonld-java</groupId>
        <artifactId>jsonld-java-clerezza</artifactId>
        <version>0.4-SNAPSHOT</version>
    </dependency>

(Adjust for most recent <version>, as found in ``pom.xml``).


ClerezzaTripleCallback
------------------

The ClerezzaTripleCallback returns an instance of `org.apache.clerezza.rdf.core.MGraph`

See [ClerezzaTripleCallbackTest.java](./src/test/java/com/github/jsonldjava/clerezza/ClerezzaTripleCallbackTest.java) for example Usage.
