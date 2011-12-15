package de.dfki.km.json.jsonld;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.dfki.km.json.JSONUtils;
import de.dfki.km.json.jsonld.JSONLDTripleCallback;
import de.dfki.km.json.jsonld.JSONLDUtils;
import de.dfki.km.json.jsonld.impl.JSONLDProcessor;

@RunWith(Parameterized.class)
public class JSONLDProcessorTest {

	private static final String TEST_DIR = "testfiles";

	private class TestTripleCallback implements JSONLDTripleCallback {
	
		public Object triple(Object s, Object p, Object o) {
			String rval;
			if (o instanceof String) {
				// literal
				rval = "<" + s + "> <" + p + "> \"" + o + "\" .";
			} else if (o instanceof Map && ((Map<String,Object>) o).containsKey("@iri")) {
				// object is an iri
				rval = "<" + s + "> <" + p + "> <" + ((Map<String,Object>) o).get("@iri") + "> .";
			} else {
				// object is a literal
				rval = "<" + s + "> <" + p + "> \"" + ((Map<String,Object>) o).get("@literal") + "\"";
				rval += "^^<" + ((Map<String,Object>) o).get("@datatype");
				rval += "> .";
			}
			return rval;
		}
	}
	
	@Parameters
	public static Collection<Object[]> data() throws URISyntaxException, IOException {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		File f = new File(cl.getResource(TEST_DIR).toURI());
		List<File> tests = Arrays.asList(
			f.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					if (name.endsWith(".test")) {
						return true;
					}
					return false;
				}}));
		
		Collection<Object[]> rdata = new ArrayList<Object[]>();
		int count = 0;
		for (File in: tests) {
			FileInputStream testfile = new FileInputStream(in);
		
			Map<String,Object> testgroup = 
					(Map<String, Object>) JSONUtils.fromInputStream(testfile);
			
			for (Map<String,Object> test: (List<Map<String,Object>>)testgroup.get("tests")) {
				rdata.add(new Object[] { testgroup.get("group"), test });
				
				// Uncomment this to print a list of all the tests (may make debugging easier
				//System.out.println("Added Test[" + count++ + "]: " + testgroup.get("group") + " " + test.get("name") + "..."
				//		+ " (" + test.get("input") + "," + test.get("expect") + ")" 
				//		);
			}
		}
		return rdata;
	}

	private String group;
	private Map<String,Object> test;
	
	public JSONLDProcessorTest(final String group, final Map<String,Object> test) {
		this.group = group;
		this.test = test;
	}
	
	private static String join(Collection<String> list, String delim) {
		StringBuilder builder = new StringBuilder();
	    Iterator<String> iter = list.iterator();
	    while (iter.hasNext()) {
	    	builder.append(iter.next());
	        if (!iter.hasNext()) {
	        	break;                  
	        }
	        builder.append(delim);
	    }
	    return builder.toString();
	}
	
	@Test
	public void runTest() throws URISyntaxException, IOException {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
				
		InputStream inputStream = 
				cl.getResourceAsStream(TEST_DIR + "/" + test.get("input"));
		assertNotNull("unable to find input file: " + test.get("input"), inputStream);
		
		InputStream expectStream = 
				cl.getResourceAsStream(TEST_DIR + "/" + test.get("expect"));
		assertNotNull("unable to find expect file: " + test.get("expect"), expectStream);
		
		Object inputJson = JSONUtils.fromInputStream(inputStream);
		Object expect;
		String expectType = (String) test.get("expect");
		expectType = expectType.substring(expectType.lastIndexOf(".")+1);
		if (expectType.equals("json")) {
			expect = JSONUtils.fromInputStream(expectStream);
		} else if (expectType.equals("nt")) {
			List<String> expectLines = new ArrayList<String>();
			BufferedReader buf = new BufferedReader(new InputStreamReader(expectStream));
			String line;
			while ((line = buf.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0 || line.charAt(0) == '#') {
					continue;
				}
				expectLines.add(line);
			}
			Collections.sort(expectLines);
			expect = join(expectLines, "\n");
		} else {
			expect = "";
			assertFalse("Unknown expect type: " + expectType, true);
		}
		
		Object result = null;
		
		String testType = (String) test.get("type");
		if ("normalize".equals(testType)) {
			result = new JSONLDProcessor().normalize(inputJson);
		} else if ("expand".equals(testType)) {
			result = new JSONLDProcessor().expand(inputJson);
		} else if ("compact".equals(testType)) {
			InputStream contextStream = 
					cl.getResourceAsStream(TEST_DIR + "/" + test.get("context"));
			Object contextJson = JSONUtils.fromInputStream(contextStream);
			result = new JSONLDProcessor().compact(inputJson, contextJson);
		} else if ("frame".equals(testType)) {
			InputStream frameStream = 
					cl.getResourceAsStream(TEST_DIR + "/" + test.get("context"));
			Object frameJson = JSONUtils.fromInputStream(frameStream);
			result = new JSONLDProcessor().frame(inputJson, frameJson);
		} else if ("triples".equals(testType)) {
			// TODO: many of the tests here fail simply because of an ordering issue
			
			TestTripleCallback ttc = new TestTripleCallback();
			List<String> results = 
					(List<String>) new JSONLDProcessor().triples(inputJson, ttc);
			Collections.sort(results);
			result = join(results, "\n");
		} else {
			assertFalse("Unknown test type", true);
		}
	
		assertTrue("\nFailed test: " + this.group + " " + this.test.get("name") + 
				" (" + test.get("input") + "," + test.get("expect") + ")\n" +
				"expected: " + expect + "\nresult: " + result, 
				JSONLDUtils.compare(expect, result) == 0);
	}
	
}
