# Introduction

The JSON-LD Test Suite is a set of tests that can
be used to verify JSON-LD Processor conformance to the set of specifications
that constitute JSON-LD. The goal of the suite is to provide an easy and
comprehensive JSON-LD testing solution for developers creating JSON-LD Processors.

More information and an RDFS definition of the test vocabulary can be found at [vocab](https://w3c.github.io/json-ld-api/tests/vocab).

# Design

Tests driven from a top-level [manifest](manifest.jsonld) and are defined into [compact](compact-manifest.jsonld), [expand](expand-manifest.jsonld), [flatten](flatten-manifest.jsonld), [html](html-manifest.jsonld), [remote-doc](remote-doc-manifest.jsonld), [fromRdf](fromRdf-manifest.jsonld), and [toRdf](toRdf-manifest.jsonld) sections:

* [compact](compact-manifest.jsonld) tests have _input_, _expected_ and _context_ documents.
  The _expected_ results can be compared using [JSON-LD object comparison](#json-ld-object-comparison) with the processor output. Additionally, if the `ordered` option is not set, result should be expanded and compared with the expanded _expected_ document also using [JSON-LD object comparison](#json-ld-object-comparison).

  For *NegativeEvaluationTests*, the result is a string associated with the expected error code.
* [expand](expand-manifest.jsonld) tests have _input_ and _expected_ documents.
  The _expected_ results can be compared using [JSON-LD object comparison](#json-ld-object-comparison) with the processor output.

  Expansion tests may have a `expandContext` option, which is treated
  as an IRI relative to the manifest.

  For *NegativeEvaluationTests*, the result is a string associated with the expected error code.
* [html](html-manifest.jsonld) tests have _input_ and _expected_ documents and an optional _context_ document.
  The _expected_ results can be compared using [JSON-LD object comparison](#json-ld-object-comparison) with the processor output
  after potentially remapping blank node identifiers (see below).
  Additionally, if the result is compacted and the `ordered` option is not set, result should be expanded and compared with the expanded _expected_ document also using [JSON-LD object comparison](#json-ld-object-comparison).

  For *NegativeEvaluationTests*, the result is a string associated with the expected error code.
* [flatten](flatten-manifest.jsonld) tests have _input_ and _expected_ documents and an optional _context_ document.
  The _expected_ results can be compared using [JSON-LD object comparison](#json-ld-object-comparison) with the processor output
  after potentially remapping blank node identifiers (see below).
  Additionally, if the result is compacted and the `ordered` option is not set, result should be expanded and compared with the expanded _expected_ document also using [JSON-LD object comparison](#json-ld-object-comparison).

  For *NegativeEvaluationTests*, the result is a string associated with the expected error code.
* [remote-doc](remote-doc-manifest.jsonld) tests have _input_ and _expected_ documents.
  The _expected_ results can be compared using [JSON-LD object comparison](#json-ld-object-comparison) with the processor output.

  For *NegativeEvaluationTests*, the result is a string associated with the expected error code.

  Options may be present to describe the intended HTTP behavior:
  * _contentType_: Content-Type of the returned HTTP payload, defaults to the appropriate type for the _input_ suffix.
  * _httpStatus_: The HTTP status code to return, defaults to `200`.
  * _redirectTo_: The HTTP _Content-Location_ header value.
  * _httpLink_: The HTTP _Link_ header value.
* [fromRdf](fromRdf-manifest.jsonld) tests have _input_ and _expected_ documents.
  The _expected_ results  can be compared using [JSON-LD object comparison](#json-ld-object-comparison) with the processor output.
* [toRdf](toRdf-manifest.jsonld) tests have _input_ and _expected_ documents.
  The _expected_ results can be compared using [RDF Dataset Isomorphism](https://www.w3.org/TR/rdf11-concepts/#dfn-dataset-isomorphism).

  ToRdf tests may have a `expandContext` option, which is treated
  as an IRI relative to the manifest.

Unless `processingMode` is set explicitly in a test entry, `processingMode` is compatible with both `json-ld-1.0` and `json-ld-1.1`.

Test results that include a context input presume that the context is provided locally, and not from the referenced location, thus the results will include the content of the context file, rather than a reference.

## JSON-LD Object comparison

If algorithms are invoked with the `ordered` flag set to `true`, simple JSON Object comparison may be used, as the order of all arrays will be preserved (except for _fromRdf_, unless the input quads are also ordered). If `ordered` is `false`, then the following algorithm will ensure arrays other than values of `@list` are compared without regard to order.

JSON-LD Object comparison compares JSON objects, arrays, and values recursively for equality.

* JSON objects are compared member by member without regard to the ordering of members within the object. Each member must have a corresponding member in the object being compared to. Values are compared recursively.
* JSON arrays are generally compared without regard to order (the lone exception being if the referencing key is `@list`). Each item within the array must be equivalent to an item in the array being compared to by using the comparison algorithm recursively. For values of `@list`, the order of these items is significant.
* JSON values are compared using strict equality.
* Values of `@language`, and other places where language tags may be used are specified in lowercase in the test results. Implementations should either normalize language tags for testing purposes, or compare language tags in a case-independent way.

Note that some tests require re-expansion and comparison, as list values may exist as values of properties that have `@container: @list` and the comparison algorithm will not consider ordering significant.

# Running tests

The top-level [manifest](manifest.jsonld) references the specific test manifests, which in turn reference each test associated with a particular type of behavior.

Implementations create their own infrastructure for running the test suite. In particular, the following should be considered:

* _remote-doc_ tests will likely not return expected HTTP headers, so the _options_ should be used to determine what headers are associated with the input document.
* Test case properties identifying a file (_input_, _output_, _context_, _expectContext_, and _frame_) are presumed to have a media type appropriate for the file extension.
  * `application/ld+json` for `.jsonld`
  * `text/html` for `.html`
  * `application/n-quads` for `.nq`
* The media type for the file associated with the _input_ property can be overridden using the `contentType` option.
* Some algorithms, particularly _fromRdf_, may not preserve the order of statements listed in the input document, and provision should be taken for performing unordered array comparison, for arrays other than values of `@list`. (This may be difficult for compacted results, where array value ordering is dependent on the associated term definition).
* Some _toRdf_ tests require the use of [JSON Canonicalization Scheme](https://tools.ietf.org/html/draft-rundgren-json-canonicalization-scheme-05) to properly generate RDF Literals from JSON literal values. This algorithm is non-normative, but is assumed to be used to properly compare results using [RDF Dataset Isomorphism](https://www.w3.org/TR/rdf11-concepts/#dfn-dataset-isomorphism). These tests are marked using the `useJCS` option.
* When comparing documents after flattening, framing or generating RDF, blank node identifiers may not be predictable. Implementations using the JSON-LD 1.0 algorithm, where output is always sorted and blank node identifiers are generated sequentially from `_:b0` may continue to use a simple object comparison. Otherwise, implementations should take this into consideration. (One way to do this may be to reduce both results and _expected_ to datsets to extract a bijective mapping of blank node labels between the two datasets as described in [RDF Dataset Isomorphism](https://www.w3.org/TR/rdf11-concepts/#dfn-dataset-isomorphism)).
* Some tests may have a `requires` property, indicating some optional behavior described by a test vocabulary term.

# Contributing

If you would like to contribute a new test or a fix to an existing test,
please follow these steps:

1. Notify the JSON-LD mailing list, public-json-ld-wg@w3.org,
   that you will be creating a new test or fix and the purpose of the
   change.
2. Clone the git repository: git://github.com/w3c/json-ld-wg.git
3. Make your changes and submit them via github, or via a 'git format-patch'
   to the [JSON-LD Working Group mailing list](mailto:json-ld-wg@w3.org).

# Distribution
  Distributed under the [W3C Test Suite License](http://www.w3.org/Consortium/Legal/2008/04-testsuite-license). To contribute to a W3C Test Suite, see the [policies and contribution forms](http://www.w3.org/2004/10/27-testcases).

# Disclaimer
  UNDER THE EXCLUSIVE LICENSE, THIS DOCUMENT AND ALL DOCUMENTS, TESTS AND SOFTWARE THAT LINK THIS STATEMENT ARE PROVIDED "AS IS," AND COPYRIGHT HOLDERS MAKE NO REPRESENTATIONS OR WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT, OR TITLE; THAT THE CONTENTS OF THE DOCUMENT ARE SUITABLE FOR ANY PURPOSE; NOR THAT THE IMPLEMENTATION OF SUCH CONTENTS WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER RIGHTS.
  COPYRIGHT HOLDERS WILL NOT BE LIABLE FOR ANY DIRECT, INDIRECT, SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF ANY USE OF THE DOCUMENT OR THE PERFORMANCE OR IMPLEMENTATION OF THE CONTENTS THEREOF.
