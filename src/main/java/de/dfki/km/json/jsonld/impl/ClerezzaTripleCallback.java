package de.dfki.km.json.jsonld.impl;


import java.util.HashMap;
import java.util.Map;

import de.dfki.km.json.jsonld.JSONLDTripleCallback;
import org.apache.clerezza.rdf.core.*;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.impl.TypedLiteralImpl;

public class ClerezzaTripleCallback implements JSONLDTripleCallback {

    private MGraph mGraph = new SimpleMGraph();
    private Map<String, BNode> bNodeMap = new HashMap<String, BNode>();

    public void setMGraph(MGraph mGraph) {
        this.mGraph = mGraph;
        bNodeMap = new HashMap<String, BNode>();
    }

    public MGraph getMGraph() {
        return mGraph;
    }

    public void triple(String s, String p, String o) {
        if (s == null || p == null || o == null) {
            // TODO: i don't know what to do here!!!!
            return;
        }

        NonLiteral subject = getNonLiteral(s);
		UriRef predicate = new UriRef(p);
		NonLiteral object = getNonLiteral(o);
		mGraph.add(new TripleImpl(subject, predicate, object));
    }

    @Override
    public void triple(String s, String p, String value, String datatype, String language) {
        NonLiteral subject = getNonLiteral(s);
		UriRef predicate = new UriRef(p);
		Resource object;
		if (language != null) {
			object = new PlainLiteralImpl(value, new Language(language)); 
		} else {
			if (datatype != null) {
				object = new TypedLiteralImpl(value, new UriRef(datatype));
			} else {
				object = new PlainLiteralImpl(value);
			}
		}
      
		mGraph.add(new TripleImpl(subject, predicate, object));
    }

	private NonLiteral getNonLiteral(String s) {
		if (s.startsWith("_:")) {
			return getBNode(s);
		} else {
			return new UriRef(s);
		}
	}

	private BNode getBNode(String s) {
		if (bNodeMap.containsKey(s)) {
			return bNodeMap.get(s);
		} else {
			BNode result = new BNode();
			bNodeMap.put(s, result);
			return result;
		}
	}

}
