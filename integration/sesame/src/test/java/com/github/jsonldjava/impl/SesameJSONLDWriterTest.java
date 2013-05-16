/**
 * 
 */
package com.github.jsonldjava.impl;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFWriterTest;

import com.github.jsonldjava.impl.SesameJSONLDParserFactory;
import com.github.jsonldjava.impl.SesameJSONLDWriterFactory;


/**
 * @author Peter Ansell p_ansell@yahoo.com
 */
public class SesameJSONLDWriterTest extends RDFWriterTest {

    public SesameJSONLDWriterTest() {
	super(new SesameJSONLDWriterFactory(), new SesameJSONLDParserFactory());
    }
    
    @Ignore
    @Test
    @Override
	public void testRoundTrip()
		throws RDFHandlerException, IOException, RDFParseException
	{
		// Ignoring test as it is implemented as an RDF-1.0 test that is not compatible 
		// with RDF-1.1 Typed Literals after translating them to have xsd:String and rdf:langString.
	}
}
