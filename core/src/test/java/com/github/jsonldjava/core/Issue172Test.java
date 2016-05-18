/* Created on 7 mai 2016 */
package com.github.jsonldjava.core;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.github.jsonldjava.core.RDFDataset.Quad;
import com.github.jsonldjava.utils.JsonUtils;

public class Issue172Test {

	/** many triples with same subject and prop: current implementation is slow */
	@Test public final void slowVsFast() throws JsonLdError, JsonGenerationException, IOException {
		RDFDataset inputRdf = new RDFDataset();
		String ns = "http://www.example.com/foo/";
		inputRdf.setNamespace("ex", ns);

		int n = 2000;
		for (int i = 0 ; i < n ; i++) {
			inputRdf.addTriple(ns + "s", ns + "o", ns + "p" + Integer.toString(i));  	
		}

		final JsonLdOptions options = new JsonLdOptions();
		options.useNamespaces = true;


		// warming
		for (int i = 0 ; i < 2 ; i++) {
			JsonLdProcessor.expand(new JsonLdApi(options).fromRDF(inputRdf));
		}

		for (int i = 0 ; i < 2 ; i++) {
			JsonLdProcessor.expand(new JsonLdApi(options).fromRDF(inputRdf, true));
		}

		int nb = 10;
		long start = System.currentTimeMillis();
		for (int i = 0 ; i < nb ; i++) {
			JsonLdProcessor.expand(new JsonLdApi(options).fromRDF(inputRdf));
		}
		System.out.println("Time to expand a dataset containing one subject with " + n + "different values of one prop:");
		System.out.println("\t- JSON-LD java as it is: " + (((System.currentTimeMillis() - start)) / nb));

		start = System.currentTimeMillis();
		for (int i = 0 ; i < nb ; i++) {
			JsonLdProcessor.expand(new JsonLdApi(options).fromRDF(inputRdf, true));
		}
		System.out.println("\t- As it could be (?): " + (((System.currentTimeMillis() - start)) / nb));
	}

	@Test public final void duplicatedTriplesInAnRDFDataset() throws JsonLdError, JsonGenerationException, IOException {
		RDFDataset inputRdf = new RDFDataset();
		String ns = "http://www.example.com/foo/";
		inputRdf.setNamespace("ex", ns);
		inputRdf.addTriple(ns + "s", ns + "p", ns + "o");
		inputRdf.addTriple(ns + "s", ns + "p", ns + "o");

		System.out.println("Twice the same triple in RDFDataset:/n");
		for (Quad quad : inputRdf.getQuads("@default")) {
			System.out.println(quad);
		}

		final JsonLdOptions options = new JsonLdOptions();
		options.useNamespaces = true;

		Object fromRDF;
		String jsonld;

		System.out.println("\nJSON-LD output is OK:\n");
		fromRDF = JsonLdProcessor.compact(new JsonLdApi(options).fromRDF(inputRdf),
				inputRdf.getContext(), options);

		jsonld = JsonUtils.toPrettyString(fromRDF);
		System.out.println(jsonld);

		System.out.println("\nWouldn't be the case assuming there is no duplicated triple in RDFDataset:\n");
		fromRDF = JsonLdProcessor.compact(new JsonLdApi(options).fromRDF(inputRdf, true),
				inputRdf.getContext(), options);
		jsonld = JsonUtils.toPrettyString(fromRDF);
		System.out.println(jsonld);

	}
}