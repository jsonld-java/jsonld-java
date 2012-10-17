package de.dfki.km.json.jsonld;

import java.util.HashMap;
import java.util.Map;

public class JSONLDProcessingError extends Exception {

	String message;
	Map details;
	
	public JSONLDProcessingError(String string, Map<String, Object> details) {
		message = string;
		details = details;
	}

	public JSONLDProcessingError(String string) {
		message = string;
		details = new HashMap();
	}

	public JSONLDProcessingError setDetail(String string, Object val) {
		details.put(string, val);
		System.out.println("ERROR DETAIL: " + string + ": " + val.toString());
		return this;
	}

	public enum Error {
		SYNTAX_ERROR
	}

	public JSONLDProcessingError setType(Error error) {
		// TODO Auto-generated method stub
		return this;
	};
	
}
