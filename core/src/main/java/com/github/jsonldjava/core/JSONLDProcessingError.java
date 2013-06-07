package com.github.jsonldjava.core;

import java.util.HashMap;
import java.util.Map;

public class JSONLDProcessingError extends Exception {

	Map<String,Object> details;
	private Error type;
	
	public JSONLDProcessingError(String string, Map<String, Object> details) {
		super(string);
		this.details = details;
	}

	public JSONLDProcessingError(String string) {
		super(string);
		details = new HashMap();
	}

	public JSONLDProcessingError setDetail(String string, Object val) {
		details.put(string, val);
		//System.out.println("ERROR DETAIL: " + string + ": " + val.toString());
		return this;
	}

	public enum Error {
		SYNTAX_ERROR, PARSE_ERROR, RDF_ERROR, CONTEXT_URL_ERROR, INVALID_URL, COMPACT_ERROR, CYCLICAL_CONTEXT, FLATTEN_ERROR, FRAME_ERROR, NORMALIZE_ERROR, UNKNOWN_FORMAT, INVALID_INPUT
	}

	public JSONLDProcessingError setType(Error error) {
		this.type = error;
		return this;
	};
	
	public Error getType() {
		return type;
	}
	
	public Map<String,Object> getDetails() {
		return details;
	}
	
	@Override
	public String getMessage() {
		String msg = super.getMessage();
		for (String key: details.keySet()) {
			msg += " {" + key + ":" + details.get(key) + "}";
		}
		return msg;
	}
}
