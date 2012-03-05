package de.dfki.km.json.jsonld;

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
     * @return The generated triple, or null to force triple generation to stop
     */
    void triple(String s, String p, String o);

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
     * @return The generated triple, or null to force triple generation to stop
     */
    void triple(String s, String p, String value, String datatype, String language);
}
