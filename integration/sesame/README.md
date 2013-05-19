JSONLD-Java Sesame Integration module
===================================

USAGE
=====

From Maven
----------

    <dependency>
        <groupId>com.github.jsonld-java</groupId>
        <artifactId>jsonld-java-sesame</artifactId>
        <version>0.1</version>
    </dependency>

Parsing JSON-LD using Sesame
----------------------------
    
To parse a JSON-LD document to a Model:

    InputStream inputStream = ...;
    String baseURI = "http://example.org/baseuri/";
    org.openrdf.model.Model statements = Rio.parse(inputStream, baseURI, RDFFormat.JSONLD);

To parse a JSON-LD document into a RepositoryConnection:

    org.openrdf.repository.Repository myRepository = ...;
    InputStream inputStream = ...;
    String baseURI = "http://example.org/baseuri/";
    org.openrdf.model.Resource contextToInsertTo = ...;
    
    org.openrdf.repository.RepositoryConnection repositoryConnection = myRepository.getConnection();
    try {
        repositoryConnection.add(inputStream, baseURI, RDFFormat.JSONLD, contextToInsertTo);
    } finally {
        repositoryConnection.close();
    }

Writing JSON-LD using Sesame
----------------------------

To write a Java Iterable<Statement> to a JSON-LD document:

    Iterable<Statement> statements = ...;
    OutputStream outputStream = ...;
    Rio.write(statements, outputStream, RDFFormat.JSONLD);

To export statements from a Repository to a JSON-LD document:

    org.openrdf.repository.Repository myRepository = ...;
    org.openrdf.model.Resource contextToExport = ...;
    OutputStream outputStream = ...;
    
    org.openrdf.repository.RepositoryConnection repositoryConnection = myRepository.getConnection();
    try {
        org.openrdf.rio.RDFWriter writer = Rio.createWriter(RDFFormat.JSONLD, outputStream);
        // Optionally define what JSON-LD profile is to be used
        // The Expand mode is used by default
        writer.getWriterConfig().set(JSONLDSettings.JSONLD_MODE, JSONLDMode.EXPAND);
        repositoryConnection.export(writer, contextToExport);
    } finally {
        repositoryConnection.close();
    }
