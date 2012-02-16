package de.dfki.km.json.jsonld;

public interface JSONLDProcessor {
	public Object expand (Object input); // TODO: optional JSONLDProcessorCallback? callback
	public Object compact (Object context, Object input);
	public Object frame (Object input, Object frame);
	public Object frame (Object input, Object frame, Object options);
	public Object normalize (Object input);
	public void triples (Object input, JSONLDTripleCallback tripleCallback);
	
}
