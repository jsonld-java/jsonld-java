package com.github.jsonldjava.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A helper class which still stores all the values in a map
 * but gives member variables easily access certain keys 
 * 
 * @author tristan
 *
 */
public class ActiveContext extends HashMap<String, Object> {
    public ActiveContext() {
    	super();
    	init();
    }
    
    public ActiveContext(Map<String,Object> copy) {
    	super(copy);
    	init();
    }
    
    private void init() {
    	if (!this.containsKey("mappings")) {
    		this.put("mappings", new HashMap<String,Object>());
    	}
    	if (!this.containsKey("keywords")) {
    		this.put("keywords", new HashMap<String, List<String>>() {
                {
                    put("@context", new ArrayList<String>());
                    put("@container", new ArrayList<String>());
                    put("@default", new ArrayList<String>());
                    put("@embed", new ArrayList<String>());
                    put("@explicit", new ArrayList<String>());
                    put("@graph", new ArrayList<String>());
                    put("@id", new ArrayList<String>());
                    put("@language", new ArrayList<String>());
                    put("@list", new ArrayList<String>());
                    put("@omitDefault", new ArrayList<String>());
                    put("@preserve", new ArrayList<String>());
                    put("@set", new ArrayList<String>());
                    put("@type", new ArrayList<String>());
                    put("@value", new ArrayList<String>());
                    put("@vocab", new ArrayList<String>());
                    // add ignored keywords
                    /*
                    for (String key : ignoredKeywords) {
                        put(keyword, new ArrayList<String>());
                    }
                    */
                }
    		});
    	}
    	mappings = (Map<String, Object>) this.get("mappings");
    	keywords = (Map<String, List<String>>) this.get("keywords");
    }

    public Object getContextValue(String key, String type) {
        if (key == null) {
            return null;
        }
        Object rval = null;
        if ("@language".equals(type) && this.containsKey(type)) {
            rval = this.get(type);
        }

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

    public Map<String, Object> mappings;
    public Map<String, List<String>> keywords;
}