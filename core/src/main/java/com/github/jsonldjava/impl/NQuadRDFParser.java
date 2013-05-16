package com.github.jsonldjava.impl;

import java.util.Map;

import com.github.jsonldjava.core.JSONLDProcessingError;
import com.github.jsonldjava.core.RDFParser;

import static com.github.jsonldjava.core.RDFDatasetUtils.parseNQuads;


public class NQuadRDFParser implements RDFParser {
	@Override
	public Map<String,Object> parse(Object dataset) throws JSONLDProcessingError {
		if (dataset instanceof String) {
			return parseNQuads((String)dataset);
		} else {
			throw new JSONLDProcessingError("NQuad Parser expected string input.")
				.setType(JSONLDProcessingError.Error.INVALID_INPUT)
				.setDetail("input", dataset);
		}
	}
	
	

}
