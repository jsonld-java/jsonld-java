package com.github.jsonldjava.impl;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.rio.ParseErrorListener;
import org.openrdf.rio.ParserConfig;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.helpers.ParseErrorLogger;
import org.openrdf.rio.helpers.RDFParserHelper;
import org.openrdf.rio.helpers.StatementCollector;

import com.github.jsonldjava.core.JSONLDTripleCallback;


public class SesameTripleCallback implements JSONLDTripleCallback {

    private ValueFactory vf;

    private RDFHandler handler;

    private ParserConfig parserConfig;

    private ParseErrorListener parseErrorListener;

    public SesameTripleCallback() {
	this(new StatementCollector(new LinkedHashModel()));
    }

    public SesameTripleCallback(RDFHandler nextHandler) {
	this(nextHandler, ValueFactoryImpl.getInstance());
    }

    public SesameTripleCallback(RDFHandler nextHandler, ValueFactory vf) {
	this(nextHandler, vf, new ParserConfig(), new ParseErrorLogger());
    }

    public SesameTripleCallback(RDFHandler nextHandler, ValueFactory vf, ParserConfig parserConfig, ParseErrorListener parseErrorListener) {
	this.handler = nextHandler;
	this.vf = vf;
	this.parserConfig = parserConfig;
	this.parseErrorListener = parseErrorListener;
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
	URI datatypeURI = datatype == null ? null : vf.createURI(datatype);
	
	Value object;
	try {
	    object = RDFParserHelper.createLiteral(value, language, datatypeURI, getParserConfig(), getParserErrorListener(), getValueFactory());
	} catch (RDFParseException e) {
	    throw new RuntimeException(e);
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

    public ParseErrorListener getParserErrorListener() {
	return this.parseErrorListener;
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

    /**
     * @return the parserConfig
     */
    public ParserConfig getParserConfig() {
	return parserConfig;
    }

    /**
     * @param parserConfig the parserConfig to set
     */
    public void setParserConfig(ParserConfig parserConfig) {
	this.parserConfig = parserConfig;
    }

    /**
     * @return the vf
     */
    public ValueFactory getValueFactory() {
	return vf;
    }

    /**
     * @param vf the vf to set
     */
    public void setValueFactory(ValueFactory vf) {
	this.vf = vf;
    }
    
    @Override
	public void triple(String s, String p, String o) {
		triple(s, p, o, null);
	}

	@Override
	public void triple(String s, String p, String value, String datatype,
			String language) {
		triple(s, p, value, datatype, language, null);
	}

	@Override
	public void processIgnored(Object parent, String parentId, String key,
			Object value) {
		// nothing to process
	}

}
