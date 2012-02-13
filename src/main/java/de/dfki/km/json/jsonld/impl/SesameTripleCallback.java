package de.dfki.km.json.jsonld.impl;


import org.openrdf.model.Graph;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.model.impl.ValueFactoryImpl;

import de.dfki.km.json.jsonld.JSONLDTripleCallback;

public class SesameTripleCallback implements JSONLDTripleCallback {

    private ValueFactory vf = ValueFactoryImpl.getInstance();

    private Graph storageGraph = new GraphImpl();
    
    @Override
	public Object triple(String s, String p, String o) {
		if (s == null || p == null || o == null) {
			// TODO: i don't know what to do here!!!!
			return null;
		}
		
		// This method is always called with three URIs as subject predicate and object
		Statement result = vf.createStatement(vf.createURI(s), vf.createURI(p), vf.createURI(o));
		storageGraph.add(result);
		return result;
	}

	@Override
	public Object triple(String s, String p, String value, String datatype,
			String language) {
		
        if (s == null || p == null || value == null) {
            // TODO: i don't know what to do here!!!!
            return null;
        }
        
	    URI subject = vf.createURI(s);
	    
	    URI predicate = vf.createURI(p);
		
		Value object;
		if (language != null) {
			object = vf.createLiteral(value, language);
		} else if (datatype != null) {
			object = vf.createLiteral(value, vf.createURI(datatype));
		} else {
            object = vf.createLiteral(value);
		}
		
        Statement result = vf.createStatement(subject, predicate, object);
        storageGraph.add(result);
        return result;
	}

    /**
     * @return the storageGraph
     */
    public Graph getStorageGraph()
    {
        return storageGraph;
    }

    /**
     * @param storageGraph the storageGraph to set
     */
    public void setStorageGraph(Graph storageGraph)
    {
        this.storageGraph = storageGraph;
    }

}
