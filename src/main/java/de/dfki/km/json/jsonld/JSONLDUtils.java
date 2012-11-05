package de.dfki.km.json.jsonld;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.codehaus.jackson.JsonParseException;

import de.dfki.km.json.JSONUtils;

public class JSONLDUtils {

    private static final int MAX_CONTEXT_URLS = 10;

	/**
     * Returns whether or not the given value is a keyword (or a keyword alias).
     *
     * @param v the value to check.
     * @param [ctx] the active context to check against.
     *
     * @return true if the value is a keyword, false if not.
     */
    public static boolean isKeyword(String key) {
        // TODO: this doesn't fit with my desire to have this list modifyable at runtime
        // I may need to make this a method of JSONLDProcessor to support this
        // which may result in a lot of the utils in this library becoming member functions
        return "@context".equals(key) || "@container".equals(key) || "@default".equals(key) || "@embed".equals(key) || "@explicit".equals(key)
                || "@graph".equals(key) || "@id".equals(key) || "@language".equals(key) || "@list".equals(key) || "@omitDefault".equals(key)
                || "@preserve".equals(key) || "@set".equals(key) || "@type".equals(key) || "@value".equals(key) || "@vocab".equals(key);
    }

    public static boolean isKeyword(String key, Map<String, Object> ctx) {
        if (ctx.containsKey("keywords")) {
            Map<String, List<String>> keywords = (Map<String, List<String>>) ctx.get("keywords");
            if (keywords.containsKey(key)) {
                return true;
            }
            for (List<String> aliases : keywords.values()) {
                if (aliases.contains(key)) {
                    return true;
                }
            }
        } else {
            throw new RuntimeException("Error: missing keywords map in context!");
        }
        return false;
    }

    public static boolean isAbsoluteIri(String value) {
        return value.contains(":");
    }

    /**
     * Adds a value to a subject. If the subject already has the value, it will
     * not be added. If the value is an array, all values in the array will be
     * added.
     *
     * Note: If the value is a subject that already exists as a property of the
     * given subject, this method makes no attempt to deeply merge properties.
     * Instead, the value will not be added.
     *
     * @param subject the subject to add the value to.
     * @param property the property that relates the value to the subject.
     * @param value the value to add.
     * @param [propertyIsArray] true if the property is always an array, false
     *          if not (default: false).
     * @param [allowDuplicate] true if the property is a @list, false
     *          if not (default: false).
     */
    public static void addValue(Map<String, Object> subject, String property, Object value, boolean propertyIsArray, boolean allowDuplicate) {
        if (value instanceof List) {
            if (((List) value).size() == 0 && propertyIsArray && !subject.containsKey(property)) {
                subject.put(property, new ArrayList<Object>());
            }
            for (Object val : (List) value) {
                addValue(subject, property, val, propertyIsArray, allowDuplicate);
            }
        } else if (subject.containsKey(property)) {
            boolean hasValue = !allowDuplicate && hasValue(subject, property, value);
            if (!(subject.get(property) instanceof List) && (!hasValue || propertyIsArray)) {
                List<Object> tmp = new ArrayList<Object>();
                tmp.add(subject.get(property));
                subject.put(property, tmp);
            }
            if (!hasValue) {
                ((List<Object>) subject.get(property)).add(value);
            }
        } else {
            Object tmp;
            if (propertyIsArray) {
                tmp = new ArrayList<Object>();
                ((List<Object>) tmp).add(value);
            } else {
                tmp = value;
            }
            subject.put(property, tmp);
        }
    }

    public static void addValue(Map<String, Object> subject, String property, Object value, boolean propertyIsArray) {
        addValue(subject, property, value, propertyIsArray, true);
    }

    public static void addValue(Map<String, Object> subject, String property, Object value) {
        addValue(subject, property, value, false, true);
    }

    /**
     * Determines if the given value is a property of the given subject.
     *
     * @param subject the subject to check.
     * @param property the property to check.
     * @param value the value to check.
     *
     * @return true if the value exists, false if not.
     */
    public static boolean hasValue(Map<String, Object> subject, String property, Object value) {
        boolean rval = false;
        if (hasProperty(subject, property)) {
            Object val = subject.get(property);
            boolean isList = (val instanceof Map && ((Map<String, Object>) val).containsKey("@list"));
            if (isList || val instanceof List) {
                if (isList) {
                    val = ((Map<String, Object>) val).get("@list");
                }
                for (Object i : (List) val) {
                    if (compareValues(value, i)) {
                        rval = true;
                        break;
                    }
                }
            } else if (!(value instanceof List)) {
                rval = compareValues(value, val);
            }
        }
        return rval;
    }

