/**
 * 
 */
package de.dfki.km.json.jsonld;

import org.openrdf.rio.RDFWriterTest;

import de.dfki.km.json.jsonld.impl.SesameJSONLDParserFactory;
import de.dfki.km.json.jsonld.impl.SesameJSONLDWriterFactory;

/**
 * @author Peter Ansell p_ansell@yahoo.com
 */
public class SesameJSONLDWriterTest extends RDFWriterTest {

    public SesameJSONLDWriterTest() {
	super(new SesameJSONLDWriterFactory(), new SesameJSONLDParserFactory());
    }

}
