package com.github.jsonldjava.impl;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.RDFDataset;
import com.github.jsonldjava.core.RDFDatasetUtils;
import com.github.jsonldjava.core.RDFParser;

public class NQuadRDFParser implements RDFParser {
    @Override
    public RDFDataset parse(Object input) throws JsonLdError {
        if (input instanceof String) {
            return RDFDatasetUtils.parseNQuads((String) input);
        } else {
            throw new JsonLdError(JsonLdError.Error.INVALID_INPUT,
                    "NQuad Parser expected string input.");
        }
    }

}
