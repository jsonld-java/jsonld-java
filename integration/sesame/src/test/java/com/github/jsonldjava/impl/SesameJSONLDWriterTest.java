/**
 * 
 */
package com.github.jsonldjava.impl;

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

}