    private static boolean hasProperty(Map<String, Object> subject, String property) {
        boolean rval = false;
        if (subject.containsKey(property)) {
            Object value = subject.get(property);
            rval = (!(value instanceof List) || ((List) value).size() > 0);
        }
        return rval;
    }

    /**
     * Compares two JSON-LD values for equality. Two JSON-LD values will be
     * considered equal if:
     *
     * 1. They are both primitives of the same type and value.
     * 2. They are both @values with the same @value, @type, and @language, OR
     * 3. They both have @ids they are the same.
     *
     * @param v1 the first value.
     * @param v2 the second value.
     *
     * @return true if v1 and v2 are considered equal, false if not.
     */
    public static boolean compareValues(Object v1, Object v2) {
        if (v1.equals(v2)) {
            return true;
        }

        if ((v1 instanceof Map && ((Map<String, Object>) v1).containsKey("@value")) && (v2 instanceof Map && ((Map<String, Object>) v2).containsKey("@value"))
                && ((Map<String, Object>) v1).get("@value").equals(((Map<String, Object>) v2).get("@value"))
                && ((Map<String, Object>) v1).get("@type").equals(((Map<String, Object>) v2).get("@type"))
                && ((Map<String, Object>) v1).get("@language").equals(((Map<String, Object>) v2).get("@language"))) {
            return true;
        }

        if ((v1 instanceof Map && ((Map<String, Object>) v1).containsKey("@id")) && (v2 instanceof Map && ((Map<String, Object>) v2).containsKey("@id"))
                && ((Map<String, Object>) v1).get("@id").equals(((Map<String, Object>) v2).get("@id"))) {
            return true;
        }

        return false;
    }
    
    /**
     * Removes a value from a subject.
     *
     * @param subject the subject.
     * @param property the property that relates the value to the subject.
     * @param value the value to remove.
     * @param [options] the options to use:
     *          [propertyIsArray] true if the property is always an array, false
     *            if not (default: false).
     */
    public static void removeValue(Map<String, Object> subject, String property,
			Map<String, Object> value) {
		removeValue(subject, property, value, false);
	}
    public static void removeValue(Map<String, Object> subject, String property,
			Map<String, Object> value, boolean propertyIsArray) {
		// filter out value
    	List<Object> values = new ArrayList<Object>();
    	if (subject.get(property) instanceof List) {
    		for (Object e: ((List)subject.get(property))) {
    			if (!(value.equals(e))) {
    				values.add(value);
    			}
    		}
    	} else {
    		if (!value.equals(subject.get(property))) {
    			values.add(subject.get(property));
    		}
    	}
    	
    	if (values.size() == 0) {
    		subject.remove(property);
    	} else if (values.size() == 1 && !propertyIsArray) {
    		subject.put(property, values.get(0));
    	} else {
    		subject.put(property, values);
    	}
	}

    /**
     * Returns true if the given value is a blank node.
     *
     * @param v the value to check.
     *
     * @return true if the value is a blank node, false if not.
     */
    public static boolean isBlankNode(Object v) {
    	// Note: A value is a blank node if all of these hold true:
    	// 1. It is an Object.
    	// 2. If it has an @id key its value begins with '_:'.
    	// 3. It has no keys OR is not a @value, @set, or @list.
        if (v instanceof Map) {
        	if (((Map)v).containsKey("@id")) {
        		return ((String)((Map)v).get("@id")).startsWith("_:");
        	} else {
        		return ((Map)v).size() == 0 || !(((Map)v).containsKey("@value") || ((Map)v).containsKey("@set") || ((Map)v).containsKey("@list"));
        	}
        }
        return false;
    }
    
    /**
     * Returns true if the given value is a subject with properties.
     *
     * @param v the value to check.
     *
     * @return true if the value is a subject with properties, false if not.
     */
    public static boolean isSubject(Object v) {
    	// Note: A value is a subject if all of these hold true:
    	// 1. It is an Object.
    	// 2. It is not a @value, @set, or @list.
    	// 3. It has more than 1 key OR any existing key is not @id.
    	if (v instanceof Map && !(((Map) v).containsKey("@value") || ((Map) v).containsKey("@set") || ((Map) v).containsKey("@list"))) {
    		return ((Map<String, Object>) v).size() > 1 || !((Map) v).containsKey("@id"); 
    	}
    	return false;
    }
    
