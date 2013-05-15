package com.github.jsonldjava.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.github.jsonldjava.utils.Obj;

/**
 * Base class for constructing the list of statements used by JSONLDProcessor.fromRDF to
 * generate JSON-LD. 
 * 
 * @author tristan
 * 
 */
public abstract class JSONLDSerializer {

    // name generator
    Iterator<String> _ng = new Iterator<String>() {
		int i = 0;
		@Override
		public void remove() {
			i++;
		}
		
		@Override
		public String next() {
			return "_:t" + i++;
		}
		
		@Override
		public boolean hasNext() {
			return true;
		}
	};
    Map<String, String> _bns;

    public JSONLDSerializer() {
        reset();
    }

    /**
     * Resets the Serializer. Call this if you want to reuse the serializer for a different document
     */
    public void reset() {
        _bns = new HashMap<String, String>();
    }

    protected String getNameForBlankNode(String node) {
        if (!_bns.containsKey(node)) {
            _bns.put(node, _ng.next());
        }
        return _bns.get(node);
    }
   
    public void setPrefix(String fullUri, String prefix) {
    	// TODO: graphs?
    	//_context.put(prefix, fullUri);
    }
    
    private List<Map<String,Object>> statements = new ArrayList<Map<String,Object>>();
    
    public List<Map<String,Object>> getStatements() {
		return statements;
	}

    public List<Map<String,Object>> getStatements(String subjectUri, String propertyUri, String objectUri) {
		List<Map<String,Object>> rval = new ArrayList<Map<String,Object>>();
		for (Map<String,Object> statement: getStatements()) {
			if (subjectUri != null) {
				if (!subjectUri.equals(Obj.get(statement, "subject", "nominalValue"))) {
					continue;
				}
			}
			if (propertyUri != null) {
				if (!propertyUri.equals(Obj.get(statement, "property", "nominalValue"))) {
					continue;
				}
			}
			if (objectUri != null) {
				if (!objectUri.equals(Obj.get(statement, "object", "nominalValue"))) {
					continue;
				}
			}
			
			rval.add(statement);
		}
		return rval;
	}
    
    /**
     * Take input and generate a list of statements from that input.
     * Must call the respective triple(...) method for each triple in the input.
     * 
     * @param input
     * @throws JSONLDProcessingError
     */
    public abstract void parse(Object input) throws JSONLDProcessingError;

    /**
     * used by fromRDF to apply any domain specific results to the resulting JSON-LD
     * 
     * @param rval
     * @return
     */
    public Object finalize(Object rval) {
		return rval;
	}
    
	protected void triple(String s, String p, String o, String g) {
    	Map<String,Object> statement = new HashMap<String, Object>();
    	Map<String,Object> subject = new HashMap<String, Object>();
    	subject.put("nominalValue", s);
    	if (s.startsWith("_:")) {
    		subject.put("interfaceName", "BlankNode");
    	} else {
    		subject.put("interfaceName", "IRI");
    	}
    	statement.put("subject", subject);
    	Map<String,Object> property = new HashMap<String, Object>();
    	property.put("nominalValue", p);
    	property.put("interfaceName", "IRI");
    	statement.put("property", property);
    	Map<String,Object> object = new HashMap<String, Object>();
    	object.put("nominalValue", o);
    	if (o.startsWith("_:")) {
    		object.put("interfaceName", "BlankNode");
    	} else {
    		object.put("interfaceName", "IRI");
    	}
    	statement.put("object", object);
    	if (g != null) {
    		Map<String,Object> graph = new HashMap<String, Object>();
        	graph.put("nominalValue", g);
        	if (g.startsWith("_:")) {
        		graph.put("interfaceName", "BlankNode");
        	} else {
        		graph.put("interfaceName", "IRI");
        	}
    		statement.put("name", graph);
    	}
    	
    	// make sure this statement is unique
    	for (Map<String,Object> si: statements) {
    		if (JSONLDProcessor.compareRdfStatements(si, statement)) {
    			return;
    		}
    	}
    	statements.add(statement);
    }
    
    protected void triple(String s, String p, String value, String datatype, String language, String g) {
    	Map<String,Object> statement = new HashMap<String, Object>();
    	Map<String,Object> subject = new HashMap<String, Object>();
    	subject.put("nominalValue", s);
    	if (s.startsWith("_:")) {
    		subject.put("interfaceName", "BlankNode");
    	} else {
    		subject.put("interfaceName", "IRI");
    	}
    	statement.put("subject", subject);
    	Map<String,Object> property = new HashMap<String, Object>();
    	property.put("nominalValue", p);
    	property.put("interfaceName", "IRI");
    	statement.put("property", property);
    	Map<String,Object> object = new HashMap<String, Object>();
    	object.put("nominalValue", value);
    	object.put("interfaceName", "LiteralNode");
    	if (datatype != null) {
    		Map<String,Object> dt = new HashMap<String, Object>();
    		dt.put("nominalValue", datatype);
	    	dt.put("interfaceName", "IRI");
	    	object.put("datatype", dt);
    	} else if (language != null) {
    		object.put("language", language);
    	}
    	statement.put("object", object);
    	if (g != null) {
    		Map<String,Object> graph = new HashMap<String, Object>();
        	graph.put("nominalValue", g);
        	if (g.startsWith("_:")) {
        		graph.put("interfaceName", "BlankNode");
        	} else {
        		graph.put("interfaceName", "IRI");
        	}
    		statement.put("name", graph);
    	}
    	
    	// make sure this statement is unique
    	for (Map<String,Object> si: statements) {
    		if (JSONLDProcessor.compareRdfStatements(si, statement)) {
    			return;
    		}
    	}
    	statements.add(statement);
    }
    
    /**
     * internal function to avoid repetition of code
     * 
     * @param s
     * @param p
     * @param value
     */
    protected void triple(String s, String p, String o) {
        triple(s, p, o, null);
    }

    /**
     * Call this to add a literal to the JSON-LD document
     * 
     * @param s
     *            the subjuct URI
     * @param p
     *            the predicate URI
     * @param value
     *            the literal value as a string
     * @param datatype
     *            the datatype URI (if null, a plain literal is assumed)
     * @param language
     *            the language (may be null, or an empty string)
     */
    protected void triple(String s, String p, String value, String datatype, String language) {
        triple(s, p, value, datatype, language, null);
    }

	public Map<String,Object> parse1(Object dataset) throws JSONLDProcessingError {
		// TODO This is here temporarily so that the other serialization method still exists until i can figure out what to do with it
		return null;
	}
}
