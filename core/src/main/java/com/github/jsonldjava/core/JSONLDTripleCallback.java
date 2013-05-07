package com.github.jsonldjava.core;

public interface JSONLDTripleCallback {
    /**
     * Construct a triple with three URIs.
     * 
     * @param s
     *            The Subject URI
     * @param p
     *            The Predicate URI
     * @param o
     *            The Object URI
     * @param graph
     * 			  The graph URI associated with this triple (null if none)
     * @return The generated triple, or null to force triple generation to stop
     */
    void triple(String s, String p, String o);
    void triple(String s, String p, String o, String graph);

    /**
     * Constructs a triple with a Literal object, which may or may not contain a
     * language and/or a datatype.
     * 
     * @param s
     *            The Subject URI
     * @param p
     *            The Predicate URI
     * @param value
     *            The literal value
     * @param datatype
     *            The literal datatype
     * @param language
     *            The literal language (NOTE: may be null if not specified!)
     * @param graph
     * 			  The graph URI associated with this triple (null if none)
     * @return The generated triple, or null to force triple generation to stop
     */
    void triple(String s, String p, String value, String datatype, String language);
	void triple(String s, String p, String value, String datatype,
			String language, String graph);

	/**
	 * Implement this to trigger special processing for ignored keywords
	 * 
	 * @param parent the parent object which contains the ignored keyword
	 * @param key
	 * @param value
	 */
	public void processIgnored(Object parent, String parentId, String key, Object value);
}
