package com.github.jsonldjava.core;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jsonldjava.utils.JSONUtils;
import com.github.jsonldjava.utils.Obj;
import com.github.jsonldjava.utils.URL;

import static com.github.jsonldjava.core.JSONLDConsts.*;
import static com.github.jsonldjava.core.JSONLDUtils.*;
import static com.github.jsonldjava.core.ToRDFUtils.*;

public class JSONLDProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(JSONLDProcessor.class);
	
    Options opts;

    public JSONLDProcessor() {
        opts = new Options("");
    }

    public JSONLDProcessor(Options opts) {
        if (opts == null) {
            opts = new Options("");
        } else {
            this.opts = opts;
        }
    }
    
    /**
     * Defines a context mapping during context processing.
     *
     * @param activeCtx the current active context.
     * @param ctx the local context being processed.
     * @param key the key in the local context to define the mapping for.
     * @param base the base IRI.
     * @param defined a map of defining/defined keys to detect cycles and prevent
     *          double definitions.
     * @throws JSONLDProcessingError 
     */
    private void defineContextMapping(ActiveContext activeCtx, Map<String, Object> ctx, String key, String base, Map<String, Boolean> defined) throws JSONLDProcessingError {
        if (defined.containsKey(key)) {
        	// key already defined
            if (defined.get(key) == Boolean.TRUE) {
                return;
            }
            // cycle detected
            throw new JSONLDProcessingError("Cyclical context definition detected")
            	.setDetail("context", ctx)
            	.setDetail("key", key);
        }
        // now defining key
        defined.put(key, Boolean.FALSE);

        // if key has a prefix, define it first
        String prefix = null;
        int colon = key.indexOf(":");
        if (colon != -1) {
            prefix = key.substring(0, colon);
            if (ctx.containsKey(prefix)) {
            	// define parent prefix
                defineContextMapping(activeCtx, ctx, prefix, base, defined);
            }
        }

        // get context key value
        Object value = ctx.get(key);

        if (JSONLDUtils.isKeyword(key)) {
        	
        	// support @vocab
            if ("@vocab".equals(key)) {
            	if (!value.equals(null) && !(value instanceof String)) {
            		throw new JSONLDProcessingError("Invalid JSON-LD syntax; the value of \"@vocab\" in a @context must be a string or null.")
                		.setType(JSONLDProcessingError.Error.SYNTAX_ERROR)
                		.setDetail("context", ctx);
            	}
            	if (((String)value).indexOf(":") == -1) {
            		throw new JSONLDProcessingError("Invalid JSON-LD syntax; the value of \"@vocab\" in a @context must be an absolute IRI.")
            		.setType(JSONLDProcessingError.Error.SYNTAX_ERROR)
            		.setDetail("context", ctx);
            	}
            	if (value == null) {
            		activeCtx.remove("@vocab");
            	} else {
            		activeCtx.put("@vocab", value);
            	}
            	defined.put(key, Boolean.TRUE);
            	return;
            }
        	
            // only @language is permitted
            if (!"@language".equals(key)) {
                throw new JSONLDProcessingError("Invalid JSON-LD syntax; keywords cannot be overridden")
                	.setType(JSONLDProcessingError.Error.SYNTAX_ERROR)
                	.setDetail("context", ctx);
            }
            if (!value.equals(null) && !(value instanceof String)) {
                throw new JSONLDProcessingError("Invalid JSON-LD syntax; the value of \"@language\" in a @context must be a string or null")
                	.setType(JSONLDProcessingError.Error.SYNTAX_ERROR)
                	.setDetail("context", ctx);
            }
            if (value == null) {
                activeCtx.remove("@language");
            } else {
                activeCtx.put("@language", value);
            }
            defined.put(key, Boolean.TRUE);
            return;
        }

        // clear context entry
        if (value == null || (value instanceof Map && ((Map<String, Object>) value).containsKey("@id") && ((Map<String, Object>) value).get("@id") == null)) {
            if (activeCtx.mappings.containsKey(key)) {
            	// if key is a keyword alias, remove it
                String kw = (String) ((Map<String, Object>) activeCtx.mappings.get(key)).get("@id");
                if (JSONLDUtils.isKeyword(kw)) {
                    List<String> aliases = activeCtx.keywords.get(kw);
                    aliases.remove(key);
                }
                activeCtx.mappings.remove(key);
            }
            defined.put(key, Boolean.TRUE);
            return;
        }

        if (value instanceof String) {
            if (JSONLDUtils.isKeyword((String) value)) {
            	// disallow aliasing @context and @preserve
                if ("@context".equals(value) || "@preserve".equals(value)) {
                    throw new JSONLDProcessingError("Invalid JSON-LD syntax; @context and @preserve cannot be aliased")
                    	.setType(JSONLDProcessingError.Error.SYNTAX_ERROR);
                }
                // uniquely add key as a keyword alias and resort
                List<String> aliases = activeCtx.keywords.get(value);
                if (!aliases.contains(key)) {
                    aliases.add(key);
                    Collections.sort(aliases, new Comparator<String>() {
                        // Compares two strings first based on length and then lexicographically
                        public int compare(String a, String b) {
                            if (a.length() < b.length()) {
                                return -1;
                            } else if (b.length() < a.length()) {
                                return 1;
                            }
                            return a.compareTo(b);
                        }
                    });
                }
            } else {
            	// expand value to a full IRI
                value = expandContextIri(activeCtx, ctx, (String) value, base, defined);
            }

            // define/redefine key to expanded IRI/keyword
            Map<String, Object> tmp = new HashMap<String, Object>();
            tmp.put("@id", value);
            activeCtx.mappings.put(key, tmp);
            defined.put(key, Boolean.TRUE);
            return;
        }

        if (!(value instanceof Map)) {
            throw new JSONLDProcessingError("Invalid JSON-LD syntax; @context property values must be strings or objects.")
            	.setType(JSONLDProcessingError.Error.SYNTAX_ERROR)
            	.setDetail("context", ctx);
        }
        // create new mapping
        Map<String, Object> mapping = new HashMap<String, Object>();

        // helper to make accessing the value as a map easier
        Map<String, Object> val = (Map<String, Object>) value;
        
        if (val.containsKey("@id")) {
            if (!(val.get("@id") instanceof String)) {
                throw new JSONLDProcessingError("Invalid JSON-LD syntax; @context @id values must be strings.")
                	.setType(JSONLDProcessingError.Error.SYNTAX_ERROR)
                	.setDetail("context", ctx);
            }
            String id = (String) val.get("@id");

            // expand @id if it is not @type
            if (!"@type".equals(id)) {
            	// expand @id to full IRI
                id = expandContextIri(activeCtx, ctx, id, base, defined);
            }
            
            // add @id to mapping
            mapping.put("@id", id);
        } else if (activeCtx.containsKey("@vocab") && activeCtx.get("@vocab") != null) {
        	// NOTE: this is not implemented in javascript (which actually fails tests that need it)
        	String id = activeCtx.get("@vocab") + key;
        	mapping.put("@id", id);
        } else {
            // non-IRIs *must* define @ids
            if (prefix == null) {
                throw new JSONLDProcessingError("Invalid JSON-LD syntax; @context terms must define an @id.")
                	.setType(JSONLDProcessingError.Error.SYNTAX_ERROR)
                	.setDetail("context", ctx)
                	.setDetail("key", val);
            }

            if (activeCtx.mappings.containsKey(prefix)) {
                String suffix = key.substring(colon + 1);
                mapping.put("@id", (String) ((Map<String, Object>) activeCtx.mappings.get(prefix)).get("@id") + suffix);
            } else {
                mapping.put("@id", key);
            }
        }

        if (val.containsKey("@type")) {
            if (!(val.get("@type") instanceof String)) {
                throw new JSONLDProcessingError("Invalid JSON-LD syntax; @context @type values must be strings.")
                	.setType(JSONLDProcessingError.Error.SYNTAX_ERROR)
                	.setDetail("context", ctx);
            }
            String type = (String) val.get("@type");
            if (!"@id".equals(type)) {
            	// expand @type to full IRI
                type = expandContextIri(activeCtx, ctx, type, null, defined);
            }
            // add @type to mapping
            mapping.put("@type", type);
        }

        if (val.containsKey("@container")) {
            Object container = val.get("@container");
            if (!("@list".equals(container) || "@set".equals(container) || "@language".equals(container))) {
                throw new JSONLDProcessingError("Invalid JSON-LD syntax; @context @container value must be \"@list\" or \"@set\".")
                	.setType(JSONLDProcessingError.Error.SYNTAX_ERROR)
                	.setDetail("context", ctx);
            }
            // add @container to mapping
            mapping.put("@container", container);
        }

        if (val.containsKey("@language")) {
            Object lang = val.get("@language");
            if (lang != null && !(lang instanceof String)) {
                throw new JSONLDProcessingError("Invalid JSON-LD syntax; @context @language must be a string or null.")
                	.setType(JSONLDProcessingError.Error.SYNTAX_ERROR)
                	.setDetail("context", ctx);
            }
            // add @language to mapping
            mapping.put("@language", lang);
        }

        // merge onto parent mapping if one exists for a prefix
        if (prefix != null && activeCtx.mappings.containsKey(prefix)) {
            Map<String, Object> child = mapping;
            mapping = (Map<String, Object>) JSONLDUtils.clone(activeCtx.mappings.get(prefix));
            for (String k : child.keySet()) {
                mapping.put(k, child.get(k));
            }
        }

        // define key mapping
        activeCtx.mappings.put(key, mapping);
        defined.put(key, Boolean.TRUE);
    }

    /**
     * Expands a string value to a full IRI during context processing. It can
     * be assumed that the value is not a keyword.
     *
     * @param activeCtx the current active context.
     * @param ctx the local context being processed.
     * @param value the string value to expand.
     * @param base the base IRI.
     * @param defined a map for tracking cycles in context definitions.
     *
     * @return the expanded value.
     * @throws JSONLDProcessingError 
     */
    private String expandContextIri(ActiveContext activeCtx, Map<String, Object> ctx, String value, String base, Map<String, Boolean> defined) throws JSONLDProcessingError {
    	// dependency not defined, define it
        if (ctx.containsKey(value) && defined.get(value) != Boolean.TRUE) {
            defineContextMapping(activeCtx, ctx, value, base, defined);
        }

        // recurse if value is a term
        if (activeCtx.mappings.containsKey(value)) {
            String id = ((Map<String, String>) activeCtx.mappings.get(value)).get("@id");
            // value is already an absolute IRI
            if (id != null && id.equals(value)) {
                return value;
            }
            return expandContextIri(activeCtx, ctx, id, base, defined);
        }

        // split value into prefix:suffix
        int colon = value.indexOf(':');
        if (colon != -1) {
            String prefix = value.substring(0, colon);
            String suffix = value.substring(colon + 1);

            // indicates the value is a blank node
            if ("_".equals(prefix)) {
                return value;
            }

            // indicates the value is an absolute IRI
            if (suffix.startsWith("//")) {
                return value;
            }

            // dependency not defined, define it
            if (ctx.containsKey(prefix) && defined.get(prefix) != Boolean.TRUE) {
                defineContextMapping(activeCtx, ctx, prefix, base, defined);
            }

            // recurse if prefix is defined
            if (activeCtx.mappings.containsKey(prefix)) {
                String id = ((Map<String, String>) activeCtx.mappings.get(prefix)).get("@id");
                return expandContextIri(activeCtx, ctx, id, base, defined) + suffix;
            }

            // consider the value an absolute IRI
            return value;
        }
        
        if (JSONLDUtils.isKeyword(value)) {
        	
        }
        // prepend vocab
        else if (base == null && ctx.containsKey("@vocab")) {
        	value = prependBaseAndnormalizeURI((String)ctx.get("@vocab"), value);
        } else if (base != null && activeCtx.containsKey("@vocab")) {
        	// NOTE: this fulfills the case where @vocab is in the root of the active content
        	// which from the expected results of test compact-0021 is required to be used
        	// over the value of base
        	value = prependBaseAndnormalizeURI((String)activeCtx.get("@vocab"), value);
        } else if (base != null) {
        	value = prependBaseAndnormalizeURI(base, value);
        }

        // value must now be an absolute IRI
        if (!JSONLDUtils.isAbsoluteIri(value)) {
            throw new JSONLDProcessingError("Invalid JSON-LD syntax; a @context value does not expand to an absolute IRI.")
            	.setType(JSONLDProcessingError.Error.SYNTAX_ERROR)
            	.setDetail("context", ctx)
            	.setDetail("value", value);
        }

        return value;
    }

    /**
     * Processes a local context and returns a new active context.
     *
     * @param activeCtx the current active context.
     * @param localCtx the local context to process.
     * @param options the context processing options.
     *
     * @return the new active context.
     */
    public ActiveContext processContext(ActiveContext activeCtx, Object localCtx) throws JSONLDProcessingError {
    	
    	// TODO: get context from cache if available
    	
    	// initialize the resulting context
        ActiveContext rval = activeCtx.clone();

        // normalize local context to an array of @context objects
        if (localCtx instanceof Map && ((Map) localCtx).containsKey("@context") && ((Map) localCtx).get("@context") instanceof List) {
            localCtx = ((Map) localCtx).get("@context");
        }

        List<Map<String, Object>> ctxs;
        if (localCtx instanceof List) {
            ctxs = (List<Map<String, Object>>) localCtx;
        } else {
            ctxs = new ArrayList<Map<String, Object>>();
            ctxs.add((Map<String, Object>) localCtx);
        }

        // process each context in order
        for (Object ctx : ctxs) {
        	if (ctx == null) {
        		// reset to initial context
        		rval = new ActiveContext(opts);
        		continue;
            }
        	
        	// context must be an object by now, all URLs resolved before this call
        	if (ctx instanceof Map) {
        		// dereference @context key if present
                if (((Map<String, Object>) ctx).containsKey("@context")) {
                    ctx = (Map<String, Object>) ((Map<String, Object>) ctx).get("@context");
                }
            } else {
            	// context must be an object by now, all URLs resolved before this call
                throw new JSONLDProcessingError("@context must be an object")
                	.setType(JSONLDProcessingError.Error.SYNTAX_ERROR)
                	.setDetail("context", ctx);
            }
        	
        	// define context mappings for keys in local context
            HashMap<String, Boolean> defined = new HashMap<String, Boolean>();
            
            // helper for access to ctx as a map
            Map<String,Object> ctxm = (Map<String,Object>)ctx;
            // handle @base
            if (ctxm.containsKey("@base")) {
            	Object base = ctxm.get("@base");
            	
            	// reset base
            	if (base == null) {
            		base = opts.base;
            	}
            	else if (!isString(base)) {
            		throw new JSONLDProcessingError(
            				"Invalid JSON-LD syntax; the value of \"@base\" in a " +
            				"@context must be a string or null.")
            			.setType(JSONLDProcessingError.Error.SYNTAX_ERROR)
            			.setDetail("context", ctx);
            	} 
            	else if (!"".equals(base) && !isAbsoluteIri((String)base)) {
            		throw new JSONLDProcessingError(
            				"Invalid JSON-LD syntax; the value of \"@base\" in a " +
            				"@context must be an absolute IRI or the empty string.")
            			.setType(JSONLDProcessingError.Error.SYNTAX_ERROR)
            			.setDetail("context", ctx);
            	}
            	
            	base = URL.parse((String)base);
            	rval.put("@base", base);
            	defined.put("@base", true);
            }
            
            // handle @vocab
            if (ctxm.containsKey("@vocab")) {
            	Object value = ctxm.get("@vocab");
            	if (value == null) {
            		rval.remove("@vocab");
            	}
            	else if (!isString(value)) {
            		throw new JSONLDProcessingError(
            				"Invalid JSON-LD syntax; the value of \"@vocab\" in a " +
            				"@context must be a string or null.")
            			.setType(JSONLDProcessingError.Error.SYNTAX_ERROR)
            			.setDetail("context", ctx);
            	}
        		else if (!isAbsoluteIri((String)value)) {
            		throw new JSONLDProcessingError(
            				"Invalid JSON-LD syntax; the value of \"@vocab\" in a " +
            				"@context must be an absolute IRI.")
            			.setType(JSONLDProcessingError.Error.SYNTAX_ERROR)
            			.setDetail("context", ctx);
            	}
        		else {
        			rval.put("@vocab", value);
        		}
            	defined.put("@vocab", true);
            }
            
            // handle @language
            if (ctxm.containsKey("@language")) {
            	Object value = ctxm.get("@language");
            	if (value == null) {
            		rval.remove("@language");
            	}
            	else if (!isString(value)) {
            		throw new JSONLDProcessingError(
            				"Invalid JSON-LD syntax; the value of \"@language\" in a " +
            				"@context must be a string or null.")
            			.setType(JSONLDProcessingError.Error.SYNTAX_ERROR)
            			.setDetail("context", ctx);
            	}
            	else {
            		rval.put("@language", ((String)value).toLowerCase());
            	}
            	defined.put("@language", true);
            }
            
            // process all other keys
            for (String key : ctxm.keySet()) {
            	createTermDefinition(rval, ctxm, key, defined);
            }
        }
        
        // TODO: cache results

        return rval;
    }

    /**
     * Processes a local context and returns a new active context.
     *
     * @param activeCtx the current active context.
     * @param localCtx the local context to process.
     * @param options the context processing options.
     *
     * @return the new active context.
     * @throws JSONLDProcessingError 
     */
    public static ActiveContext processContext(ActiveContext activeCtx, Object localCtx, Options opts) throws JSONLDProcessingError {
        JSONLDProcessor p = new JSONLDProcessor(opts);
        if (localCtx == null) {
            return new ActiveContext();
        }

        localCtx = JSONLDUtils.clone(localCtx);
        if (localCtx instanceof Map && !((Map) localCtx).containsKey("@context")) {
            Map<String, Object> tmp = new HashMap<String, Object>();
            tmp.put("@context", localCtx);
            localCtx = tmp;
        }
        return p.processContext(activeCtx, localCtx);
    }

    /**
     * Expands a term into an absolute IRI. The term may be a regular term, a
     * prefix, a relative IRI, or an absolute IRI. In any case, the associated
     * absolute IRI will be returned.
     *
     * @param ctx the active context to use.
     * @param term the term to expand.
     * @param base the base IRI to use if a relative IRI is detected.
     *
     * @return the expanded term as an absolute IRI.
     */
    private String expandTerm(ActiveContext ctx, String term, String base) {
    	return expandTerm(ctx, term, base, false, false);
    }
    
    // NOTE: adding isKey and isValueOfType flags to support points 4.3.5/6 
    private String expandTerm(ActiveContext ctx, String term, String base, Boolean isKey, Boolean isValueOfType) {
    	// nothing to expand
        if (term == null) {
            return null;
        }

        // the term has a mapping, so it is a plain term
        if (ctx.mappings.containsKey(term)) {
            String id = (String) ((Map<String, Object>) ctx.mappings.get(term)).get("@id");
            // term is already an absolute IRI
            if (term.equals(id)) {
                return term;
            }
            return expandTerm(ctx, id, base);
        }

        // split term into prefix:suffix
        int colon = term.indexOf(':');
        if (colon != -1) {
            String prefix = term.substring(0, colon);
            String suffix = term.substring(colon + 1);

            // indicates the value is a blank node
            if ("_".equals(prefix)) {
                return term;
            }

            // indicates the value is an absolute IRI
            if (suffix.startsWith("//")) {
                return term;
            }

            // the term's prefix has a mapping, so it is a CURIE
            if (ctx.mappings.containsKey(prefix)) {
                return expandTerm(ctx, (String) ((Map<String, Object>) ctx.mappings.get(prefix)).get("@id"), base) + suffix;
            }

            // consider term an absolute IRI
            return term;
        }
        
        
        // TODO: 5) Otherwise, if the IRI being processed does not contain a colon and is a property,
        // i.e., a key in a JSON object, or the value of @type and the active context has a @vocab 
        // mapping, join the mapped value to the suffix using textual concatenation.
        
        // TODO: this seems to be missing a check to see if this is a keyword
        if (JSONLDUtils.isKeyword(term)) {
        	// then we just return the term
        }
        // use vocab // TODO: added base check, since terms that are not key's or values of @type should prioritise base before @vocab
        else if ((isKey || isValueOfType) && ctx.containsKey("@vocab")) {
        	term = prependBaseAndnormalizeURI((String) ctx.get("@vocab"), term);
        // prepend base to term
        } else if (!isKey && base != null) {
        	term = prependBaseAndnormalizeURI(base, term);            
        }

        return term;
    }
    
    /**
     * prepends the iri to the base uri and normalizes that URI
     * 
     * @param base
     * @param iri
     * @return the new normalized uri or the original iri if base is not a valid uri
     */
    private String prependBaseAndnormalizeURI(String base, String iri) {
        try {
        	URI b = new URI(base);
        	URI rval;
        	// URI.resolve does not handle query strings, bases ending with # or empty iris correctly
        	if (iri.startsWith("?") || base.endsWith("#") || "".equals(iri)) {
        		rval = new URI(base + iri);
        	} else {
        		rval = b.resolve(iri);
        	}

			rval = rval.normalize();
			// check if there are ./ or ../ still in the uri path, and if so normalize manually (as java doesn't handle excessive ../ correctly
			if (rval.getPath().contains("./")) {
				List<String> pathelems = new ArrayList<String>(Arrays.asList(rval.getPath().split("/")));
				String tmp; // used to force remove (int)
				for (int i = 0; i < pathelems.size(); i++) {
					if (".".equals(pathelems.get(i))) {
						tmp = pathelems.remove(i);
						--i;
					} else if ("..".equals(pathelems.get(i))) {
						 tmp = pathelems.remove(i);
						// make sure we don't remove the first element ""
						if (i > 1) {
							tmp = pathelems.remove(i-1);
							i =- 2;
						} else {
							--i;
						}
					}
				}
				String path = "";
				for (String p: pathelems) {
					path += p + "/";
				}
				if (!rval.getPath().endsWith("/")) {
					path = path.substring(0, path.length()-1); // remove trailing / if the original path doesn't end with one
				}
				rval = rval.resolve(path);
			}
			return rval.toString();
			
		} catch (URISyntaxException e) {
			// if we have an error here just ignore it and return the unmodified IRI
			return iri;
		}

	}

    /**
     * Ranks a term that is possible choice for compacting an IRI associated with
     * the given value.
     *
     * @param ctx the active context.
     * @param term the term to rank.
     * @param value the associated value.
     *
     * @return the term rank.
     */
    private static int rankTerm(ActiveContext ctx, String term, Object value) {
    	// no term restrictions for a null value
        if (value == null) {
            return 3;
        }

        // get context entry for term
        Map<String, Object> entry = (Map<String, Object>) ctx.mappings.get(term);
        
        // @list rank is the sum of its values' ranks
        if (value instanceof Map && ((Map<String, Object>) value).containsKey("@list")) {
            List<Object> list = (List<Object>) ((Map<String, Object>) value).get("@list");
            if (list.size() == 0) {
                return "@list".equals(entry.get("@container")) ? 1 : 0;
            }
            // sum term ranks for each list value
            int sum = 0;
            for (Object i : list) {
                sum += rankTerm(ctx, term, i);
            }
            return sum;
        }
        
        // Note: Value must be an object that is a @value or subject/reference.

        if (value instanceof Map && ((Map<String, Object>) value).containsKey("@value")) {
        	// value has a @type
            if (((Map<String, Object>) value).containsKey("@type")) {
            	// @types match
                if (entry.containsKey("@type")) {
                    Object vt = ((Map<String, Object>) value).get("@type");
                    Object et = entry.get("@type");
                    if ((vt == null && et == null) || (vt != null && vt.equals(et))) {
                        return 3;
                    }
                }
                return (!entry.containsKey("@type") && !entry.containsKey("@language")) ? 1 : 0;
            }

            // rank non-string value
            if (!(((Map<String, Object>) value).get("@value") instanceof String)) {
                return (!entry.containsKey("@type") && !entry.containsKey("@language")) ? 2 : 1;
            }

            // value has no @type or @language
            if (!((Map<String, Object>) value).containsKey("@language")) {
                if ((entry.containsKey("@language") && entry.get("@language") == null)
                        || (!entry.containsKey("@type") && !entry.containsKey("@language") && !ctx.containsKey("@language"))) {
                    return 3;
                }
                return 0;
            }

            // @languages match or entry has no @type or @language but default
            // @language matches
            Object vl = ((Map<String, Object>) value).get("@language");
            Object el = entry.get("@language");
            Object cl = ctx.get("@language");
            if ((entry.containsKey("@language") && ((vl == null && el == null) || vl.equals(el)))
                    || (!entry.containsKey("@type") && !entry.containsKey("@language") && (ctx.containsKey("@language") && ((vl == null && cl == null) || vl
                            .equals(cl))))) {
                return 3;
            }
            return (!entry.containsKey("@type") && !entry.containsKey("@language")) ? 1 : 0;
        }

        // value must be a subject/reference
        if ("@id".equals(entry.get("@type"))) {
            return 3;
        }

        return (!entry.containsKey("@type") && !entry.containsKey("@language")) ? 1 : 0;
    }

    /**
     * Recursively expands an element using the given context. Any context in
     * the element will be removed. All context URLs must have been retrieved
     * before calling this method.
     *
     * @param activeCtx the context to use.
     * @param activeProperty the property for the element, null for none.
     * @param element the element to expand.
     * @param options the expansion options.
     * @param insideList true if the element is a list, false if not.
     *
     * @return the expanded value.
     * 
     * TODO:
     *  - does this function always return a map, or can it also return a list, the expandedValue variable below seems to assume a map, but in javascript, `in` will just return false if the result is a list
     */
    public Object expand(ActiveContext activeCtx, String activeProperty, Object element, Boolean insideList) throws JSONLDProcessingError {
    	// nothing to expand
    	if (element == null) {
    		return null;
    	}

    	// recursively expand array
    	if (element instanceof List) {
    		List<Object> rval = new ArrayList<Object>();
    		for (Object i : (List<Object>) element) {
    			// expand element
    			Object e = expand(activeCtx, activeProperty, i, insideList);
    			if (insideList && (isArray(e) || isList(e))) {
    				// lists of lists are illegal
    				throw new JSONLDProcessingError("Invalid JSON-LD syntax; lists of lists are not permitted.")
    				.setType(JSONLDProcessingError.Error.SYNTAX_ERROR);
    				// drop null values
    			} else if (e != null) {
    				if (isArray(e)) {
    					rval.addAll((Collection<? extends Object>) e);
    				} else {
    					rval.add(e);
    				}
    			}
    		}
    		return rval;
    	}

    	/*
        // NOTE: HANDLING TEST-CASE 30 HERE
        // TODO: this will be incomplete as it doesn't seem to be defined yet in the spec
        // and isn't implemented in the javascript code, but as long as the tests pass I don't care!
        if (property != null && ctx.mappings.containsKey(property) && 
        		ctx.mappings.get(property) instanceof Map && ((Map)ctx.mappings.get(property)).containsKey("@container") &&
        		"@language".equals(((Map)ctx.mappings.get(property)).get("@container"))) {
        	// prob becomes @language
        	// value becomes @value
        	List<Object> rval = new ArrayList<Object>();
        	for (String key : ((Map<String,Object>)element).keySet()) {
        		Object value = ((Map<String,Object>)element).get(key);
        		value = expand(ctx, namer, null, value);
        		value = handleNestedLanguageContainer(value, key);
        		if (value instanceof List) {
        			rval.addAll((List)value);
        		} else {
        			rval.add(value);
        		}
        	}
        	return rval;
        }
    	 */

    	// recursively expand object
    	if (isObject(element)) {
    		// access helper
    		Map<String, Object> elem = (Map<String, Object>) element;

    		// if element has a context, process it
    		if (elem.containsKey("@context")) {
    			activeCtx = processContext(activeCtx, elem.get("@context"));
    			//elem.remove("@context");
    		}

    		// expand the active property
    		String expandedActiveProperty = expandIri(activeCtx, activeProperty, false, true, null, null); //  {vocab: true}

    		Object rval = new HashMap<String, Object>();
    		Map<String,Object> mval = (Map<String, Object>) rval; // to make things easier while we know rval is a map
    		List<String> keys = new ArrayList<String>(elem.keySet());
    		Collections.sort(keys);
    		for (String key : keys) {
    			Object value = elem.get(key);
    			Object expandedValue;

    			// skip @context
    			if (key.equals("@context")) {
    				continue;
    			}

    			// expand key to IRI
    			String expandedProperty = expandIri(activeCtx, key, false, true, null, null); //  {vocab: true}

    			// drop non-absolute IRI keys that aren't keywords
    			if (expandedProperty == null || !(isAbsoluteIri(expandedProperty) || isKeyword(expandedProperty))) {
    				continue;
    			}

    			if (isKeyword(expandedProperty) && "@reverse".equals(expandedActiveProperty)) {
    				throw new JSONLDProcessingError("Invalid JSON-LD syntax; a keyword cannot be used as a @reverse propery.")
    				.setType(JSONLDProcessingError.Error.SYNTAX_ERROR)
    				.setDetail("value", value);
    			}

    			if ("@id".equals(expandedProperty) && !isString(value)) {
    				throw new JSONLDProcessingError("Invalid JSON-LD syntax; \"@id\" value must a string.")
    				.setType(JSONLDProcessingError.Error.SYNTAX_ERROR)
    				.setDetail("value", value);
    			}

    			// validate @type value
    			if ("@type".equals(expandedProperty)) {
    				validateTypeValue(value);
    			}

    			// @graph must be an array or an object
    			if ("@graph".equals(expandedProperty) && !(isObject(value) || isArray(value))) {
    				throw new JSONLDProcessingError("Invalid JSON-LD syntax; \"@graph\" value must be an object or an array.")
    				.setType(JSONLDProcessingError.Error.SYNTAX_ERROR)
    				.setDetail("value", value);
    			}

    			// @value must not be an object or an array
    			if ("@value".equals(expandedProperty) && (value instanceof Map || value instanceof List)) {
    				throw new JSONLDProcessingError("Invalid JSON-LD syntax; \"@value\" value must not be an object or an array.")
    				.setType(JSONLDProcessingError.Error.SYNTAX_ERROR)
    				.setDetail("value", value);
    			}

    			// @language must be a string
    			if ("@language".equals(expandedProperty) && !(value instanceof String)) {
    				throw new JSONLDProcessingError("Invalid JSON-LD syntax; \"@language\" value must be a string.")
    				.setType(JSONLDProcessingError.Error.SYNTAX_ERROR)
    				.setDetail("value", value);
    			}

    			// @index must be a string
    			if ("@index".equals(expandedProperty) && !(value instanceof String)) {
    				throw new JSONLDProcessingError("Invalid JSON-LD syntax; \"@index\" value must be a string.")
    				.setType(JSONLDProcessingError.Error.SYNTAX_ERROR)
    				.setDetail("value", value);
    			}

    			// @reverse must be an object
    			if ("@reverse".equals(expandedProperty)) {
    				if (!isObject(value)) {
    					throw new JSONLDProcessingError("Invalid JSON-LD syntax; \"@reverse\" value must be an object.")
    					.setType(JSONLDProcessingError.Error.SYNTAX_ERROR)
    					.setDetail("value", value);
    				}

    				expandedValue = expand(activeCtx, "@reverse", value, insideList);

	    			// properties double-reversed
	    			if (expandedValue instanceof Map && ((Map<String, Object>) expandedValue).containsKey("@reverse")) {
	    				// TODO: javascript seems to assume that the value of reverse will always be an object, may need to add a check here if this turns out to be the case
	    				Map<String,Object> rev = (Map<String, Object>) ((Map<String, Object>) expandedValue).get("@reverse");
	    				for (String property : rev.keySet()) {
	    					addValue(mval, property, rev.get(property), true);
	    				}
	
	    			}

	    			// FIXME: can this be merged with the code below to simplify?
	    			// merge in all reversed properties
	    			if (expandedValue instanceof Map) { // TODO: javascript doesn't make this check, can we assume expandedValue is always going to be an object?
	    				Map<String,Object> reverseMap = (Map<String, Object>) mval.get("@reverse");
	    				for (String property : ((Map<String, Object>) expandedValue).keySet()) {
	    					if ("@reverse".equals(property)) {
	    						continue;
	    					}
	    					if (reverseMap == null) {
	    						reverseMap = new HashMap<String, Object>();
	    						mval.put("@reverse", reverseMap);
	    					}
	    					addValue(reverseMap, property, new ArrayList<Object>(), true);
	    					List<Object> items = (List<Object>) ((Map<String, Object>) expandedValue).get(property);
	    					for (Object item : items) {
	    						if (isValue(item) || isList(item)) {
	    							throw new JSONLDProcessingError("Invalid JSON-LD syntax; \"@reverse\" value must not be a @value or an @list.")
	    							.setType(JSONLDProcessingError.Error.SYNTAX_ERROR)
	    							.setDetail("value", expandedValue);
	    						}
	    						addValue(reverseMap, property, item, true);
	    					}
	    				}
	    			}
	    			continue;
	    		}

    			String container = (String) activeCtx.getContextValue(key, "@container");

    			// handle language map container (skip if value is not an object)
    			if ("@language".equals(container) && isObject(value)) {
    				expandedValue = expandLanguageMap((Map<String,Object>)value);
    			}
    			// handle index container (skip if value is not an object)
    			else if ("@index".equals(container) && isObject(value)) {
    				// NOTE: implementing embeded function expandIndexMap from javascript as rolled out code here
    				// as it doesn't call itself and needs access to this instance's expand method.
    				// using eim_ prefix for variables to avoid clashes
    				String eim_activeProperty = key;
    				List<Object> eim_rval = new ArrayList<Object>();
    		    	for (String eim_key: ((Map<String, Object>) value).keySet()) {
    		    		List<Object> eim_val;
    					if (!isArray(((Map<String, Object>) value).get(eim_key))) {
    						eim_val = new ArrayList<Object>();
    						eim_val.add(((Map<String, Object>) value).get(eim_key));
    					} else {
    						eim_val = (List<Object>) ((Map<String, Object>) value).get(eim_key);
    					}
    					// NOTE: javascript assumes list result here, so I am as well
    					eim_val = (List<Object>) expand(activeCtx, eim_activeProperty, eim_val, false);
    					for (Object eim_item : eim_val) {
    						if (isObject(eim_item)) {
    							if (!((Map<String, Object>) eim_item).containsKey("@index")) {
    								((Map<String, Object>) eim_item).put("@index", eim_key);
    							}
    							eim_rval.add(eim_item);
    						}
    					}
    		    	}
    				expandedValue = eim_rval;
    			} else {
    				// recurse into @list or @set
    				Boolean isList = "@list".equals(expandedProperty);
    				if (isList || "@set".equals(expandedProperty)) {
    					String nextActiveProperty = activeProperty;
    					if (isList && "@graph".equals(expandedActiveProperty)) {
    						nextActiveProperty = null;
    					}
    					expandedValue = expand(activeCtx, nextActiveProperty, value, isList);
    					if (isList && isList(expandedValue)) {
    						throw new JSONLDProcessingError("Invalid JSON-LD syntax; lists of lists are not permitted.")
    						.setType(JSONLDProcessingError.Error.SYNTAX_ERROR);
    					}
    				} 
    				else {
    					// recursively expand value with key as new active property
    					expandedValue = expand(activeCtx, key, value, false);
    				}
    			}

    			// drop null values if property is not @value
    			if (expandedValue == null && !"@value".equals(expandedProperty)) {
    				continue;
    			}

    			// convert expanded value to @list if container specified it
    			if (!"@list".equals(expandedProperty) && !isList(expandedValue) && "@list".equals(container)) {
    				// ensure expanded value is an array
    				Map<String,Object> tm = new HashMap<String,Object>();
    				List<Object> tl;
    				if (isArray(expandedValue)) {
    					tl = (List<Object>) expandedValue;
    				} else {
    					tl = new ArrayList<Object>();
    					tl.add(expandedValue);
    				}
    				tm.put("@list", tl);
    				expandedValue = tm;
    			}

    			// FIXME: can this be merged with the code above to simplify?
    			// merge in all reversed properties
    			if (Boolean.TRUE.equals(Obj.get(activeCtx.mappings, key, "reverse"))) {
    				Map<String,Object> reverseMap = new HashMap<String,Object>();
    				mval.put("@reverse", reverseMap);
    				if (!isArray(expandedValue)) {
    					List<Object> tmp = new ArrayList<Object>();
    					tmp.add(expandedValue);
    					expandedValue = tmp;
    				}
    				for (Object item : (List<Object>)expandedValue) {
    					if (isValue(item) || isList(item)) {
    						throw new JSONLDProcessingError("Invalid JSON-LD syntax; \"@reverse\" value must not be a @value or an @list.")
    						.setType(JSONLDProcessingError.Error.SYNTAX_ERROR)
    						.setDetail("value", expandedValue);
    					}
    					addValue(reverseMap, expandedProperty, item, true);
    				}
    				continue;
    			}

    			// add value for property
    			// use an array except for certain keywords
    			Boolean useArray = !("@index".equals(expandedProperty) || "@id".equals(expandedProperty) || 
    					"@type".equals(expandedProperty) || "@value".equals(expandedProperty) || 
    					"@language".equals(expandedProperty));
    			addValue(mval, expandedProperty, expandedValue, useArray);

    		}

    		// get property count on expanded output
    		int count = mval.size(); 

    		// @value must only have @language or @type
    		if (mval.containsKey("@value")) {
    			// @value must only have @language or @type
    			if (mval.containsKey("@type") && mval.containsKey("@language")) {
    				throw new JSONLDProcessingError(
    						"Invalid JSON-LD syntax; an element containing \"@value\" may not contain both \"@type\" and \"@language\".")
    				.setType(JSONLDProcessingError.Error.SYNTAX_ERROR)
    				.setDetail("element", mval);
    			}
    			int validCount = count -1;
    			if (mval.containsKey("@type") || mval.containsKey("@language")) {
    				validCount -= 1;
    			}
    			if (mval.containsKey("@index")) {
    				validCount -= 1;
    			}
    			if (validCount != 0) {
    				throw new JSONLDProcessingError(
    						"Invalid JSON-LD syntax; an element containing \"@value\" may only have an \"@index\" property " +
    						"and at most one other property which can be \"@type\" or \"@language\".")
    				.setType(JSONLDProcessingError.Error.SYNTAX_ERROR)
    				.setDetail("element", mval);
    			}
    			
    			// drop null @values
    			if (mval.get("@value") == null) {
    				rval = null; mval = null;
    			}
    			// drop @language if @value isn't a string
    			else if (mval.containsKey("@language") && !isString(mval.get("@value"))) {
    				mval.remove("@language");
    			}
    		}
    		// convert @type to an array
    		else if (mval.containsKey("@type") && !isArray(mval.get("@type"))) {
    			List<Object> tmp = new ArrayList<Object>();
    			tmp.add(mval.get("@type"));
    			mval.put("@type", tmp);
    		}
    		// handle @set and @list
    		else if (mval.containsKey("@set") || mval.containsKey("@list")) {
    			if (count > 1 && (count != 2 && mval.containsKey("@index"))) {
    				throw new JSONLDProcessingError(
    						"Invalid JSON-LD syntax; if an element has the property \"@set\" or \"@list\", then it can have " +
    						"at most one other property that is \"@index\".")
    				.setType(JSONLDProcessingError.Error.SYNTAX_ERROR)
    				.setDetail("element", mval);
    			}
    			// optimize away @set
    			if (mval.containsKey("@set")) {
    				rval = mval.get("@set");
    				mval = null; // result is no longer a map, so don't allow this to be used anymore
    				count = ((Collection)rval).size(); // TODO: i'm sure the result here should be a List, but Collection works, so it'll do for now
    			}
    		} 
    		// drop objects with only @language
    		else if (mval.containsKey("@language") && count == 1) {
    			rval = null; mval = null;
    		}

    		// drop certain top-level object that do not occur in lists
    		if (isObject(rval) && !opts.keepFreeFloatingNodes && !insideList &&
    				(activeProperty == null || "@graph".equals(expandedActiveProperty))) {
    			// drop empty object or top-level @value
    			if (count == 0 || mval.containsKey("@value")) {
    				rval = null; mval = null;
    			} else {
    				// drop nodes that generate no triples
    				boolean hasTriples = false;
    				for (String key: mval.keySet()) {
    					if (hasTriples) {
    						break;
    					}
    					if (!isKeyword(key) || "@graph".equals(key) || "@type".equals(key)) {
    						hasTriples = true;
    					}
    				}
    				if (!hasTriples) {
    					rval = null; mval = null;
    				}
    			}
    		}
    		
    		return rval;
    	}

    	// drop top-level scalars that are not in lists
    	if (!insideList && (activeProperty == null || "@graph".equals(expandIri(activeCtx, activeProperty, false, true, null, null)))) {
    		return null;
    	}
    	
    	// expand element according to value expansion rules
    	return expandValue(activeCtx, activeProperty, element);
    }

	/**
     * Used in the handling of @language containers
     * 
     * @param value
     * @param lang
     * @return
     */
    private Object handleNestedLanguageContainer(Object value, String lang) {
    	if (value == null) {
    		return null;
    	}
    	if (value instanceof String) {
    		// since we expand out values before we call this function, a string @value should be represented as a map with
    		// an @value tag again, so we'll ignore these cases
    		return value;
    	}
    	if (value instanceof List) {
    		List<Object> rval = new ArrayList<Object>();
			for (Object v: ((List)value)) {
				rval.add(handleNestedLanguageContainer(v, lang));
			}
			return rval;
		}
    	// only thing left is a map
    	Map<String,Object> rval;
    	
    	// if that map already has a @value key, just add a @language tag to it
    	if (((Map<String,Object>)value).containsKey("@value")) {
    		rval = (Map<String, Object>)JSONLDUtils.clone(value);
    		rval.put("@language", lang);
    		return rval;
    	}
    	
    	rval = new HashMap<String,Object>();
    	for (String key: ((Map<String, Object>) value).keySet()) {
			rval.put(key, handleNestedLanguageContainer(((Map<String,Object>)value).get(key), lang));
		}
		return rval;
		
	}

    /**
     * Recursively compacts an element using the given active context. All values
     * must be in expanded form before this method is called.
     *
     * @param activeCtx the active context to use.
     * @param activeProperty the compacted property associated with the element
     *          to compact, null for none.
     * @param element the element to compact.
     * @param options the compaction options.
     *
     * @return the compacted value.
     */
    public Object compact(ActiveContext activeCtx, String activeProperty, Object element) throws JSONLDProcessingError {

    	// recursively compact array
        if (isArray(element)) {
            List<Object> rval = new ArrayList<Object>();
            for (Object i : (List<Object>) element) {
            	// compact, dropping any null values
                Object compacted = compact(activeCtx, activeProperty, i);
                if (compacted != null) {
                    rval.add(compacted);
                }
            }
            if (opts.compactArrays && rval.size() == 1) {
            	// use single element if no container is specified
                Object container = activeCtx.getContextValue(activeProperty, "@container");
                if (container == null) {
                    return rval.get(0);
                }
            }
            return rval;
        }

        // recursively compact object
        if (isObject(element)) {
        	// access helper
            Map<String, Object> elem = (Map<String, Object>) element;
            
            // do value compaction on @value and subject references
            if (isValue(element) || isSubjectReference(element)) {
            	return compactValue(activeCtx, activeProperty, element);
            }
            
            // FIXME: avoid misuse of active property as an expanded property?
            boolean insideReverse = ("@reverse".equals(activeProperty));
            
            // process element keys in order
            List<String> keys = new ArrayList<String>(elem.keySet());
            Collections.sort(keys);
            Map<String,Object> rval = new HashMap<String, Object>();
            for (String expandedProperty : keys) {
                Object expandedValue = elem.get(expandedProperty);
                
                /* TODO:
                // handle ignored keys
                if (opts.isIgnored(key)) {
                	//JSONLDUtils.addValue(rval, key, value, false);
                	rval.put(key, value);
                	continue;
                }
                */
                
                // compact @id and @type(s)
                if ("@id".equals(expandedProperty) || "@type".equals(expandedProperty)) {
                	Object compactedValue;
                	
                	// compact single @id
                    if (isString(expandedValue)) {
                        compactedValue = compactIri(activeCtx, (String)expandedValue, null, "@type".equals(expandedProperty), false);
                    }
                    // expanded value must be a @type array
                    else {
                        List<String> types = new ArrayList<String>();
                        for (String i : (List<String>) expandedValue) {
                            types.add(compactIri(activeCtx, i, null, true, false));
                        }
                        compactedValue = types;
                    }

                    // use keyword alias and add value
                    String alias = compactIri(activeCtx, expandedProperty);
                    addValue(rval, alias, compactedValue, 
                    		isArray(compactedValue) && ((List<Object>) expandedValue).size() == 0);
                    continue;
                }

                // handle @reverse
                if ("@reverse".equals(expandedProperty)) {
                	// recursively compact expanded value
                	// TODO: i'm assuming this will always be a map due to the rest of the code 
                	Map<String,Object> compactedValue = (Map<String, Object>) compact(activeCtx, "@reverse", expandedValue);
                	
                	// handle double-reversed properties
                	for (String compactedProperty : compactedValue.keySet()) {
                		
                		if (Boolean.TRUE.equals(Obj.get(activeCtx.mappings, compactedProperty, "reverse"))) {
                			if (!rval.containsKey(compactedProperty) && !opts.compactArrays) {
                				rval.put(compactedProperty, new ArrayList<Object>());
                			}
                			addValue(rval, compactedProperty, compactedValue.get(compactedProperty));
                			compactedValue.remove(compactedProperty);
                		}
                	}
                	
                	if (compactedValue.size() > 0) {
                		// use keyword alias and add value
                		addValue(rval, compactIri(activeCtx, expandedProperty), compactedValue);
                	}
                	
                	continue;
                }

                // handle @index property
                if ("@index".equals(expandedProperty)) {
                	// drop @index if inside an @index container
                	String container = (String) activeCtx.getContextValue(activeProperty, "@container");
                	if ("@index".equals(container)) {
                		continue;
                	}
                	
                	// use keyword alias and add value
                	addValue(rval, compactIri(activeCtx, expandedProperty), expandedValue);
                	continue;
                }
                
                // NOTE: expanded value must be an array due to expansion algorithm.
                
                // preserve empty arrays
                if (((List<Object>) expandedValue).size() == 0) {
                	addValue(rval, compactIri(activeCtx, expandedProperty, expandedValue, true, insideReverse), expandedValue, true);
                }
               
                // recusively process array values
                for (Object expandedItem : (List<Object>) expandedValue) {
                	// compact property and get container type
                	String itemActiveProperty = compactIri(activeCtx, expandedProperty, expandedItem, true, insideReverse);
                    String container = (String) activeCtx.getContextValue(itemActiveProperty, "@container");
                    
                    // get @list value if appropriate
                    boolean isList = isList(expandedItem);
                    Object list = null;
                    if (isList) {
                    	list = ((HashMap<String, Object>) expandedItem).get("@list");
                    }
                    
                    // recursively compact expanded item
                    Object compactedItem = compact(activeCtx, itemActiveProperty, isList ? list : expandedItem);
                    
                    // handle @list
                    if (isList) {
                    	// ensure @list value is an array
                    	if (!isArray(compactedItem)) {
                    		List<Object> tmp = new ArrayList<Object>();
                    		tmp.add(compactedItem);
                    		compactedItem = tmp;
                    	}
                    	
                    	if (!"@list".equals(container)) {
                    		// wrap using @list alias
                    		Map<String,Object> wrapper = new HashMap<String, Object>();
                    		wrapper.put(compactIri(activeCtx, "@list"), compactedItem);
                    		compactedItem = wrapper;
                    		
                    		// include @index from expanded @list, if any
                    		if (((HashMap<String, Object>) expandedItem).containsKey("@index")) {
                    			((HashMap<String, Object>) compactedItem).put(compactIri(activeCtx, "@index"), ((HashMap<String, Object>) expandedItem).get("@index"));
                    		}
                    	}
                    	// can't use @list container for more than 1 list
                    	else if (rval.containsKey(itemActiveProperty)) {
                    		throw new JSONLDProcessingError(
            						"Invalid JSON-LD compact error; property has a \"@list\" @container " +
            						"rule but there is more than a single @list that matches " +
            						"the compacted term in the document. Compaction might mix " +
            						"unwanted items into the list.")
            					.setType(JSONLDProcessingError.Error.SYNTAX_ERROR);
                    	}
                    }
                    
                    // handle language and index maps
                    if ("@language".equals(container) || "@index".equals(container)) {
                    	// get or create the map object
                    	Map<String, Object> mapObject;
                    	if (rval.containsKey(itemActiveProperty)) {
                    		mapObject = (Map<String, Object>) rval.get(itemActiveProperty);
                    	}
                    	else {
                    		mapObject = new HashMap<String, Object>();
                    		rval.put(itemActiveProperty, mapObject);
                    	}
                    	
                    	// if container is a language map, simplify compacted value to
                    	// a simple string
                    	if ("@language".equals(container) && isValue(compactedItem)) {
                    		compactedItem = ((HashMap<String, Object>) compactedItem).get("@value");
                    	}
                    	
                    	// add compact value to map object using key from expanded value
                    	// based on the container type
                    	addValue(mapObject, (String)((HashMap<String, Object>) expandedItem).get(container), compactedItem);
                    }
                    else {
                    	// use an array if: compactArrays flag is false,
                    	// @container is @set or @list, value is an empty
                    	// array, or key is @graph
                    	Boolean isArray = (!opts.compactArrays || "@set".equals(container) || "@list".equals(container) ||
                    			(isArray(compactedItem) && ((List<Object>) compactedItem).size() == 0) ||
                    			"@list".equals(expandedProperty) || "@graph".equals(expandedProperty));
                    	
                    	// add compact value
                    	addValue(rval, itemActiveProperty, compactedItem, isArray);
                    }
                }
            }
            
            return rval;
        }
        
        // only primatives remain which are already compact
        return element;
    }

	private class FramingContext {
    	public Map<String,Object> embeds = null;
    	public Map<String,Object> graphs = null;
    	public Map<String,Object> subjects = null;
    	public Options options = opts;
    }
    
	/**
	 * Performs JSON-LD framing.
	 *
	 * @param input the expanded JSON-LD to frame.
	 * @param frame the expanded JSON-LD frame to use.
	 * @param options the framing options.
	 *
	 * @return the framed output.
     * @throws JSONLDProcessingError 
     */
    public Object frame(Object input, Object frame) throws JSONLDProcessingError {
    	// create framing state
    	FramingContext state = new FramingContext();
    	//Map<String,Object> state = new HashMap<String, Object>();
    	//state.put("options", this.opts);
    	state.graphs = new HashMap<String, Object>();
    	state.graphs.put("@default", new HashMap<String, Object>());
    	state.graphs.put("@merged", new HashMap<String, Object>());
    	
    	// produce a map of all graphs and name each bnode
    	// FIXME: currently uses subjects from @merged graph only
    	UniqueNamer namer = new UniqueNamer("_:b");
    	createNodeMap(input, state.graphs, "@merged", namer);
    	state.subjects = (Map<String, Object>) state.graphs.get("@merged");
    	
    	// frame the subjects
        List<Object> framed = new ArrayList<Object>();
        List<String> sortedKeys = new ArrayList<String>(state.subjects.keySet());
        Collections.sort(sortedKeys);
        frame(state, sortedKeys, frame, framed, null);
    	return framed;
    }

    /**
     * Frames subjects according to the given frame.
     *
     * @param state the current framing state.
     * @param subjects the subjects to filter.
     * @param frame the frame.
     * @param parent the parent subject or top-level array.
     * @param property the parent property, initialized to null.
     * @throws JSONLDProcessingError 
     */
    private void frame(FramingContext state, Collection<String> subjects,
			Object frame, Object parent, String property) throws JSONLDProcessingError {
		// validate the frame
    	validateFrame(state, frame);
    	// NOTE: once validated we move to the function where the frame is specifically a map
    	frame(state, subjects, (Map<String,Object>)((List<Object>)frame).get(0), parent, property);
    }
    
    private void frame(FramingContext state, Collection<String> subjects, 
    		Map<String,Object> frame, Object parent, String property) throws JSONLDProcessingError {
    	// filter out subjects that match the frame
    	Map<String,Object> matches = filterSubjects(state, subjects, frame);
    	
    	// get flags for current frame
    	Options options = state.options;
    	Boolean embedOn = (frame.containsKey("@embed")) ? (Boolean)((List)frame.get("@embed")).get(0) : options.embed;
    	Boolean explicicOn = (frame.containsKey("@explicit")) ? (Boolean)((List)frame.get("@explicit")).get(0) : options.explicit;
    	
    	// add matches to output
    	List<String> ids = new ArrayList<String>(matches.keySet());
    	Collections.sort(ids);
    	for (String id: ids) {
    		
    		// Note: In order to treat each top-level match as a compartmentalized
    	    // result, create an independent copy of the embedded subjects map when the
    	    // property is null, which only occurs at the top-level.
    		if (property == null) {
    			state.embeds = new HashMap<String,Object>();
    		}
    		
    		// start output
    		Map<String,Object> output = new HashMap<String,Object>();
    		output.put("@id", id);
    		
    		// prepare embed meta info
    		Map<String,Object> embed = new HashMap<String, Object>();
    		embed.put("parent", parent);
    		embed.put("property", property);
    		
    		// if embed is on and there is an existing embed
    		if (embedOn && state.embeds.containsKey(id)) {
    			// only overwrite an existing embed if it has already been added to its
    			// parent -- otherwise its parent is somewhere up the tree from this
    			// embed and the embed would occur twice once the tree is added
    			embedOn = false;
    			
    			// existing embed's parent is an array
    			Map<String,Object> existing = (Map<String, Object>) state.embeds.get(id);
    			if (isArray(existing.get("parent"))) {
    				for (Object o: (List<Object>)existing.get("parent")) {
    					if (compareValues(output, o)) {
    						embedOn = true;
    						break;
    					}
    				}
    			}
    			// existing embed's parent is an object
    			else if (hasValue((Map<String,Object>)existing.get("parent"), (String)existing.get("property"), output)) {
    				embedOn = true;
    			}
    			
    			// existing embed has already been added, so allow an overwrite
    			if (embedOn) {
    				removeEmbed(state, id);
    			}
    		}
    		
    		// not embedding, add output without any other properties
    		if (!embedOn) {
    			addFrameOutput(state, parent, property, output);
    		} else {
    			// add embed meta info
    			state.embeds.put(id, embed);
    			
    			// iterate over subject properties
    			Map<String,Object> subject = (Map<String,Object>) matches.get(id);
    			List<String> props = new ArrayList<String>(subject.keySet());
    			Collections.sort(props);
    			for (String prop: props) {

    				// handle ignored keys
    				if (opts.isIgnored(prop)) {
    					output.put((String) prop, JSONLDUtils.clone(subject.get(prop)));
    					continue;
    				}
    				
    				// copy keywords to output
    				if (isKeyword(prop)) {
    					output.put((String) prop, JSONLDUtils.clone(subject.get(prop)));
    					continue;
    				}
    				
    				// if property isn't in the frame
    				if (!frame.containsKey(prop)) {
    					// if explicit is off, embed values
    					if (!explicicOn) {
    						embedValues(state, subject, prop, output);
    					}
    					continue;
    				}
    				
    				// add objects
    				Object objects = subject.get(prop); 
    				// TODO: i've done some crazy stuff here because i'm unsure if objects is always a list or if it can
    				// be a map as well. I think it's always a map, but i'll get it working like this first
    				for (Object i: objects instanceof List ? (List)objects : ((Map)objects).keySet()) {
    					Object o = objects instanceof List ? i : ((Map)objects).get(i);
    					
    					// recurse into list
    					if (isList(o)) {
    						// add empty list
    						Map<String,Object> list = new HashMap<String, Object>();
    						list.put("@list", new ArrayList<Object>());
    						addFrameOutput(state, output, prop, list);
    						
    						// add list objects
    						List src = (List)((Map)o).get("@list");
    						for (Object n: src) {
    							// recurse into subject reference
    							if (isSubjectReference(o)) {
    								List tmp = new ArrayList();
    								tmp.add(((Map)n).get("@id"));
    								frame(state, tmp, frame.get(prop), list, "@list");
    							} else {
    								// include other values automatcially
    								addFrameOutput(state, list, "@list", (Map<String, Object>) JSONLDUtils.clone(n));
    							}
    						}
    						continue;
    					}
    					
    					// recurse into subject reference
    					if (isSubjectReference(o)) {
    						List tmp = new ArrayList();
							tmp.add(((Map)o).get("@id"));
							frame(state, tmp, frame.get(prop), output, prop);
    					} else {
    						// include other values automatically
    						addFrameOutput(state, output, prop, (Map<String, Object>) JSONLDUtils.clone(o));
    					}
    				}
    			}
    			
    			// handle defaults
    			props = new ArrayList<String>(frame.keySet());
    			Collections.sort(props);
    			for (String prop: props) {
    				// skip keywords
    				if (isKeyword(prop)) {
    					continue;
    				}
    				
    				// if omit default is off, then include default values for properties
    		        // that appear in the next frame but are not in the matching subject
    				Map<String,Object> next = (Map<String, Object>) ((List<Object>) frame.get(prop)).get(0);
    				boolean omitDefaultOn = 
    						(next.containsKey("@omitDefault")) ? (Boolean)((List)next.get("@omitDefault")).get(0) : options.omitDefault ;
    				if (!omitDefaultOn && !output.containsKey(prop)) {
    					Object preserve = "@null";
    					if (next.containsKey("@default")) {
    						preserve = JSONLDUtils.clone(next.get("@default"));
    					}
    					if (!isArray(preserve)) {
    						List<Object> tmp = new ArrayList<Object>();
    						tmp.add(preserve);
        					preserve = tmp;
    					}
    					Map<String,Object> tmp1 = new HashMap<String, Object>();
    					tmp1.put("@preserve", preserve);
    					List<Object> tmp2 = new ArrayList<Object>();
    					tmp2.add(tmp1);
    					output.put(prop, tmp2);
    				}
    			}
    			
    			// add output to parent
    			addFrameOutput(state, parent, property, output);
    		}
    	}
	}
    
    /**
     * Embeds values for the given subject and property into the given output
     * during the framing algorithm.
     *
     * @param state the current framing state.
     * @param subject the subject.
     * @param property the property.
     * @param output the output.
     */
    private void embedValues(FramingContext state,
			Map<String, Object> subject, String property, Object output) {
    	// embed subject properties in output
    	Object objects = subject.get(property);
    	
    	// TODO: more craziness due to lack of knowledge about whether objects should
    	// be an array or an object
    	for (Object i: objects instanceof List ? (List)objects : ((Map)objects).keySet()) {
			Object o = objects instanceof List ? i : ((Map)objects).get(i);
			
			// recurse into @list
			if (isList(o)) {
				Map<String,Object> list = new HashMap<String, Object>();
				list.put("@list", new ArrayList());
				addFrameOutput(state, output, property, list);
				embedValues(state, (Map<String,Object>)o, "@list", list.get("@list"));
				return;
			}
			
			// handle subject reference
			if (isSubjectReference(o)) {
				String id = (String) ((Map<String, Object>) o).get("@id");
				
				// embed full subject if isn't already embedded
				if (!state.embeds.containsKey(id)) {
					// add embed
					Map<String,Object> embed = new HashMap<String, Object>();
					embed.put("parent", output);
					embed.put("property", property);
					state.embeds.put(id, embed);
					
					// recurse into subject
					o = new HashMap<String,Object>();
					Map<String,Object> s = (Map<String, Object>) state.subjects.get(id);
					for (String prop: s.keySet()) {
						// copy keywords
						if (isKeyword(prop) || opts.isIgnored(prop)) {
							((Map<String, Object>) o).put(prop, JSONLDUtils.clone(s.get(prop)));
							continue;
						}
						embedValues(state, s, prop, o);
					}
				}
				addFrameOutput(state, output, property, o);
			}
			// copy non-subject value
			else {
				addFrameOutput(state, output, property, JSONLDUtils.clone(o));
			}
    	}
		
	}

	/**
     * Adds framing output to the given parent.
     *
     * @param state the current framing state.
     * @param parent the parent to add to.
     * @param property the parent property.
     * @param output the output to add.
     */
    private static void addFrameOutput(FramingContext state, Object parent,
			String property, Object output) {
		if (isObject(parent)) {
			addValue((Map<String,Object>)parent, property, output, true);
		} else {
			((List)parent).add(output);
		}
		
	}

    /**
     * Removes an existing embed.
     *
     * @param state the current framing state.
     * @param id the @id of the embed to remove.
     */
	private static void removeEmbed(FramingContext state, String id) {
		// get existing embed
		Map<String, Object> embeds = state.embeds;
		Map<String,Object> embed = (Map<String, Object>) embeds.get(id);
		Object parent = embed.get("parent");
		String property = (String) embed.get("property");
		
		// create reference to replace embed
		Map<String,Object> subject = new HashMap<String, Object>();
		subject.put("@id", id);
		
		// remove existing embed
		if (isArray(parent)) {
			// replace subject with reference
			for (int i = 0; i < ((List)parent).size(); i++) {
				if (compareValues(((List)parent).get(i), subject)) {
					((List)parent).set(i, subject);
					break;
				}
			}
		} else {
			// replace subject with reference
			removeValue(((Map<String,Object>)parent), property, subject, ((Map<String, Object>) parent).get(property) instanceof List);
			addValue(((Map<String,Object>)parent), property, subject, ((Map<String, Object>) parent).get(property) instanceof List);
		}
		
		// recursively remove dependent dangling embeds
		removeDependents(embeds, id);
	}
    
    private static void removeDependents(Map<String,Object> embeds, String id) {
    	// get embed keys as a separate array to enable deleting keys in map
    	Set<String> ids = embeds.keySet();
    	for (String next: ids) {
    		if (embeds.containsKey(next) && 
    				((Map<String, Object>) embeds.get(next)).get("parent") instanceof Map &&
    				id.equals(((Map<String, Object>) ((Map<String, Object>) embeds.get(next)).get("parent")).get("@id"))) {
    			embeds.remove(next);
    			removeDependents(embeds, next);
    		}
    	}
    }

	/**
     * Returns a map of all of the subjects that match a parsed frame.
     *
     * @param state the current framing state.
     * @param subjects the set of subjects to filter.
     * @param frame the parsed frame.
     *
     * @return all of the matched subjects.
     */
    private static Map<String,Object> filterSubjects(FramingContext state,
			Collection<String> subjects, Map<String,Object> frame) {
    	// filter subjects in @id order
		Map<String,Object> rval = new HashMap<String,Object>();
		for (String id: subjects) {
			Map<String,Object> subject = (Map<String, Object>) state.subjects.get(id);
			if (filterSubject(subject, frame)) {
				rval.put(id, subject);
			}
		}
		return rval;
	}

    /**
     * Returns true if the given subject matches the given frame.
     *
     * @param subject the subject to check.
     * @param frame the frame to check.
     *
     * @return true if the subject matches, false if not.
     */
	private static boolean filterSubject(Map<String,Object> subject, Map<String,Object> frame) {
		// check @type (object value means 'any' type, fall through to ducktyping)
		Object t = frame.get("@type");
		// TODO: it seems @type should always be a list
		if (frame.containsKey("@type") && !(t instanceof List && ((List)t).size() == 1 && ((List)t).get(0) instanceof Map)) {
			for (Object i: (List)t) {
				if (hasValue(subject, "@type", i)) {
					return true;
				}
			}
			return false;
		}
		
		// check ducktype
		for (String key: frame.keySet()) {
			if ("@id".equals(key) || !isKeyword(key) && !(subject.containsKey(key))) {
				return false;
			}
		}
		return true;
	}

	/**
     * Validates a JSON-LD frame, throwing an exception if the frame is invalid.
     *
     * @param state the current frame state.
     * @param frame the frame to validate.
	 * @throws JSONLDProcessingError 
     */
	private static void validateFrame(FramingContext state, Object frame) throws JSONLDProcessingError {
		if (!(frame instanceof List) || ((List)frame).size() != 1 || !(((List) frame).get(0) instanceof Map)) {
			throw new JSONLDProcessingError("Invalid JSON-LD syntax; a JSON-LD frame must be a single object.")
				.setType(JSONLDProcessingError.Error.SYNTAX_ERROR)
				.setDetail("frame", frame);
		}
	}

    /**
     * Recursively flattens the subjects in the given JSON-LD expanded input.
     *
     * @param input the JSON-LD expanded input.
     * @param graphs a map of graph name to subject map.
     * @param graph the name of the current graph.
     * @param namer the blank node namer.
     * @param name the name assigned to the current input if it is a bnode.
     * @param list the list to append to, null for none.
     */
    private void flatten(Object input, Map<String,Object> graphs, String graph, UniqueNamer namer, String name, List<Object> list) {
    	// recurse through array
    	if (input instanceof List) {
    		for (Object i: (List)input) {
    			flatten(i, graphs, graph, namer, null, list);
    		}
    		return;
    	}
    	
    	// add non-object or value
    	if (!(input instanceof Map) || ((Map)input).containsKey("@value")) {
    		if (list != null) {
    			list.add(input);
    		}
    		return;
    	}
    	
    	// TODO: isUndefined (in js this is different from === null
    	// get name for subject
    	if (name == null) {
    		name = JSONLDUtils.isBlankNode(input) ? namer.getName((String) ((Map<String, Object>) input).get("@id")) : (String)((Map<String, Object>) input).get("@id");
    	}
    	
    	// add subject reference to list
    	if (list != null) {
    		HashMap<String, Object> map = new HashMap<String,Object>();
    		map.put("@id", name);
    		list.add(map);
    	}
    	
    	// create new subject or merge into existing one
    	Map<String,Object> subjects = (Map<String, Object>) graphs.get(graph);
    	Map<String,Object> subject;
    	if (subjects.containsKey(name)) {
    		subject = (Map<String, Object>) subjects.get(name);
    	} else {
    		subject = new HashMap<String, Object>();
    		subjects.put(name, subject);
    	}
    	subject.put("@id", name);
    	for (String prop: ((Map<String, Object>) input).keySet()) {
    		// skip @id
    		if ("@id".equals(prop)) {
    			continue;
    		}
    		
    		// recurse into graph
    		if ("@graph".equals(prop)) {
    			// add graph subjects map entry
    			if (!graphs.containsKey(name)) {
    				graphs.put(name, new HashMap<String, Object>());
    			}
    			String g = "@merged".equals(graph) ? graph : name;
    			flatten(((Map<String, Object>) input).get(prop), graphs, g, namer, null, null);
    			continue;
    		}
    		
    		// copy ignored keys
    		if (opts.isIgnored(prop)) {
    			subject.put(prop, ((Map<String, Object>) input).get(prop));
    			continue;
    		}
    		
    		// copy non-@type keywords
    		if (!"@type".equals(prop) && JSONLDUtils.isKeyword(prop)) {
    			subject.put(prop, ((Map<String, Object>) input).get(prop));
    			continue;
    		}
    		
    		// iterate over objects
    		Object objects = ((Map<String, Object>) input).get(prop);
    		Object[] keys = null;
    		int len = 0;
    		if (objects instanceof Map) {
    			keys = ((Map<String,Object>)objects).keySet().toArray();
    			len = keys.length;
    		} else {
    			len = ((List)objects).size();
    		}
    		for (int i = 0; i < len; i++) {
    			Object o;
    			if (objects instanceof Map) {
    				o = ((Map)objects).get(keys[i]);
    			} else {
    				o = ((List)objects).get(i);
    			}
    			
    			if (JSONLDUtils.isSubject(o) || JSONLDUtils.isSubjectReference(o)) {
    				// rename blank node @id
    				String id = (JSONLDUtils.isBlankNode(o) ? namer.getName((String) ((Map<String,Object>)o).get("@id")) : (String)((Map<String,Object>)o).get("@id"));
    				
    				// add reference and recurse
    				Map<String,Object> tmp = new HashMap<String, Object>();
    				tmp.put("@id", id);
    				JSONLDUtils.addValue(subject, prop, tmp, true);
    				flatten(o, graphs, graph, namer, id);
    			} else {
    				// recurse into list
    				if (o instanceof Map && ((Map)o).containsKey("@list")) {
    					List<Object> _list = new ArrayList<Object>();
    					flatten(((Map)o).get("@list"), graphs, graph, namer, name, _list);
    					o = new HashMap<String, Object>();
    					((Map<String, Object>) o).put("@list", _list);
    				// special-handle @type IRIs
    				} else if ("@type".equals(prop) && o instanceof String && ((String) o).startsWith("_:")) {
    					o = namer.getName((String) o);
    				}
    				
    				// add non-subject
    				JSONLDUtils.addValue(subject, prop, o, true);
    			}
    		}
    	}
    }
    
    private void flatten(Object input, Map<String,Object> graphs, String graph, UniqueNamer namer, String name) {
    	flatten(input, graphs, graph, namer, name, null);
    }
    
    private void flatten(Object input, Map<String,Object> graphs, String graph, UniqueNamer namer) {
    	flatten(input, graphs, graph, namer, null, null);
    }
    
    private class MapStatements implements JSONLDTripleCallback {

    	List<List<String>> statements = new ArrayList<List<String>>();
    	Map<String,Object> bnodes = new HashMap<String, Object>();
    	UniqueNamer namer = new UniqueNamer("_:t");
    	
		@Override
		public void triple(String s, String p, String o, String graph) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void triple(String s, String p, String value, String datatype,
				String language, String graph) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void triple(String s, String p, String o) {
			triple(s, p, o, null);
		}

		@Override
		public void triple(String s, String p, String value, String datatype,
				String language) {
			triple(s, p, value, datatype, language, null);
		}

		@Override
		public void processIgnored(Object parent, String parentId, String key,
				Object value) {
			// nothing to process
		}
    	
    }
	
	private interface CallbackWrapper {
		public void callback(Map<String,Object> statement);
		public void processIgnored(Object parent, String parentId, String prop, Object object);
	}
	
	private class ToRDFCallback implements CallbackWrapper {
		private JSONLDTripleCallback cb;
		public ToRDFCallback(JSONLDTripleCallback cb) {
			this.cb = cb;
		}
		public void callback(Map<String,Object> statement) {
			if (statement == null) {
				return;
			}
			Map<String,Object> s = (Map<String, Object>) statement.get("subject");
			Map<String,Object> p = (Map<String, Object>) statement.get("property");
			Map<String,Object> o = (Map<String, Object>) statement.get("object");
			Map<String,Object> g = (Map<String, Object>) statement.get("name");
			
			String sub = (String) s.get("nominalValue");
			String pre = (String) p.get("nominalValue");
			String obj = (String) o.get("nominalValue");
			
			String graph = null;
			if (g != null) {
				graph = (String) g.get("nominalValue");
			}
			
			if (o.containsKey("datatype") && 
					!XSD_STRING.equals(((Map<String, Object>) o.get("datatype")).get("nominalValue"))) {
				cb.triple(sub, pre, obj, (String) ((Map<String, Object>) o.get("datatype")).get("nominalValue"), null, graph);
			} else if (o.containsKey("language")) {
				cb.triple(sub, pre, obj, (String) null, (String)o.get("language"), graph);
			} else if ("LiteralNode".equals(o.get("interfaceName"))) {
				cb.triple(sub, pre, obj, null, null, graph);
			} else {
				cb.triple(sub, pre, obj, graph);
			}
		}
		@Override
		public void processIgnored(Object parent, String parentId, String prop, Object object) {
			cb.processIgnored(parent, parentId, prop, object);
		}
	}
	
	private class NormalizeCallback implements CallbackWrapper {
		
		List<Map<String,Object>> statements = new ArrayList<Map<String,Object>>();
		Map<String,Map<String,Object>> bnodes = new HashMap<String, Map<String,Object>>();
		UniqueNamer namer = new UniqueNamer("_:c14n");
					
		public void callback(Map<String, Object> statement) {
			if (statement == null) {
				finalise();
			}
			
			for (Map<String,Object> s: statements) {
				if (compareRdfStatements(s, statement)) {
					return;
				}
			}
			
			statements.add(statement);
			
			for (String n: new String[] { "subject", "object" }) {
				Map<String,Object> node = (Map<String, Object>) statement.get(n);
				String id = (String) node.get("nominalValue");
				if ("BlankNode".equals(node.get("interfaceName"))) {
					List<Object> stmts;
					if (bnodes.containsKey(id)) {
						stmts = (List<Object>) bnodes.get(id).get("statements");
					} else {
						stmts = new ArrayList<Object>();
						Map<String,Object> tmp = new HashMap<String, Object>();
						tmp.put("statements", stmts);
						bnodes.put(id, tmp);
					}
					stmts.add(statement);
				}
			}
		}
		
		public void hashBlankNodes(List<String> unnamed) {
			// generate unique and duplicate hashes for bnodes
			List<String> nextUnnamed = new ArrayList<String>();
			Map<String,List<String>> duplicates = new HashMap<String, List<String>>();
			Map<String,String> unique = new HashMap<String, String>();
			
			for (String bnode: unnamed) {

				String hash = hashStatements(bnode, bnodes, namer);
				
				// store hash as unique or a duplicate
				if (duplicates.containsKey(hash)) {
					duplicates.get(hash).add(bnode);
					nextUnnamed.add(bnode);
				} else if (unique.containsKey(hash)) {
					List<String> tmp = new ArrayList<String>();
					tmp.add(unique.get(hash));
					tmp.add(bnode);
					duplicates.put(hash, tmp);
					unique.remove(hash);
				} else {
					unique.put(hash, bnode);
				}
			}
			
			// name blank nodes
			nameBlankNodes(unique, duplicates, nextUnnamed);
		}

		private void nameBlankNodes(Map<String, String> unique,
				Map<String, List<String>> duplicates, List<String> unnamed) {
			boolean named = false;
			List<String> hashes = new ArrayList<String>(unique.keySet());
			Collections.sort(hashes);
			for (String hash: hashes) {
				String bnode = unique.get(hash);
				namer.getName(bnode);
				named = true;
			}
			
			if (named) {
				hashBlankNodes(unnamed);
			} else {
				nameDuplicates(duplicates);
			}
			
		}

		private void nameDuplicates(Map<String, List<String>> duplicates) {
			List<String> hashes = new ArrayList<String>(duplicates.keySet());
			Collections.sort(hashes);
			
			// process each group
			for (String hash: hashes) {
				// name each group member
				List<String> group = duplicates.get(hash);
				List<Map<String,Object>> results = new ArrayList<Map<String,Object>>();
				
				for (String bnode: group) {
					// skip already-named bnodes
					if (namer.isNamed(bnode)) {
						continue;
					}
					
					// hash bnode paths
					UniqueNamer pathNamer = new UniqueNamer("_:t");
					pathNamer.getName(bnode);
						try {
							// create SHA-1 digest
							MessageDigest md = MessageDigest.getInstance("SHA-1");
							
							// group adjacent bnodes by hash, keep properties and references separate
							Map<String,List<String>> groups = new HashMap<String, List<String>>();
							List<Object> statements = (List<Object>) bnodes.get(bnode).get("statements");
							
							for (Object statement: statements) {
								// get adjacent bnode
								String adjbnode = 
										("BlankNode".equals(((Map<String, Object>) ((Map<String, Object>) statement).get("subject")).get("interfaceName")) &&
											!bnode.equals(((Map<String, Object>) ((Map<String, Object>) statement).get("subject")).get("nominalValue"))) ?
													(String)((Map<String, Object>) ((Map<String, Object>) statement).get("subject")).get("nominalValue") : null;
								String direction = null;
								if (adjbnode != null) {
									direction = "p";
								} else {
									adjbnode = 
											("BlankNode".equals(((Map<String, Object>) ((Map<String, Object>) statement).get("object")).get("interfaceName")) &&
												!bnode.equals(((Map<String, Object>) ((Map<String, Object>) statement).get("object")).get("nominalValue"))) ?
														(String)((Map<String, Object>) ((Map<String, Object>) statement).get("object")).get("nominalValue") : null;
									if (adjbnode != null) {
										direction = "r";
									}
								}
								
								if (adjbnode != null) {
									// get bnode name (try canonical, path, then hash
									String name;
									if (namer.isNamed(adjbnode)) {
										name = namer.getName(adjbnode);
									} else if (pathNamer.isNamed(adjbnode)) {
										name = pathNamer.getName(adjbnode);
									} else {
										// TODO: _hashStatements(adjbnode, bnodes, namer);
										name = "";
									}
									
									MessageDigest md1 = MessageDigest.getInstance("SHA-1");
									md1.update(direction.getBytes());
									md1.update(((String) ((Map<String, Object>) ((Map<String, Object>) statement).get("property")).get("nominalValue")).getBytes());
									md1.update(name.getBytes());
									String groupHash = new String(md1.digest());
									
									if (groups.containsKey(groupHash)) {
										groups.get(groupHash).add(adjbnode);
									} else {
										List<String> tmp = new ArrayList<String>();
										tmp.add(adjbnode);
										groups.put(adjbnode, tmp);
									}
								}
							}
							
							// hashGroup: hashes a group of adjacent bnodes
							List<String> groupHashes = new ArrayList<String>(groups.keySet());
							Collections.sort(groupHashes);
							for (String groupHash: groupHashes) {
								// digest group hash
								md.update(groupHash.getBytes());
								
								// choose a path and namer from the permutations
								// TODO: impt Permutator
								// TODO: continue impl @ L2341 of jsonld.js
							}
							
						} catch (NoSuchAlgorithmException e) {
							throw new RuntimeException(e);
						}
				}
				
				// name bnodes in hash order
				Collections.sort(results, new Comparator<Map<String,Object>>() {
					@Override
					public int compare(Map<String, Object> o1,
							Map<String, Object> o2) {
						String a = (String) o1.get("hash");
						String b = (String) o2.get("hash");
						return a.compareTo(b);
					}
				});
				for (Map<String,Object> r: results) {
					// name all bnodes in path namer in key-entry order
		            // Note: key-order is preserved in javascript
					for (String key: ((UniqueNamer)r.get("pathNamer")).existing().keySet()) {
						namer.getName(key);
					}
				}
			}
			
			// done, create JSON-LD array
			createArray();
			
		}

		private void createArray() {
			// TODO Auto-generated method stub
			
		}

		public void finalise() {
			// TODO Auto-generated method stub
			hashBlankNodes(new ArrayList<String>(bnodes.keySet()));
		}

		@Override
		public void processIgnored(Object parent, String parentId, String prop, Object object) {
			// TODO Auto-generated method stub
		}
	}
	
	/**
	 * Hashes all of the statements about a blank node.
	 *
	 * @param id the ID of the bnode to hash statements for.
	 * @param bnodes the mapping of bnodes to statements.
	 * @param namer the canonical bnode namer.
	 *
	 * @return the new hash.
	 */
	private String hashStatements(String id, Map<String,Map<String,Object>> bnodes, UniqueNamer namer) {
		if (bnodes.get(id).containsKey("hash")) {
			return (String) bnodes.get("id").get("hash");
		}
		
		List<Map<String,Object>> statements = (List<Map<String, Object>>) bnodes.get(id).get("statements");
		List<String> nquads = new ArrayList<String>();
		for (Map<String,Object> statement: statements) {
			// TODO: this is _toNQuad, and some of the code is pointless in this 
			// case and some is duplicated code.
			Map<String,Object> s = (Map<String, Object>) statement.get("subject");
			Map<String,Object> p = (Map<String, Object>) statement.get("property");
			Map<String,Object> o = (Map<String, Object>) statement.get("object");
			Map<String,Object> g = (Map<String, Object>) statement.get("name");
			
			String quad = "";
			
			if ("IRI".equals(s.get("interfaceName"))) {
				quad += "<" + s.get("nominalValue") + ">";
			} else if (id != null) {
				quad += (id.equals(s.get("nominalValue")) ? "_:a" : "_:z");
			} else {
				quad += s.get("nominalValue");
			}
			
			quad += " <" + p.get("nominalValue") + "> ";
			
			if ("IRI".equals(o.get("interfaceName"))) {
				quad += "<" + o.get("nominalValue") + ">";
			} else if ("BlankNode".equals(o.get("interfaceName"))) {
				if (id != null) {
					quad += (id.equals(o.get("nominalValue")) ? "_:a" : "_:z");
				} else {
					quad += o.get("nominalValue");
				}
			} else {
				String escaped = ((String)o.get("nominalValue"))
						.replaceAll("\\\\", "\\\\\\\\")
						.replaceAll("\\t", "\\\\t")
						.replaceAll("\\n", "\\\\n")
						.replaceAll("\\r", "\\\\r")
						.replaceAll("\\\"", "\\\\\"");
				quad += "\"" + escaped + "\"";
				if (o.containsKey("datatype") && !XSD_STRING.equals(((Map<String, Object>) o.get("datatype")).get("nominalValue"))) {
					quad += "^^<" + ((Map<String, Object>) o.get("datatype")).get("nominalValue") + ">";
				} else if (o.containsKey("language")) {
					quad += "@" + o.get("language");
				}
			}
			
			if (g != null) {
				if ("IRI".equals(g.get("interfaceName"))) {
					quad += " <" + g.get("nominalValue") + ">";
				} else if (id != null) {
					quad += "_:g";
				} else {
					quad += " " + g.get("nominalValue");
				}
			}
			
			quad += " .";
			// END OF _toNQuad
			
			nquads.add(quad);
		}
		
		Collections.sort(nquads);
		String hash = "";
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			for (String nquad: nquads) {
				md.update(nquad.getBytes());
			}
			hash = new String(md.digest());
		} catch (NoSuchAlgorithmException e) {
			// TODO: i don't expect that SHA-1 is even NOT going to be available?
			// look into this further
			throw new RuntimeException(e);
		}
		return hash;
	}
	
	/**
	 * Compares two RDF statements for equality.
	 *
	 * @param s1 the first statement.
	 * @param s2 the second statement.
	 *
	 * @return true if the statements are the same, false if not.
	 */
	static boolean compareRdfStatements(Map<String,Object> s1, Map<String,Object> s2) {
		for (String attr: new String[] { "subject", "property", "object" }) {
			Object s1int = ((Map<String, Object>) s1.get(attr)).get("interfaceName");
			Object s2int = ((Map<String, Object>) s2.get(attr)).get("interfaceName");
			Object s1nom = ((Map<String, Object>) s1.get(attr)).get("nominalValue");
			Object s2nom = ((Map<String, Object>) s2.get(attr)).get("nominalValue");
			if ( !(s1int != null && s1int.equals(s2int) || s1int == null && s2int == null) ||
				 !(s1nom != null && s1nom.equals(s2nom) || s1nom == null && s2nom == null)) {
				return false;
			}
		}
		Object s1lang = ((Map<String, Object>) s1.get("object")).get("language");
		Object s2lang = ((Map<String, Object>) s2.get("object")).get("language");
		if (!(s1lang != null && s1lang.equals(s2lang) || s1lang == null && s2lang == null)) {
			return false;
		}
		
		if (((Map<String, Object>) s1.get("object")).containsKey("datatype") != ((Map<String, Object>) s2.get("object")).containsKey("datatype")) {
			return false;
		}
		
		if (((Map<String, Object>) s1.get("object")).containsKey("datatype")) {
			Object s1int = ((Map<String, Object>) s1.get("object")).get("interfaceName");
			Object s2int = ((Map<String, Object>) s2.get("object")).get("interfaceName");
			Object s1nom = ((Map<String, Object>) s1.get("object")).get("nominalValue");
			Object s2nom = ((Map<String, Object>) s2.get("object")).get("nominalValue");
			if ( !(s1int != null && s1int.equals(s2int) || s1int == null && s2int == null) ||
				 !(s1nom != null && s1nom.equals(s2nom) || s1nom == null && s2nom == null)) {
				return false;
			}
		}
		
		Object s1name = s1.get("name");
		Object s2name = s2.get("name");
		if (!(s1name != null && s1name.equals(s2name) || s1name == null && s2name == null)) {
			return false;
		}
		return true;
	}
	
	private static boolean compareRdfStatements(String s1, String s2) {
		return s1 != null && s1.equals(s2) || s1 == null && s2 == null;
	}
	
	 /**
     * Performs RDF normalization on the given JSON-LD input.
     *
     * @param input the expanded JSON-LD object to normalize.
     * @param options the normalization options.
     * @param callback(err, normalized) called once the operation completes.
     */
	public List normalize(Object input) {
		NormalizeCallback cb = new NormalizeCallback();
		_toRDF(input, new UniqueNamer("_:t"), null, null, null, cb);
		cb.finalise();
		return null;
	}
	
	/**
	 * Adds RDF triples for each graph in the given node map to an RDF dataset.
	 *
	 * @param nodeMap the node map.
	 *
	 * @return the RDF dataset.
	 */
	public Map<String,Object> toRDF(Map<String,Object> nodeMap) {
		UniqueNamer namer = new UniqueNamer("_:b");
		Map<String,Object> dataset = new HashMap<String, Object>();
		for (String graphName : nodeMap.keySet()) {
			Map<String,Object> graph = (Map<String, Object>) nodeMap.get(graphName);
			if (graphName.indexOf("_:") == 0) {
				graphName = namer.getName(graphName);
			}
			dataset.put(graphName, graphToRDF(graph, namer));
		}
		return dataset;
	}
	
	/**
	 * Outputs the RDF statements found in the given JSON-LD element.
	 *
	 * @param element the JSON-LD element.
	 * @param namer the UniqueNamer for assigning bnode names.
	 * @param subject the active subject.
	 * @param property the active property.
	 * @param graph the graph name.
	 * @param callback(err, statement) called when a statement is output, with the
	 *          last statement as null.
	 */
	public void toRDF(Object element, UniqueNamer namer, String subject, String property, Object graph, JSONLDTripleCallback callback) {
		CallbackWrapper cbw = new ToRDFCallback(callback);
		_toRDF(element, namer, subject, property, graph, cbw);
		cbw.callback(null);
	}
    
	/**
	 * Recursively outputs the RDF statements found in the given JSON-LD element.
	 *
	 * @param element the JSON-LD element.
	 * @param namer the UniqueNamer for assigning bnode names.
	 * @param subject the active subject.
	 * @param property the active property.
	 * @param graph the graph name.
	 * @param callback(err, statement) called when a statement is output, with the
	 *          last statement as null.
	 */
	private void _toRDF(Object elem, UniqueNamer namer, Object subject, Object property, Object graph, CallbackWrapper callback) {
		if (elem instanceof Map) {
			Map<String,Object> element = (Map<String, Object>) elem;
			if (element.containsKey("@value")) {
				Object value = element.get("@value");
				Object datatype = element.get("@type"); // || null;
				if (value instanceof Boolean || value instanceof Number) {
					// convert to XSD datatype
					if (value instanceof Boolean) {
						value = value.toString();
						if (datatype == null) {
							datatype = XSD_BOOLEAN;
						}
					} else if (value instanceof Double || value instanceof Float) {
						DecimalFormat df = new DecimalFormat("0.0###############E0");
						value = df.format(value);
						if (datatype == null) {
							datatype = XSD_DOUBLE;
						}
					} else {
						DecimalFormat df = new DecimalFormat("0");
						value = df.format(value);
						if (datatype == null) {
							datatype = XSD_INTEGER;
						}
					}
				}
				
				if (datatype == null) {
					datatype = XSD_STRING;
				}
				
				Map<String,Object> object = new HashMap<String, Object>();
				object.put("nominalValue", value);
				object.put("interfaceName", "LiteralNode");
				Map<String,Object> objdt = new HashMap<String, Object>();
				objdt.put("nominalValue", datatype);
				objdt.put("intergaceName", "IRI");
				object.put("datatype", objdt);
				
				if (element.containsKey("@language") && XSD_STRING.equals(datatype)) {
					object.put("language", element.get("@language"));
				}
				
				// emit literal
				Map<String,Object> statement = new HashMap<String, Object>();
				statement.put("subject", JSONLDUtils.clone(subject));
				statement.put("property", JSONLDUtils.clone(property));
				statement.put("object", object);
				
				if (graph != null) {
					statement.put("name", graph);
				}
				
				// TODO:
				callback.callback(statement);
				return;
			}
			
			if (element.containsKey("@list")) {
				// convert @list array into embedded blank node linked list in reverse
				List<Object> list = (List<Object>) element.get("@list");
				int len = list.size();
				Map<String,Object> tail = new HashMap<String, Object>();
				tail.put("@id", RDF_NIL);
				for (int i = len - 1; i >= 0; --i) {
					Map<String,Object> e = new HashMap<String, Object>();
					List<Object> f = new ArrayList<Object>();
					f.add(list.get(i));
					List<Object> r = new ArrayList<Object>();
					r.add(tail);
					e.put(RDF_FIRST, f);
					e.put(RDF_REST, r);
					tail = e;
				}
				
				_toRDF(tail, namer, subject, property, graph, callback);
				return;
			}
			
			// Note: element must be a subject
			
			// get subject @id (generate one if it is a bnode)
			Boolean isBnode = JSONLDUtils.isBlankNode(element);
			String id = isBnode ? namer.getName((String) element.get("@id")) : (String)element.get("@id");
						
			// create object 
			Map<String,Object> object = new HashMap<String, Object>();
			object.put("nominalValue", id);
			object.put("interfaceName", isBnode ? "BlankNode" : "IRI");
			
			if (subject != null) {
				Map<String,Object> statement = new HashMap<String, Object>();
				statement.put("subject", JSONLDUtils.clone(subject));
				statement.put("property", JSONLDUtils.clone(property));
				statement.put("object", object);
				
				if (graph != null) {
					statement.put("name", graph);
				}
				callback.callback(statement);
			}
			
			// set new active subject to object
			subject = object;
			
			// recurse over subject properties in order
			List<String> props = new ArrayList<String>(element.keySet());
			Collections.sort(props);
			for (String prop: props) {
				
				// skip ignored keys
				if (opts.isIgnored(prop)) {
					
					// TODO: I need the id of the parent object, even if it's a
					// blank node, so i'm passing the subject value here, where
					// element makes more sense (but does not have to contain
					// the @id, or in the cases of blank nodes, may not have the
					// same @id as the resulting rdf.
					callback.processIgnored(subject, id, prop, element.get(prop));
					
					continue;
				}
				
				Object e = element.get(prop);
				
				// convert @type to rdf:type
				if ("@type".equals(prop)) {
					prop = RDF_TYPE;
				}
				
				// recurse into @graph
				if ("@graph".equals(prop)) {
					_toRDF(e, namer, null, null, subject, callback);
					continue;
				}
				
				// skip keywords
				if (JSONLDUtils.isKeyword(prop)) {
					continue;
				}
				
				// create new active property
				property = new HashMap<String, Object>();
				((Map<String, Object>) property).put("nominalValue", prop);
				((Map<String, Object>) property).put("interfaceName", "IRI");
				
				// recurse into value
				_toRDF(e, namer, subject, property, graph, callback);
			}
			
			return;
		}
		
		if (elem instanceof List) {
			// recurse into arrays
			for (Object element: (List)elem) {
				_toRDF(element, namer, subject, property, graph, callback);
			}
			return;
		}
		
		// element must be an rdf:type IRI (@values covered above)
		if (elem instanceof String) {
			// emit IRI
			Map<String,Object> object = new HashMap<String, Object>();
			object.put("nominalValue", elem);
			object.put("interfaceName", "IRI");
			Map<String,Object> statement = new HashMap<String, Object>();
			statement.put("subject", JSONLDUtils.clone(subject));
			statement.put("property", JSONLDUtils.clone(property));
			statement.put("object", object);
			
			if (graph != null) {
				statement.put("name", graph);
			}
			
			callback.callback(statement);
			return;
		}
	}
	
	/**
	 * Converts RDF statements into JSON-LD.
	 *
	 * @param statements the RDF statements.
	 * @param options the RDF conversion options.
	 * @param callback(err, output) called once the operation completes.
	 * @throws JSONLDProcessingError 
	 */
	public Object fromRDF(List<Map<String, Object>> statements) throws JSONLDProcessingError {
		Map<String,Object> defaultGraph = new HashMap<String, Object>();
		defaultGraph.put("subjects", new HashMap<String, Object>());
		defaultGraph.put("listMap", new HashMap<String, Object>());
		Map<String,Map<String,Object>> graphs = new HashMap<String, Map<String,Object>>();
		graphs.put("", (Map<String,Object>)defaultGraph);
		
		for (Map<String,Object> statement: statements) {
			// get subject, property, object, and graph name (default to '')
			String s = (String)Obj.get(statement, "subject", "nominalValue");
			String p = (String)Obj.get(statement, "property", "nominalValue");
			Map<String,Object> o = (Map<String, Object>) statement.get("object");
			String name = statement.containsKey("name") ? (String)Obj.get(statement, "name", "nominalValue") : "";
			
			// create a graph entry as needed
			Map<String,Object> graph;
			if (!graphs.containsKey(name)) {
				graph = new HashMap<String,Object>();
				graph.put("subjects", new HashMap<String, Object>());
				graph.put("listMap", new HashMap<String, Object>());
				graphs.put(name, graph);
			} else {
				graph = graphs.get(name);
			}
			
			// handle element in @list
			if (RDF_FIRST.equals(p)) {
				// create list entry as needed
				Map<String,Object> listMap = (Map<String, Object>) graph.get("listMap");
				Map<String,Object> entry;
				if (!listMap.containsKey(s)) {
					entry = new HashMap<String, Object>();
					listMap.put(s, entry);
				} else {
					entry = (Map<String, Object>) listMap.get(s);
				}
				// set object value
				entry.put("first", rdfToObject(o));
				continue;
			}
			
			// handle other element in @list
			if (RDF_REST.equals(p)) {
				// set next in list
				if ("BlankNode".equals(o.get("interfaceName"))) {
					// create list entry as needed
					Map<String,Object> listMap = (Map<String, Object>) graph.get("listMap");
					Map<String,Object> entry;
					if (!listMap.containsKey(s)) {
						entry = new HashMap<String, Object>();
						listMap.put(s, entry);
					} else {
						entry = (Map<String, Object>) listMap.get(s);
					}
					// set object value
					entry.put("rest", o.get("nominalValue"));
				}
				continue;
			}
			
			// add graph subject to default graph as needed
			if (!"".equals(name) && !Obj.contains(defaultGraph, "subjects", name)) {
				Map<String,Object> tmp = new HashMap<String, Object>();
				tmp.put("@id", name);
				Obj.put(defaultGraph, "subjects", name, tmp);
			}
			
			// add subject to graph as needed
			Map<String,Object> subjects = (Map<String, Object>) graph.get("subjects");
			Map<String,Object> value;
			if (!subjects.containsKey(s)) {
				value = new HashMap<String, Object>();
				value.put("@id", s);
				subjects.put(s, value);
			} else {
				value = (Map<String, Object>) subjects.get(s);
			}
			
			// convert to @type unless options indicate to treat rdf:type as a property
			if (RDF_TYPE.equals(p) && !opts.useRdfType) {
				// add value of object as @type
				JSONLDUtils.addValue(value, "@type", o.get("nominalValue"), true);
			} else {
				// add property to value as needed
				Object object = rdfToObject(o);
				JSONLDUtils.addValue(value, p, object, true);
				
				// a bnode might be the beginning of a list, so add it to the list map
				if ("BlankNode".equals(o.get("interfaceName"))) {
					String id = (String) Obj.get(object, "@id");
					Map<String,Object> listMap = (Map<String, Object>) graph.get("listMap");
					Map<String,Object> entry;
					if (!listMap.containsKey(id)) {
						entry = new HashMap<String, Object>();
						listMap.put(id, entry);
					} else {
						entry = (Map<String, Object>) listMap.get(id);
					}
					entry.put("head", object);
				}
			}
		}
		
		// build @lists
		for (String name: graphs.keySet()) {
			Map<String,Object> graph = graphs.get(name);
			
			// find list head
			Map<String,Object> listMap = (Map<String, Object>) graph.get("listMap");
			for (String subject: listMap.keySet()) {
				Map<String,Object> entry = (Map<String, Object>) listMap.get(subject);
				
				// head found, build lists
				if (entry.containsKey("head") && entry.containsKey("first")) {
					// replace bnode @id with @list
					Obj.remove(entry, "head", "@id");
					List<Object> list = new ArrayList<Object>();
					list.add(entry.get("first"));
					Obj.put(entry, "head", "@list", list);
					while (entry.containsKey("rest")) {
						String rest = (String) entry.get("rest");
						entry = (Map<String, Object>) listMap.get(rest);
						if (!entry.containsKey("first")) {
							throw new JSONLDProcessingError("Invalid RDF list entry.)")
									.setType(JSONLDProcessingError.Error.RDF_ERROR)
									.setDetail("bnode", rest);
						}
						list.add(entry.get("first"));
					}
				}
			}
		}
		
		// build default graph in subject @id order
		List<Object> output = new ArrayList<Object>();
		Map<String,Object> subjects = (Map<String, Object>) defaultGraph.get("subjects");
		List<String> ids = new ArrayList<String>(subjects.keySet());
		Collections.sort(ids);
		for (String id: ids) {
			// add subject to default graph
			Map<String,Object> subject = (Map<String, Object>) subjects.get(id);
			output.add(subject);
			
			// output named graph in subject @id order
			if (graphs.containsKey(id)) {
				List<Object> graph = new ArrayList<Object>();
				subject.put("@graph", graph);
				Map<String,Object> _subjects = (Map<String, Object>) Obj.get(graphs, id, "subjects");
				List<String> _ids = new ArrayList<String>(_subjects.keySet());
				for (String _id: _ids) {
					graph.add(_subjects.get(_id));
				}
			}
		}
		
		return output;
	}
	
	/**
	 * Converts an RDF statement object to a JSON-LD object.
	 *
	 * @param o the RDF statement object to convert.
	 * @param useNativeTypes true to output native types, false not to.
	 *
	 * @return the JSON-LD object.
	 */
	private Object rdfToObject(Map<String, Object> o) {
		Map<String,Object> rval = new HashMap<String, Object>();
		
		// convert empty list
		if ("IRI".equals(o.get("interfaceName")) && RDF_NIL.equals(o.get("nominalValue"))) {
			rval.put("@list", new ArrayList<Object>());
			return rval;
		}
		
		// convert IRI/BlankNode object to JSON-LD
		if ("IRI".equals(o.get("interfaceName")) || "BlankNode".equals(o.get("interfaceName"))) {
			rval.put("@id", o.get("nominalValue"));
			return rval;
		}
		
		// convert literal object to JSON-LD
		rval.put("@value", o.get("nominalValue"));
		
		// add datatype
		if (o.containsKey("datatype")) {
			String type = (String) Obj.get(o, "datatype", "nominalValue");
			if (opts.useNativeTypes) {
				// use native datatypes for certain xsd types
				if (XSD_BOOLEAN.equals(type)) {
					if ("true".equals(rval.get("@value"))) {
						rval.put("@value", Boolean.TRUE);
					} else if ("false".equals(rval.get("@value"))) {
						rval.put("@value", Boolean.FALSE);
					}
				} else if (Pattern.matches("^[+-]?[0-9]+((?:\\.?[0-9]+((?:E?[+-]?[0-9]+)|)|))$", (String)rval.get("@value"))){
					try {
						Double d = Double.parseDouble((String)rval.get("@value"));
						if (!Double.isNaN(d) && !Double.isInfinite(d)) {
							if (XSD_INTEGER.equals(type)) {
								Integer i = d.intValue();
								if (i.toString().equals(rval.get("@value"))) {
									rval.put("@value", i);
								}
							} else if (XSD_DOUBLE.equals(type)) {
								rval.put("@value", d);
							} else {
								// we don't know the type, so we should add it to the JSON-LD
								rval.put("@type", type);
							}
						}
					} catch (NumberFormatException e) {
						// TODO: This should never happen since we match the value with regex!
					}
				}
				// do not add xsd:string type
				else if (!XSD_STRING.equals(type)) {
					rval.put("@type", type);
				}
			} else {
				rval.put("@type", type);
			}
		}
		// add language
		if (o.containsKey("language")) {
			rval.put("@language", o.get("language"));
		}
		
		return rval;
	}

	/**
     * Generates a unique simplified key from a URI and add it to the context 
     * 
     * @param key to full URI to generate the simplified key from
     * @param ctx the context to add the simplified key too
     * @param isid whether to set the type to @id
     */
    private static void processKeyVal(Map<String, Object> ctx, String key, Object val) {
        int idx = key.lastIndexOf('#');
        if (idx < 0) {
            idx = key.lastIndexOf('/');
        }
        String skey = key.substring(idx + 1);
        Object keyval = key;
        Map entry = new HashMap();
        entry.put("@id", keyval);
        Object v = val;
    	while (true) {
    		if (v instanceof List && ((List)v).size() > 0) {
    			// use the first entry as a reference
    			v = ((List)v).get(0);
    			continue;
    		}
    		if (v instanceof Map && ((Map)v).containsKey("@list")) {
    			v = ((Map)v).get("@list");
    			entry.put("@container", "@list");
    			continue;
    		}
    		if (v instanceof Map && ((Map)v).containsKey("@set")) {
    			v = ((Map)v).get("@set");
    			entry.put("@container", "@set");
    			continue;
    		}
    		break;
    	}
    	if (v instanceof Map && ((Map) v).containsKey("@id")) {
    		entry.put("@type", "@id");
    	}
    	if (entry.size() == 1) {
    		keyval = entry.get("@id");
    	} else {
    		keyval = entry;
    	}
        while (true) {
            // check if the key is already in the frame ctx
            if (ctx.containsKey(skey)) {
                // if so, check if the values are the same
                if (JSONUtils.equals(ctx.get(skey), keyval)) {
                    // if they are, skip adding this
                    break;
                }
                // if not, add a _ to the simple key and try again
                skey += "_";
            } else {
                ctx.put(skey, keyval);
                break;
            }
        }
    }
    
    /**
     * Performs JSON-LD flattening.
     *
     * @param input the expanded JSON-LD to flatten.
     *
     * @return the flattened output.
     * @throws JSONLDProcessingError 
     */
	public List<Object> flatten(List<Object> input) throws JSONLDProcessingError {
		// produce a map of all subjects and name each bnode
		UniqueNamer namer = new UniqueNamer("_:b");
		Map<String,Object> graphs = new HashMap<String, Object>() {{
			put("@default", new HashMap<String, Object>());
		}};
		createNodeMap(input, graphs, "@default", namer);
		
		// add all non-default graphs to default graph
		Map<String,Object> defaultGraph = (Map<String, Object>) graphs.get("@default");
		List<String> graphNames = new ArrayList<String>(graphs.keySet());
		Collections.sort(graphNames);
		for (String graphName : graphNames) {
			if ("@default".equals(graphName)) {
				continue;
			}
			Map<String,Object> nodeMap = (Map<String, Object>) graphs.get(graphName);
			Map<String,Object> subject = (Map<String, Object>) defaultGraph.get(graphName);
			if (subject == null) {
				subject = new HashMap<String, Object>();
				subject.put("@id", graphName);
				subject.put("@graph", new ArrayList<Object>());
				defaultGraph.put(graphName, subject);
			}
			else if (!subject.containsKey("@graph")) {
				subject.put("@graph", new ArrayList<Object>());
			}
			List<Object> graph = (List<Object>) subject.get("@graph");
			List<String> ids = new ArrayList<String>(nodeMap.keySet());
			for (String id : ids) {
				graph.add(nodeMap.get(id));
			}
		}
		
		// produce flattened output
		List<Object> flattened = new ArrayList<Object>();
		List<String> keys = new ArrayList<String>(defaultGraph.keySet());
		Collections.sort(keys);
		for (String key : keys) {
			flattened.add(defaultGraph.get(key));
		}
		return flattened;
	}

	/**
     * Generates the context to be used by simplify.
     * 
     * @param input
     * @param ctx
     */
    private static void generateSimplifyContext(Object input, Map<String, Object> ctx) {
        if (input instanceof List) {
            for (Object o : (List) input) {
                generateSimplifyContext(o, ctx);
            }
        } else if (input instanceof Map) {
            Map<String, Object> o = (Map<String, Object>) input;
            Map<String, Object> localCtx = (Map<String, Object>) o.remove("@context");
            for (String key : o.keySet()) {
                Object val = o.get(key);
                if (key.matches("^https?://.+$")) {
                	processKeyVal(ctx, key, val);
                }
                if ("@type".equals(key)) {
                    if (!(val instanceof List)) {
                        List<Object> tmp = new ArrayList<Object>();
                        tmp.add(val);
                        val = tmp;
                    }
                    for (Object t : (List<Object>) val) {
                        if (t instanceof String) {
                            processKeyVal(ctx, (String) t, new HashMap<String,Object>() {{
                            	put("@id", "");
                            }});
                        } else {
                            throw new RuntimeException("TODO: don't yet know how to handle non-string types in @type");
                        }
                    }
                } else if (val instanceof Map || val instanceof List) {
                    generateSimplifyContext(val, ctx);
                }
            }
        }
    }

    /**
     * Automatically builds a context which attempts to simplify the keys and values as much as possible
     * and uses that context to compact the input
     * 
     * NOTE: this is experimental and only built for specific conditions
     * 
     * @param input
     * @return the simplified version of input
     * @throws JSONLDProcessingError 
     */
    public Object simplify(Object input) throws JSONLDProcessingError {

        Object expanded = JSONLD.expand(input, opts);
        Map<String, Object> ctx = new HashMap<String,Object>();
        
        generateSimplifyContext(expanded, ctx);

        Map<String,Object> tmp = new HashMap<String, Object>();
        tmp.put("@context", ctx);
        
        // add optimize flag to opts (clone the opts so we don't change the flag for the base processor)
        Options opts1 = opts.clone();
        //opts1.optimize = true;
        return JSONLD.compact(input, tmp, opts1);
    }
    
}