    /**
     * Returns true if the given value is a subject reference.
     *
     * @param v the value to check.
     *
     * @return true if the value is a subject reference, false if not.
     */
    public static boolean isSubjectReference(Object v) {
    	// Note: A value is a subject reference if all of these hold true:
    	// 1. It is an Object.
    	// 2. It has a single key: @id.
    	return (v instanceof Map && ((Map<String, Object>) v).size() == 1 && ((Map<String, Object>) v).containsKey("@id"));
    }
    
    /**
     * Resolves external @context URLs using the given URL resolver. Each
     * instance of @context in the input that refers to a URL will be replaced
     * with the JSON @context found at that URL.
     *
     * @param input the JSON-LD input with possible contexts.
     * @param resolver(url, callback(err, jsonCtx)) the URL resolver to use.
     * @param callback(err, input) called once the operation completes.
     * @throws JSONLDProcessingError 
     */
    public static void resolveContextUrls(Object input) throws JSONLDProcessingError {
    	resolve(input, new HashMap<String, Object>());
	}
    
    private static void resolve(Object input, Map<String, Object> cycles) throws JSONLDProcessingError {
    	Pattern regex = Pattern.compile("(http|https)://(\\w+:{0,1}\\w*@)?(\\S+)(:[0-9]+)?(/|/([\\w#!:.?+=&%@!\\-/]))?");
    	
    	if (cycles.size() > MAX_CONTEXT_URLS) {
    		throw new JSONLDProcessingError("Maximum number of @context URLs exceeded.")
    			.setType(JSONLDProcessingError.Error.CONTEXT_URL_ERROR)
    			.setDetail("max", MAX_CONTEXT_URLS);
    	}
    	
    	// for tracking the URLs to resolve
    	Map<String,Object> urls = new HashMap<String, Object>();
    	
    	// find all URLs in the given input
    	if (!findContextUrls(input, urls, false)) {
    		// finished
    		findContextUrls(input, urls, true);
    	}
    	
    	// queue all unresolved URLs
    	List<String> queue = new ArrayList<String>();
    	for (String url: urls.keySet()) {
    		if (Boolean.FALSE.equals((Boolean)urls.get(url))) {
    			// validate URL
    			if (!regex.matcher(url).matches()) {
    				throw new JSONLDProcessingError("Malformed URL.")
    					.setType(JSONLDProcessingError.Error.INVALID_URL)
    					.setDetail("url", url);
    			}
    			queue.add(url);
    		}
    	}
    	
    	// resolve URLs in queue
    	int count = queue.size();
    	for (String url: queue) {
    		// check for context URL cycle
    		if (cycles.containsKey(url)) {
    			throw new JSONLDProcessingError("Cyclical @context URLs detected.")
    				.setType(JSONLDProcessingError.Error.CONTEXT_URL_ERROR)
    				.setDetail("url", url);
    		}
    		Map<String, Object> _cycles = (Map<String, Object>) clone(cycles);
    		_cycles.put(url, Boolean.TRUE);
    		
    		try {
				Map<String,Object> ctx = (Map<String,Object>)JSONUtils.fromString((String)new URL(url).getContent());
				if (!ctx.containsKey("@context")) {
					ctx = new HashMap<String, Object>();
					ctx.put("@context", new HashMap<String, Object>());
				}
				resolve(ctx, _cycles);
				urls.put(url, ctx.get("@context"));
				count -= 1;
				if (count == 0) {
					findContextUrls(input, urls, true);
				}
			} catch (JsonParseException e) {
				throw new JSONLDProcessingError("URL does not resolve to a valid JSON-LD object.")
					.setType(JSONLDProcessingError.Error.INVALID_URL)
					.setDetail("url", url);
			} catch (MalformedURLException e) {
				throw new JSONLDProcessingError("Malformed URL.")
					.setType(JSONLDProcessingError.Error.INVALID_URL)
					.setDetail("url", url);
			} catch (IOException e) {
				throw new JSONLDProcessingError("Unable to open URL.")
					.setType(JSONLDProcessingError.Error.INVALID_URL)
					.setDetail("url", url);
			}
    	}
		
	}

