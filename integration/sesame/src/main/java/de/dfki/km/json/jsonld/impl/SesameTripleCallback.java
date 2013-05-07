package de.dfki.km.json.jsonld.impl;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.StatementCollector;

import de.dfki.km.json.jsonld.JSONLDTripleCallback;

public class SesameTripleCallback extends JSONLDTripleCallback {

    private ValueFactory vf = ValueFactoryImpl.getInstance();

    private RDFHandler handler;

    public SesameTripleCallback() {
	this(new StatementCollector(new LinkedHashModel()));
    }

    public SesameTripleCallback(RDFHandler nextHandler) {
	handler = nextHandler;
    }

    public SesameTripleCallback(RDFHandler nextHandler, ValueFactory vf) {
	handler = nextHandler;
    }

    @Override
    public void triple(String s, String p, String o, String graph) {
	if (s == null || p == null || o == null) {
	    // TODO: i don't know what to do here!!!!
	    return;
	}

	// This method is always called with three Resources as subject predicate and
	// object
	if (graph == null) {
	    Statement result = vf.createStatement(createResource(s),
		    vf.createURI(p), createResource(o));
	    try {
		handler.handleStatement(result);
	    } catch (RDFHandlerException e) {
		throw new RuntimeException(e);
	    }
	} else {
	    Statement result = vf.createStatement(createResource(s),
		    vf.createURI(p), createResource(o), createResource(graph));
	    try {
		handler.handleStatement(result);
	    } catch (RDFHandlerException e) {
		throw new RuntimeException(e);
	    }
	}

    }

    private Resource createResource(String resource) {
	// Blank node without any given identifier
	if (resource.equals("_:")) {
	    return vf.createBNode();
	} else if (resource.startsWith("_:")) {
	    return vf.createBNode(resource.substring(2));
	} else {
	    return vf.createURI(resource);
	}
    }

    @Override
    public void triple(String s, String p, String value, String datatype,
	    String language, String graph) {

	if (s == null || p == null || value == null) {
	    // TODO: i don't know what to do here!!!!
	    return;
	}

	Resource subject = createResource(s);

	URI predicate = vf.createURI(p);

	Value object;
	if (language != null) {
	    object = vf.createLiteral(value, language);
	} else if (datatype != null) {
	    object = vf.createLiteral(value, vf.createURI(datatype));
	} else {
	    object = vf.createLiteral(value);
	}

	Statement result;
	if (graph == null) {
	    result = vf.createStatement(subject, predicate, object);
	} else {
	    result = vf.createStatement(subject, predicate, object,
		    createResource(graph));
	}

	try {
	    handler.handleStatement(result);
	} catch (RDFHandlerException e) {
	    throw new RuntimeException(e);
	}
    }

    /**
     * @return the handler
     */
    public RDFHandler getHandler() {
	return handler;
    }

    /**
     * @param handler
     *            the handler to set
     */
    public void setHandler(RDFHandler handler) {
	this.handler = handler;
    }

}
