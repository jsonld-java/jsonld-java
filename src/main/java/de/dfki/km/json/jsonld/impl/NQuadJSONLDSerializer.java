package de.dfki.km.json.jsonld.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.dfki.km.json.jsonld.JSONLDProcessingError;
import de.dfki.km.json.jsonld.JSONLDSerializer;

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
		int lineNumber = 0;
		for (String line: input.split(eoln.pattern())) {
			lineNumber++;
			
			// skip empty lines
			if (empty.matcher(line).matches()) {
				continue;
			}
			
			// parse quad
			Matcher match = quad.matcher(line);
			if (!match.matches()) {
				throw new JSONLDProcessingError("Error while parsing N-Quads; invalid quad.")
					.setType(JSONLDProcessingError.Error.PARSE_ERROR)
					.setDetail("line", lineNumber);						
			}
			
			// get subject
			String subject;
			if (match.group(1) != null) {
				subject = match.group(1);
			} else {
				subject = match.group(2);
			}
			
			// get property
			String property = match.group(3);
			
			// get object
			String object = null;
			String value = null;
			String datatype = null;
			String language = null;
			if (match.group(4) != null) {
				object = match.group(4);
			} else if (match.group(5) != null) {
				object = match.group(5);
			} else {
				value = match.group(6)
						.replaceAll("\\\\", "\\\\\\\\")
						.replaceAll("\\t", "\\\\t")
						.replaceAll("\\n", "\\\\n")
						.replaceAll("\\r", "\\\\r")
						.replaceAll("\\\"", "\\\\\"");
				datatype = match.group(7);
				language = match.group(8);
			}
			
			// get graph
			String graph = match.group(9);
			if (graph == null) {
				graph = match.group(10);
			}
			
			// add statement
			if (object != null) {
				triple(subject, property, object, graph);
			} else {
				triple(subject, property, value, datatype, language, graph);
			}
		}
	}
	
	@Override
	public void parse(Object input) throws JSONLDProcessingError {
		if (input instanceof String) {
			parse((String)input);
		}
	}

}
