package com.github.jsonldjava.impl;

import com.github.jsonldjava.core.JSONLDProcessingError;
import com.github.jsonldjava.core.RDFDataset;
import com.github.jsonldjava.core.RDFParser;

import static com.github.jsonldjava.core.RDFDatasetUtils.parseNQuads;


public class NQuadRDFParser implements RDFParser {
	@Override
	public RDFDataset parse(Object input) throws JSONLDProcessingError {
		if (input instanceof String) {
			return (RDFDataset)parseNQuads((String)input);
		} else {
			throw new JSONLDProcessingError("NQuad Parser expected string input.")
				.setType(JSONLDProcessingError.Error.INVALID_INPUT)
				.setDetail("input", input);
		}
	}
	
	

}
