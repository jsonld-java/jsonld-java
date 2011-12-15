package de.dfki.km.json.jsonld;

public interface JSONLDProcessor {
	public Object expand (Object input); // TODO: optional JSONLDProcessorCallback? callback
	public Object compact (Object input, Object context);
	public Object frame (Object input, Object frame);
	public Object frame (Object input, Object frame, Object options);
	public Object normalize (Object input);
	public Object triples (Object input, JSONLDTripleCallback tripleCallback);
	
}
