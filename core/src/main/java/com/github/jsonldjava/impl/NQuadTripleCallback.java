package com.github.jsonldjava.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.jsonldjava.core.JSONLDTripleCallback;


public class NQuadTripleCallback implements JSONLDTripleCallback {

	private List<String> lines = new ArrayList<String>();
	
	@Override
	public void triple(String s, String p, String o, String graph) {
		String quad = "";
		if (s.startsWith("_:")) {
			quad += s;
		} else {
			quad += "<" + s + ">";
		}
		
		quad += " <" + p + "> " ;
		
		if (o.startsWith("_:")) {
			quad += o;
		} else {
			quad += "<" + o + ">";
		}
		
		if (graph != null) {
			quad += " <" + graph + ">";
		}
		
		quad += " .";
		lines.add(quad);
	}

	@Override
	public void triple(String s, String p, String value, String datatype,
			String language, String graph) {
		String quad = "";
		
		if (s.startsWith("_:")) {
			quad += s;
		} else {
			quad += "<" + s + ">";
		}
		
		quad += " <" + p + "> " ;
		
		String escaped = value
				.replaceAll("\\\\", "\\\\\\\\")
				.replaceAll("\\t", "\\\\t")
				.replaceAll("\\n", "\\\\n")
				.replaceAll("\\r", "\\\\r")
				.replaceAll("\\\"", "\\\\\"");
		quad += "\"" + escaped + "\"";
		if (datatype != null && !"http://www.w3.org/2001/XMLSchema#string".equals(datatype)) {
			quad += "^^<" + datatype + ">";
		} else if (language != null) {
			quad += "@" + language;
		}
		
		if (graph != null) {
			quad += " <" + graph + ">";
		}
		
		quad += " .";
		lines.add(quad);
	}
	

	public String getResult() {
		String result = "";
		Collections.sort(lines);
		for (int i = 0; i < lines.size(); i++) {
			result += lines.get(i);
			if (i < lines.size()-1) {
				result += "\n";
			}
		}
		return result; 
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
