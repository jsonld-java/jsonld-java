package com.github.jsonldjava.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.nio.CharBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.jsonldjava.core.JSONLD;
import com.github.jsonldjava.core.JSONLDProcessingError;
import com.github.jsonldjava.core.Options;

public class EarlTestSuite {
	
	private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
	private static final String FILE_SEP = System.getProperty("file.separator");
	private String cacheDir;
	private String etag;
	private Map<String, Object> manifest;
	private List<Map<String, Object>> tests;

	public EarlTestSuite(String manifestURL) throws IOException {
		this(manifestURL, null, null);
	}
	
	/**
	 * Loads an earl test suite
	 * 
	 * @param manifestURL the URL of the manifest file
	 * @param cacheDir the base directory to cache the files into
	 * @param etag the ETag field to load (useful if you know you have the latest manifest cached and don't want to query the server for a new one every time you run the tests).
	 * @throws IOException
	 */
	public EarlTestSuite(String manifestURL, String cacheDir, String etag) throws IOException {
		if (cacheDir == null) {
			cacheDir = getCacheDir(manifestURL);
		}
		this.cacheDir = cacheDir;
		new File(this.cacheDir).mkdirs();
		
		if (etag == null) {
			java.net.URLConnection conn = new java.net.URL(manifestURL).openConnection();
			this.etag = conn.getHeaderField("ETag");
		} else {
			this.etag = etag;
		}
		final String manifestFile = getFile(manifestURL);
		
		if (manifestURL.endsWith(".ttl") || manifestURL.endsWith("nq") || manifestURL.endsWith("nt")) {
        	try {
				Map<String,Object> rval = (Map<String, Object>) JSONLD.fromRDF(manifestFile, 
						new Options(manifestURL) {{
							this.format = "text/turtle"; 
							this.useNamespaces = true; 
							this.outputForm = "compacted"; 
						}});
				
				Map<String,Object> frame = new LinkedHashMap<String, Object>();
				frame.put("@context", rval.get("@context"));
				frame.put("@type", "http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#Manifest");
				// make manifest the base object, embeding any referenced items (e.g. the test entries)
				rval = (Map<String, Object>) JSONLD.frame(rval, frame, new Options(manifestURL));
				// compact to remove the @graph label
				this.manifest = (Map<String, Object>) JSONLD.compact(rval, frame.get("@context"), new Options(manifestURL));
				this.tests = (List<Map<String,Object>>)Obj.get(this.manifest, "mf:entries", "@list");
				
			} catch (JSONLDProcessingError e) {
				throw new RuntimeException(e);
			}
        } else if (manifestURL.endsWith(".jsonld") || manifestURL.endsWith(".json")) {
        	Object rval = JSONUtils.fromString(manifestFile);
        	if (rval instanceof Map) {
        		this.manifest = (Map<String,Object>)rval;
        		this.tests = (List<Map<String,Object>>)Obj.get(this.manifest, "sequence");
        	} else {
        		throw new RuntimeException("expected JSON manifest file result to be an Object");
        	}
        } else {
        	throw new RuntimeException("unknown manifest file format");
        }
	}
	
	public String getFile(String url) throws IOException {
		URL url_ = URL.parse(url.toString());
		String fn = this.cacheDir + url_.file + "." + this.etag;
		File f = new File(fn);

		BufferedWriter fw = null;
		BufferedReader in;
		if (f.exists()) {
			in = new BufferedReader(
					   new InputStreamReader(
			                      new FileInputStream(f), "UTF-8"));
		} else {
			java.net.URLConnection conn = new java.net.URL(url).openConnection();
			in = new BufferedReader(
					   new InputStreamReader(
							   conn.getInputStream(), "UTF-8"));
			f.createNewFile();
			fw = new BufferedWriter(
					new OutputStreamWriter(
							new FileOutputStream(f), "UTF-8"));
		}

		StringWriter sw = new StringWriter();
		char[] str = new char[1024];
		int read;
		while ((read = in.read(str)) >= 0) {
		    sw.write(str, 0, read);
		    if (fw != null) {
		    	fw.write(str, 0, read);
		    }
		}

        in.close();
        if (fw != null) {
        	fw.close();
        }
        
        return sw.toString();
	}
	
	private static String getCacheDir(String url) {
		URL url_ = URL.parse(url);
		String dir = url_.path.substring(0, url_.path.lastIndexOf("/")+1);
		if (!FILE_SEP.equals("/")) {
			dir = dir.replace("/", FILE_SEP);
		}
		return TEMP_DIR + dir;
	}

	public List<Map<String,Object>> getTests() {
		return tests;
	}
	
}
