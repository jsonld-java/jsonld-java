package com.github.jsonldjava.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.jsonldjava.core.JSONLDProcessingError;
import com.github.jsonldjava.core.RDFDataset;
import com.github.jsonldjava.core.RDFDatasetUtils;
import com.github.jsonldjava.core.RDFParser;

import com.github.jsonldjava.core.RDFDatasetUtils.Regex;

import static com.github.jsonldjava.core.JSONLDConsts.*;

public class TurtleRDFParser implements RDFParser {

	private class State {
		String baseIri = null;
		Map<String,String> namespaces = new LinkedHashMap<String, String>();
		Map<String, String> bnodeLabels = new LinkedHashMap<String, String>();
		String curSubject = null;
		String curPredicate = null;
		
		String[] lines = null;
		String line = null;
		
		int lineNumber = 0;
		int linePosition = 0;
		
		int bnodes = 0;
		
		private Stack<Map<String, String>> stack = new Stack<Map<String,String>>();
		public boolean expectingBnodeClose = false;
		
		public State(String input) throws JSONLDProcessingError {
			lines = Regex.EOLN.split(input);
			advanceLineNumber();
			clearInitialWhitespace();
		}

		public void push() {
			stack.push(new LinkedHashMap<String, String>() {{
				put(curSubject, curPredicate);
			}});
			expectingBnodeClose = true;
			curSubject = null;
			curPredicate = null;
		}
		
		public void pop() {
			if (stack.size() > 0) {
				for (Entry<String, String> x : stack.pop().entrySet()) {
					curSubject = x.getKey();
					curPredicate = x.getValue();
				}
			}
			if (stack.size() == 0) {
				expectingBnodeClose = false;
			}
		}
		
		private void advanceLineNumber() {
			if (lineNumber < lines.length) {
				line = lines[lineNumber++];
			} else {
				line = null;
			}
		}
		
		public void advanceLinePosition(int len) throws JSONLDProcessingError {
			if (len <= 0) {
				return;
			}
			linePosition += len;
			line = line.substring(len);
			while (line != null && Regex.EMPTY.matcher(line).matches()) {
				advanceLineNumber();
				linePosition = 0;
			}
			if (line == null && !endIsOK()) {
				throw new JSONLDProcessingError("Error while parsing Turtle; unexpected end of input.")
					.setType(JSONLDProcessingError.Error.PARSE_ERROR)
					.setDetail("line", lineNumber)
					.setDetail("position", linePosition);
			} else if (line != null) {
				clearInitialWhitespace();
			}
		}
		
		private boolean endIsOK() {
			return curSubject == null && stack.size() == 0;
		}

		private String clearInitialWhitespace() throws JSONLDProcessingError {
			// clear starting whitespace
			Matcher match = Regex.IWSO.matcher(line);
			if (match.find()) {
				advanceLinePosition(match.group(0).length());
			}
			return line;
		}

		public String expandIRI(String ns, String name) {
			if (namespaces.containsKey(ns)) {
				return namespaces.get(ns) + name;
			} else {
				// we don't have a match, just rejoin the original name
				// TODO: should we throw an exception here instead?
				return ns + ":" + name;
			}
		}
	}
	
