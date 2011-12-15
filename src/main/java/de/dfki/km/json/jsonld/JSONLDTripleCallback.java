package de.dfki.km.json.jsonld;

public interface JSONLDTripleCallback {
	// TODO: interface in the api is different from what it is in example code
	
	/*
	 * subject should always be an iri string
	 * predicate should always be an iri string
	 * object could be a string or a map<string,object>
	 * 
	 * return null to force triple generation to stop
	 * 
	 */
	Object triple(Object s, Object p, Object o);
}