	/**
     * Finds all @context URLs in the given JSON-LD input.
     *
     * @param input the JSON-LD input.
     * @param urls a map of URLs (url => false/@contexts).
     * @param replace true to replace the URLs in the given input with the
     *           @contexts from the urls map, false not to.
     *
     * @return true if new URLs to resolve were found, false if not.
     */
    private static boolean findContextUrls(Object input,
			Map<String, Object> urls, Boolean replace) {
		int count = urls.size();
		if (input instanceof List) {
			for (Object i: (List)input) {
				findContextUrls(i, urls, replace);
			}
			return count < urls.size();
		} else if (input instanceof Map) {
			for (String key: ((Map<String,Object>)input).keySet()) {
				if (!"@context".equals(key)) {
					findContextUrls(((Map) input).get(key), urls, replace);
					continue;
				}
				
				// get @context
				Object ctx = ((Map) input).get(key);
				
				// array @context
				if (ctx instanceof List) {
					int length = ((List) ctx).size();
					for (int i = 0; i < length; i++) {
						Object _ctx = ((List) ctx).get(i);
						if (_ctx instanceof String) {
							// replace w/@context if requested
							if (replace) {
								_ctx = urls.get(_ctx);
								if (_ctx instanceof List) {
									// add flattened context
									((List)ctx).remove(i);
									((List)ctx).addAll((Collection) _ctx);
									i += ((List) _ctx).size();
									length += ((List) _ctx).size();
								} else {
									((List)ctx).set(i, _ctx);
								}
							}
							// @context URL found
							else if (!urls.containsKey(_ctx)) {
								urls.put((String)_ctx, Boolean.FALSE);
							}
						}
					}
				}
				// string @context
				else if (ctx instanceof String) {
					// replace w/@context if requested
					if (replace) {
						((Map) input).put(key, urls.get((String)ctx));
					}
					// @context URL found
					else if (!urls.containsKey(ctx)) {
						urls.put((String)ctx, Boolean.FALSE);
					}
				}
			}
			return (count < urls.size());
		}
		return false;
	}

	// END OF NEW CODE

    public static Object clone(Object value) {// throws
    	// CloneNotSupportedException {
    	Object rval = null;
    	if (value instanceof Cloneable) {
    		try {
    			rval = value.getClass().getMethod("clone").invoke(value);
    		} catch (Exception e) {
    			rval = e;
    		}
    	}
    	if (rval == null || rval instanceof Exception) {
    		// the object wasn't cloneable, or an error occured
    		if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
    			// strings numbers and booleans are immutable
    			rval = value;
    		} else {
    			// TODO: making this throw runtime exception so it doesn't have
    			// to be caught
    			// because simply it should never fail in the case of JSON-LD
    			// and means that
    			// the input JSON-LD is invalid
    			throw new RuntimeException(new CloneNotSupportedException((rval instanceof Exception ? ((Exception) rval).getMessage() : "")));
    		}
    	}
    	return rval;
    }

    public static int compare(Object v1, Object v2) {
    	int rval = 0;

    	if (v1 instanceof List && v2 instanceof List) {
    		if (((List) v1).size() != ((List) v2).size()) {
    			rval = 1;
    		} else {
    			// TODO: should the order of things in the list matter?
    			for (int i = 0; i < ((List<Object>) v1).size() && rval == 0; i++) {
    				rval = compare(((List<Object>) v1).get(i), ((List<Object>) v2).get(i));
    			}
    		}
    	} else if (v1 instanceof Number && v2 instanceof Number) {
    		// TODO: this is VERY sketchy
    		double n1 = ((Number) v1).doubleValue();
    		double n2 = ((Number) v2).doubleValue();

    		rval = (n1 < n2 ? -1 : (n1 > n2 ? 1 : 0));
    	} else if (v1 instanceof String && v2 instanceof String) {
    		rval = ((String) v1).compareTo((String) v2);
    		if (rval > 1)
    			rval = 1;
    		else if (rval < -1)
    			rval = -1;
    	} else if (v1 instanceof Map && v2 instanceof Map) {
    		throw new RuntimeException("I don't know how I should handle this case yet!");
    		/*
    		 * TODO: not sure what to do here exactly...
    		 * 
    		 * python can compare objects using the < and > operators. js pretends it can (i.e. it doesn't throw an error) but always returns false. thus the js
    		 * code and the py code are inconsistant.
    		 * 
    		 * // TODO: this assumes the order of keys doesn't matter if (((Map) v1).size() != ((Map) v2).size() ) { rval = 1; } else { if (((Map) v1).size() !=
    		 * ((Map) v2).size()) { rval = 1; } else { for (Object k1: ((Map) v1).keySet()) { rval = ((Map) v2).containsKey(k1) ? compare(((Map) v1).get(k1),
    		 * ((Map) v2).get(k1)) : 1; if (rval != 0) { break; } } } } } else if (v1 instanceof Boolean && v2 instanceof Boolean) { //rval = (v1 == v2 ? 0 :
    		 * 1);
    		 */
    	} else {
    		// TODO: this is probably something I don't want to allow either
    		throw new RuntimeException("compare unspecified for these objects");
    		// rval = (v1.equals(v2) ? 0 : 1);
    	}
    	return rval;
    }
}