package com.github.jsonldjava.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.jsonldjava.utils.URL;

/**
 * A helper class which still stores all the values in a map
 * but gives member variables easily access certain keys 
 * 
 * @author tristan
 *
 */
public class ActiveContext extends HashMap<String, Object> {
    public ActiveContext() {
    	this(new Options());
    }
    
    public ActiveContext(Options options) {
    	super();
    	init(options);
    }
    
    private void init(Options options) {
    	Object base = URL.parse(options.base);
    	
    	this.put("mappings", new HashMap<String,Object>());
    	
    	this.put("@base", base);
    	mappings = (Map<String, Object>) this.get("mappings");

    }

    public Object getContextValue(String key, String type) {
    	
    	// return null for invalid key
        if (key == null) {
            return null;
        }
        
        Object rval = null;
        
        // get default language
        if ("@language".equals(type) && this.containsKey(type)) {
            rval = this.get(type);
        }

        // get specific entry information
        if (this.mappings.containsKey(key)) {
            Map<String, Object> entry = (Map<String, Object>) this.mappings.get(key);

            if (type == null) {
                rval = entry;
            } else if (entry.containsKey(type)) {
                rval = entry.get(type);
            }
        }

        return rval;
    }
    
    public ActiveContext clone() {
    	return (ActiveContext) super.clone();
    }

    public Map<String, Object> mappings;
    
    // TODO: remove this when it's not needed by old code
    public Map<String, List<String>> keywords;
}