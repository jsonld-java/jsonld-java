package com.github.jsonldjava.impl;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import com.github.jsonldjava.core.JSONLD;
import com.github.jsonldjava.core.JSONLDProcessor.Options;
import com.github.jsonldjava.utils.JSONUtils;


public class Playground {

    static boolean validOption(String opt) {
        return "--ignorekeys".equals(opt) || "--ignorekeys".equals(opt) || "--expand".equals(opt) || "--compact".equals(opt) || "--frame".equals(opt) || "--normalize".equals(opt)
                || "--simplify".equals(opt) || "--debug".equals(opt);
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
                Object frobj = null;
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
                        inobj = JSONUtils.fromInputStream(new FileInputStream(in));
                        if ("--frame".equals(args[i - 2])) {
                            if (args.length <= i) {
                                System.out.println("Error: missing frame file");
                                usage();
                                return;
                            }
                            in = new File(args[i++]);
                            if (!in.exists()) {
                                System.out.println("Error: file \"" + args[i - 1] + "\" doesn't exist");
                                usage();
                                return;
                            }
                            frobj = JSONUtils.fromInputStream(new FileInputStream(in));
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
                    outobj = JSONLD.compact(inobj, new HashMap<String, Object>(), opts);
                } else if ("--normalize".equals(opt)) {
                    //outobj = p.normalize(inobj);
                } else if ("--frame".equals(opt)) {
                    if (frobj == null) {
                        System.out.println("Error: no frame file specified");
                        usage();
                        return;
                    } else {
                        outobj = JSONLD.frame(inobj, frobj, opts);
                    }
                } else if ("--simplify".equals(opt)) {
                    outobj = JSONLD.simplify((Map) inobj, opts);
                } else {
                    System.out.println("Error: invalid option \"" + opt + "\"");
                    usage();
                    return;
                }

                System.out.println(JSONUtils.toPrettyString(outobj));
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
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
        System.out.println("\t\t--expand <input>: expand the input jsonld");
        System.out.println("\t\t--compact <input> : compact the input jsonld");
        System.out.println("\t\t--normalize <input> : normalize the input jsonld");
        System.out.println("\t\t--frame <input> <frame> : frame the input jsonld with the frame file");
        System.out.println("\t\t--simplify : simplify the input jsonld");
        System.exit(1);
    }
}
