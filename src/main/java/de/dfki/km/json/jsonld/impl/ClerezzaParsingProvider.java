package de.dfki.km.json.jsonld.impl;

import java.io.InputStream;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.serializedform.ParsingProvider;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;

import de.dfki.km.json.jsonld.JSONLDProcessor;

@SupportedFormat("application/rdf+json")
public class ClerezzaParsingProvider implements ParsingProvider {

	@Override
	public void parse(MGraph target, InputStream serializedGraph,
			String formatIdentifier, UriRef baseUri) {
		JSONLDProcessor processor = new JSONLDProcessor();
		ClerezzaTripleCallback callback = new ClerezzaTripleCallback();
		processor.triples(serializedGraph, callback);
	}

}
