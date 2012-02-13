package de.dfki.km.json.jsonld;

public interface JSONLDTripleCallback {
	/**
	 * 
	 * @param s The Subject URI
	 * @param p The Predicate URI
	 * @param o The Object URI
	 * @return The generated triple, or null to force triple generaton to stop
	 */
	Object triple(String s, String p, String o);
	
	/**
	 * 
	 * @param s The Subject URI
	 * @param p The Predicate URI
	 * @param value The literal value
	 * @param datatype The literal datatype
	 * @param language The literal language (NOTE: may be null if not specified!)
	 * @return The generated triple, or null to force triple generaton to stop
	 */
	Object triple(String s, String p, String value, String datatype, String language);
}
