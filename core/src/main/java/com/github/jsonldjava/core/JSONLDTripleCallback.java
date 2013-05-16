package com.github.jsonldjava.core;

import java.util.Map;

/**
 * 
 * @author Tristan
 *
 * TODO: in the JSONLD RDF API the callback we're representing here is QuadCallback which takes
 * a list of quads (subject, predicat, object, graph). for the moment i'm just going to use
 * the dataset provided by toRDF but this should probably change in the future
 */
public interface JSONLDTripleCallback {
	
	/**
	 * Construct output based on internal RDF dataset format
	 * @param dataset
	 * 
	 * @return the resulting RDF object in the desired format
	 */
	public Object call(Map<String, Object> dataset);
}
