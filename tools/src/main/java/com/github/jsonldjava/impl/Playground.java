package com.github.jsonldjava.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.github.jsonldjava.core.JSONLD;
import com.github.jsonldjava.core.JSONLDProcessingError;
import com.github.jsonldjava.core.Options;
import com.github.jsonldjava.utils.JSONUtils;


public class Playground {

    static boolean validOption(String opt) {
        return "--ignorekeys".equals(opt) || "--expand".equals(opt) || "--compact".equals(opt) || "--frame".equals(opt) || "--normalize".equals(opt)
                || "--simplify".equals(opt) || "--debug".equals(opt) || "--base".equals(opt) || "--flatten".equals(opt) || "--fromRDF".equals(opt)
                || "--toRDF".equals(opt);
    }

    static boolean hasContext(String opt) {
    	return "--compact".equals(opt) || "--frame".equals(opt) || "--flatten".equals(opt);
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        boolean debug = false;
        try {
            if (args.length < 2 || !args[0].startsWith("--")) {
                usage();
            } else {

                Options opts = new Options("");
                Object inobj = null;
                Object ctxobj = null;
                String opt = null;
                for (int i = 0; i < args.length;) {
                    if ("--debug".equals(args[i])) {
                        i++;
                        debug = true;
                    } else if ("--ignorekeys".equals(args[i])) {
                        i++;
                        while (i < args.length && !validOption(args[i])) {
                            opts.ignoreKey(args[i++]);
                        }
                    } else if ("--base".equals(args[i])) {
                    	opts.base = args[i++];
                    } else if (validOption(args[i])) {
                        if (opt != null) {
                            System.out.println("Error: can only do one operation on the input at a time");
                            usage();
                            return;
                        }
                        opt = args[i];
                        i++;
                        if (args.length <= i) {
                            System.out.println("Error: missing file names after argument " + args[i - 1]);
                            usage();
                            return;
                        }
                        File in = new File(args[i++]);
                        if (!in.exists()) {
                            System.out.println("Error: file \"" + args[i - 1] + "\" doesn't exist");
                            usage();
                            return;
                        }
                        // if base is currently null, set it
                        if (opts.base == null || opts.base.equals("")) {
                        	opts.base = in.toURI().toASCIIString();
                        }
                        if ("--fromRDF".equals(opt)) {
                        	BufferedReader buf = new BufferedReader(new InputStreamReader(new FileInputStream(in), "UTF-8"));
                        	inobj = "";
        	                String line;
        	                while ((line = buf.readLine()) != null) {
        	                    line = line.trim();
        	                    if (line.length() == 0 || line.charAt(0) == '#') {
        	                        continue;
        	                    }
        	                    inobj = ((String)inobj) + line + "\n";
        	                }
                        	
                        } else {
                        	inobj = JSONUtils.fromInputStream(new FileInputStream(in));
                        }
                        if ("--fromRDF".equals(opt) || "--toRDF".equals(opt) || "--normalize".equals(opt)) {
                        	// get format option
                        	if (args.length > i && !args[i].startsWith("--")) {
                        		opts.format = args[i++];
                        		// remove any quotes
                        		if (Pattern.matches("^['\"`].*['\"`]$", opts.format)) {
                        			opts.format = opts.format.substring(1, opts.format.length()-1);
                        		}
                        	}
                        	// default to nquads
                        	if (opts.format == null || "null".equals(opts.format)) {
                        		opts.format = "application/nquads";
                        	}
                        } else if (hasContext(opt)) {
                            if (args.length > i) {
	                                
	                            in = new File(args[i++]);
	                            if (!in.exists()) {
	                            	if (args[i-1].startsWith("--")) {
	                            		// the frame is optional, so if it turns out we have another option after the --frame options
	                            		// we have to make sure we process it
	                            		i--;
	                            	} else {
		                                System.out.println("Error: file \"" + args[i - 1] + "\" doesn't exist");
		                                usage();
		                                return;
	                            	}
	                            }
	                            ctxobj = JSONUtils.fromInputStream(new FileInputStream(in));
                            }
                        }
                    } else {
                        System.out.println("Invalid option: " + args[i]);
                        usage();
                        return;
                    }
                }

                if (opt == null) {
                    System.out.println("Error: missing processing option");
                    usage();
                    return;
                }

                Object outobj = null;
                if ("--expand".equals(opt)) {
                    outobj = JSONLD.expand(inobj, opts);
                } else if ("--compact".equals(opt)) {
                	if (ctxobj == null) {
                		System.out.println("Error: The compaction context must not be null.");
                        usage();
                        return;
                	}
                    outobj = JSONLD.compact(inobj, ctxobj, opts);
                } else if ("--normalize".equals(opt)) {
                    outobj = JSONLD.normalize(inobj, opts);
                } else if ("--frame".equals(opt)) {
                    if (ctxobj != null && !(ctxobj instanceof Map)) {
                    	System.out.println("Invalid JSON-LD syntax; a JSON-LD frame must be a single object.");
                        usage();
                        return;
                    }
                    outobj = JSONLD.frame(inobj, (Map<String,Object>)ctxobj, opts);
                } else if ("--flatten".equals(opt)) {
                	outobj = JSONLD.flatten(inobj, ctxobj, opts);
                } else if ("--toRDF".equals(opt)) {
                	outobj = JSONLD.toRDF(inobj, opts);
                } else if ("--fromRDF".equals(opt)) {
                	outobj = JSONLD.fromRDF(inobj, opts);
                } else if ("--simplify".equals(opt)) {
                    outobj = JSONLD.simplify(inobj, opts);
                } else {
                    System.out.println("Error: invalid option \"" + opt + "\"");
                    usage();
                    return;
                }

                if ("--toRDF".equals(opt) || "--normalize".equals(opt)) {
                	System.out.println((String)outobj);
                } else {
                	System.out.println(JSONUtils.toPrettyString(outobj));
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            if (e instanceof JSONLDProcessingError) {
            	for (Entry<String,Object> detail : ((JSONLDProcessingError)e).getDetails().entrySet()) {
            		System.out.println(detail.getKey() + ": " + detail.getValue());
            	}
            }
            if (debug) {
                e.printStackTrace();
            }
            usage();
            return;
        }
    }

    private static void usage() {
        System.out.println("Usage: jsonldplayground <options>");
        System.out.println("\tinput: a filename or URL to the rdf input (in rdfxml or n3)");
        System.out.println("\toptions:");
        System.out.println("\t\t--ignorekeys <keys to ignore> : a (space separated) list of keys to ignore (e.g. @geojson)");
        System.out.println("\t\t--base <uri>: base URI");
        System.out.println("\t\t--debug: Print out stack traces when errors occur");
        System.out.println("\t\t--expand <input>: expand the input  JSON-LD");
        System.out.println("\t\t--compact <input> <context> : compact the input JSON-LD applying the optional context file");
        System.out.println("\t\t--normalize <input> <format> : normalize the input JSON-LD outputting as format (defaults to nquad)");
        System.out.println("\t\t--frame <input> <frame> : frame the input JSON-LD with the optional frame file");
        System.out.println("\t\t--flatten <input> <context> : flatten the input JSON-LD applying the optional context file");
        System.out.println("\t\t--fromRDF <input> <format> : generate JSON-LD from the input rdf (format defaults to nquads)");
        System.out.println("\t\t--toRDF <input> <format> : generate RDF from the input JSON-LD (format defaults to nquads)");
        System.out.println("\t\t--simplify : simplify the input JSON-LD");
        System.exit(1);
    }
}
