package com.github.jsonldjava.impl;

import java.util.Map;
import java.util.regex.Pattern;

import com.github.jsonldjava.core.JSONLDProcessingError;
import com.github.jsonldjava.core.JSONLDSerializer;

import static com.github.jsonldjava.core.RDFDatasetUtils.parseNQuads;


public class NQuadJSONLDSerializer extends JSONLDSerializer {
	
	final Pattern iri = Pattern.compile("(?:<([^:]+:[^>]*)>)");
	final Pattern bnode = Pattern.compile("(_:(?:[A-Za-z][A-Za-z0-9]*))");
	final Pattern plain = Pattern.compile("\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"");
	final Pattern datatype = Pattern.compile("(?:\\^\\^" + iri + ")");
	final Pattern language = Pattern.compile("(?:@([a-z]+(?:-[a-z0-9]+)*))");
	final Pattern literal = Pattern.compile("(?:" + plain + "(?:" + datatype + "|" + language + ")?)");
	final Pattern ws = Pattern.compile("[ \\t]+");
	final Pattern wso = Pattern.compile("[ \\t]*");
	final Pattern eoln = Pattern.compile("(?:\r\n)|(?:\n)|(?:\r)");
	final Pattern empty = Pattern.compile("^" + wso + "$");
	
	final Pattern subject = Pattern.compile("(?:" + iri + "|" + bnode + ")" + ws);
	final Pattern property = Pattern.compile(iri.pattern() + ws.pattern());
	final Pattern object = Pattern.compile("(?:" + iri + "|" + bnode + "|" + literal + ")" + wso);
	final Pattern graph = Pattern.compile("(?:\\.|(?:(?:" + iri + "|" + bnode + ")" + wso + "\\.))");
	
	final Pattern quad = Pattern.compile("^" + wso + subject + property + object + graph + wso + "$");
	
	private void parse(String input) throws JSONLDProcessingError {
		
	}
	
	@Override
	public Map<String,Object> parse1(Object dataset) throws JSONLDProcessingError {
		if (dataset instanceof String) {
			return parseNQuads((String)dataset);
		} else {
			throw new JSONLDProcessingError("NQuad Parser expected string input.")
				.setType(JSONLDProcessingError.Error.INVALID_INPUT)
				.setDetail("input", dataset);
		}
	}
	
	@Override
	public void parse(Object input) throws JSONLDProcessingError {
		if (input instanceof String) {
			parse((String)input);
		}
	}

}