	@Override
	public RDFDataset parse(Object input)
			throws JSONLDProcessingError {
		if (!(input instanceof String)) {
			throw new JSONLDProcessingError("Invalid input; Triple RDF Parser requires a string input");
		}
		final RDFDataset result = new RDFDataset();
		final String graphName = "@default";
		boolean donewithprefixes = false;
		State state = new State((String)input);
		
		while (state.line != null) {
			
			// parse prefix
			if (!donewithprefixes) {
				Matcher match = Regex.TTL_PREFIX_ID.matcher(state.line);
				if (match.matches()) {
					String ns = match.group(1);
					String iri = match.group(2);
					state.namespaces.put(ns, iri);
					result.setNamespace(ns, iri);
					state.advanceLinePosition(match.group(0).length());
					continue;
				} else {
					// TODO: this assumes all @prefix lines must be at the top of the document
					donewithprefixes = true;
				}
			}
			
			Matcher match;
			
			if (state.curSubject == null) {
				// we need to match a subject
				match = Regex.TTL_SUBJECT.matcher(state.line);
				if (match.find()) {
					String iri;
					if (match.group(1) != null) {
						// matched NS:NAME
						String ns = match.group(1);
						String name = match.group(2);
						iri = state.expandIRI(ns, name);
					} else if (match.group(4) != null) {
						// matched IRI
						iri = match.group(4);
					} else {
						// matched BNODE
						iri = match.group(0).trim();
					}
					state.curSubject = iri;
					state.advanceLinePosition(match.group(0).length());
				} else if (state.line.startsWith("[")) {
					String bnode = "_:b" + (++state.bnodes);
					state.advanceLinePosition(1);
					state.push();
					state.curSubject = bnode;
				}
				// make sure we have a subject already
				else {
					throw new JSONLDProcessingError("Error while parsing Turtle; missing expected subject.")
						.setType(JSONLDProcessingError.Error.PARSE_ERROR)
						.setDetail("line", state.lineNumber)
						.setDetail("position", state.linePosition);
				}
			}
			
			if (state.curPredicate == null) {
				// match predicate
				match = Regex.TTL_PREDICATE.matcher(state.line);
				if (match.find()) {
					String iri = "";
					if (match.group(1) != null) {
						// matched NS:NAME
						String ns = match.group(1);
						String name = match.group(2);
						iri = state.expandIRI(ns, name);
					} else if (match.group(3) != null) {
						// matched IRI
						iri = match.group(3);
					}
					state.curPredicate = iri;
					state.advanceLinePosition(match.group(0).length());
				} else if (Pattern.matches("^a(?:[ \\t]+.*|[ \\t]*" + Regex.EOLN + ")$", state.line)) {
					state.curPredicate = RDF_TYPE;
					state.advanceLinePosition(1);
				} else {
					throw new JSONLDProcessingError("Error while parsing Turtle; missing expected predicate.")
						.setType(JSONLDProcessingError.Error.PARSE_ERROR)
						.setDetail("line", state.lineNumber)
						.setDetail("position", state.linePosition);
				}
			}
			
			// expecting bnode or object
			
			// match BNODE values
			if (state.line.startsWith("[")) {
				String bnode = "_:b" + (++state.bnodes);
				result.addTriple(state.curSubject, state.curPredicate, bnode);
				state.advanceLinePosition(1);
				state.push();
				state.curSubject = bnode;
				// next we expect a predicate
				continue;
			}
			
			// match object
			match = Regex.TTL_OBJECT.matcher(state.line);
			if (match.find()) {
				String iri = null;
				if (match.group(1) != null) {
					// matched NS:NAME
					String ns = match.group(1);
					String name = match.group(2);
					iri = state.expandIRI(ns, name);
				} else if (match.group(4) != null) {
					// matched IRI
					iri = match.group(4);
				} else if(match.group(5) != null) {
					// matched BNODE
					iri = match.group(0).trim();
				}
				if (iri != null) {
					// we have a object
					result.addTriple(state.curSubject, state.curPredicate, iri);
				} else {
					// we have a literal
					result.addTriple(state.curSubject, state.curPredicate, 
									match.group(6), 
									match.group(7) != null ? state.expandIRI(match.group(7), match.group(8)) : match.group(9), 
									match.group(10));
				}
				state.advanceLinePosition(match.group(0).length());
				// some hacky stuff for the case where there is no whitespace between ns:name elements and the delimiters
				// making sure that name can still contain .'s
				if (match.group(3) != null && !"".equals(match.group(3))) {
					state.linePosition--;
					state.line = match.group(3) + state.line;
				}
			} else {
				throw new JSONLDProcessingError("Error while parsing Turtle; missing expected object or blank node.")
					.setType(JSONLDProcessingError.Error.PARSE_ERROR)
					.setDetail("line", state.lineNumber)
					.setDetail("position", state.linePosition);
			}
			
			// match end of bnode
			if (state.line.startsWith("]")) {
				state.pop();
				state.advanceLinePosition(1);
			}
			
			// match list separator
			if (state.line.startsWith(",")) {
				state.advanceLinePosition(1);
				// now we expect another object/bnode
				continue;
			}
			
			// match predicate end
			if (state.line.startsWith(";")) {
				state.curPredicate = null;
				state.advanceLinePosition(1);
				// now we expect another predicate
				continue;
			}
			
			if (state.line.startsWith(".")) {
				if (state.expectingBnodeClose ) {
					throw new JSONLDProcessingError("Error while parsing Turtle; missing expected \"]\".")
						.setType(JSONLDProcessingError.Error.PARSE_ERROR)
						.setDetail("line", state.lineNumber)
						.setDetail("position", state.linePosition);
				}
				state.curSubject = null;
				state.curPredicate = null;
				state.advanceLinePosition(1);
				// this can now be the end of the document.
				continue;
			}
			
			// if we get here, we're missing a close statement
			throw new JSONLDProcessingError("Error while parsing Turtle; missing expected \"]\" \",\" \";\" or \".\".")
				.setType(JSONLDProcessingError.Error.PARSE_ERROR)
				.setDetail("line", state.lineNumber)
				.setDetail("position", state.linePosition);
		}
		
		return result;
	}

}
