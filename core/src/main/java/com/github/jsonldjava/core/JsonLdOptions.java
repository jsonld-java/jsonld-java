package com.github.jsonldjava.core;

/**
 * http://json-ld.org/spec/latest/json-ld-api/#the-jsonldoptions-type
 * 
 * @author tristan
 *
 */
public class JsonLdOptions {
    public JsonLdOptions() {
        this.setBase("");
    }

    public JsonLdOptions(String base) {
        this.setBase(base);
    }

    @Override
    public JsonLdOptions clone() {
        final JsonLdOptions rval = new JsonLdOptions(getBase());
        return rval;
    }
    
    // base options
    
    private String base = null;
    private Boolean compactArrays = true;
    private Object expandContext = null;
    private String processingMode = "json-ld-1.0";
    
    // frame options
    
    private Boolean embed = null;
    private Boolean explicit = null;
    private Boolean omitDefault = null;
    
    // rdf conversion options
    Boolean useRdfType = false;
    Boolean useNativeTypes = false;
	private boolean produceGeneralizedRdf = false;

 
    public Boolean getEmbed() {
		return embed;
	}

	public void setEmbed(Boolean embed) {
		this.embed = embed;
	}

	public Boolean getExplicit() {
		return explicit;
	}

	public void setExplicit(Boolean explicit) {
		this.explicit = explicit;
	}

	public Boolean getOmitDefault() {
		return omitDefault;
	}

	public void setOmitDefault(Boolean omitDefault) {
		this.omitDefault = omitDefault;
	}

	public Boolean getCompactArrays() {
		return compactArrays;
	}

	public void setCompactArrays(Boolean compactArrays) {
		this.compactArrays = compactArrays;
	}

	public Object getExpandContext() {
		return expandContext;
	}

	public void setExpandContext(Object expandContext) {
		this.expandContext = expandContext;
	}

	public String getProcessingMode() {
		return processingMode;
	}

	public void setProcessingMode(String processingMode) {
		this.processingMode = processingMode;
	}

	public String getBase() {
		return base;
	}

	public void setBase(String base) {
		this.base = base;
	}
	
	
	public Boolean getUseRdfType() {
		return useRdfType;
	}

	public void setUseRdfType(Boolean useRdfType) {
		this.useRdfType = useRdfType;
	}

	public Boolean getUseNativeTypes() {
		return useNativeTypes;
	}

	public void setUseNativeTypes(Boolean useNativeTypes) {
		this.useNativeTypes = useNativeTypes;
	}


	public boolean getProduceGeneralizedRdf() {
		// TODO Auto-generated method stub
		return this.produceGeneralizedRdf ;
	}

	public void setProduceGeneralizedRdf(Boolean produceGeneralizedRdf) {
		this.produceGeneralizedRdf = produceGeneralizedRdf;
	}
	
	// TODO: THE FOLLOWING ONLY EXIST SO I DON'T HAVE TO DELETE A LOT OF CODE, REMOVE IT WHEN DONE
	public String format = null;
	public Boolean useNamespaces = false;
	public String outputForm = null;
	public DocumentLoader documentLoader;
}
