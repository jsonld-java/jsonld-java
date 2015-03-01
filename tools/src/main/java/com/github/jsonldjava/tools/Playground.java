package com.github.jsonldjava.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParserRegistry;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.ValueConversionException;
import joptsimple.ValueConverter;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

public class Playground {

    private static boolean validOption(String opt) {
        return "--expand".equals(opt) || "--compact".equals(opt)
                || "--frame".equals(opt) || "--normalize".equals(opt) || "--simplify".equals(opt)
                || "--flatten".equals(opt) || "--fromRDF".equals(opt) || "--toRDF".equals(opt);
    }

    private static boolean hasContext(String opt) {
        return "--compact".equals(opt) || "--frame".equals(opt) || "--flatten".equals(opt);
    }

    private static Map<String, RDFFormat> getOutputFormats() {
        Map<String, RDFFormat> outputFormats = new HashMap<String, RDFFormat>();
        
        for(RDFFormat format : RDFParserRegistry.getInstance().getKeys()) {
            outputFormats.put(format.getName().replaceAll("-", "").replaceAll("/", "").toLowerCase(), format);
        }
        
        return outputFormats;
    }
    
    public static void main(String[] args) throws Exception {
        
        final Map<String, RDFFormat> formats = getOutputFormats();
        final Set<String> outputForms = new LinkedHashSet<String>(Arrays.asList("compacted", "expanded", "flattened"));
        
        final OptionParser parser = new OptionParser();
        
        final OptionSpec<Void> help = parser.accepts("help").forHelp();
        
        final OptionSpec<String> base = parser.accepts("base")
                        .withRequiredArg()
                        .ofType(String.class)
                        .describedAs("base URI");
        
        final OptionSpec<File> inputFile =
                parser.accepts("inputFile").withRequiredArg().ofType(File.class)
                        .describedAs("The input file");
        
        final OptionSpec<File> context =
                parser.accepts("context").withRequiredArg().ofType(File.class)
                        .describedAs("The context");
        
        final OptionSpec<RDFFormat> outputFormat =
                parser.accepts("format")
                        .withOptionalArg()
                        .ofType(String.class)
                        .defaultsTo(RDFFormat.NQUADS.getName())
                        .withValuesConvertedBy(new ValueConverter<RDFFormat>() {
                            @Override
                            public RDFFormat convert(String arg0) {
                                // Normalise the name to provide alternatives
                                String formatName = arg0.replaceAll("-", "").replaceAll("/", "").toLowerCase();
                                if(formats.containsKey(formatName)) {
                                   return formats.get(formatName);
                                }
                                throw new ValueConversionException("Format was not known: " + arg0);
                            }

                            @Override
                            public String valuePattern() {
                                return null;
                            }

                            @Override
                            public Class<RDFFormat> valueType() {
                                return RDFFormat.class;
                            }
                        })
                        .describedAs(
                                "The output file format to use. Defaults to nquads.");
        
        final OptionSpec<String> processingOption = parser.accepts("process")
                .withRequiredArg()
                .ofType(String.class)
                .required()
                .withValuesConvertedBy(new ValueConverter<String>() {
                    @Override
                    public String convert(String value) {
                        if(validOption(value.toLowerCase())) {
                            return value.toLowerCase();
                        }
                        throw new ValueConversionException("Processing option was not known: " + value);
                    }

                    @Override
                    public Class<String> valueType() {
                        return String.class;
                    }

                    @Override
                    public String valuePattern() {
                        return null;
                    }
                })
                
                ;
        
        final OptionSpec<String> outputForm = parser.accepts("outputForm")
                .withOptionalArg()
                .ofType(String.class)
                .defaultsTo("expanded")
                .withValuesConvertedBy(new ValueConverter<String>() {
                    @Override
                    public String convert(String value) {
                        if(outputForms.contains(value.toLowerCase())) {
                            return value.toLowerCase();
                        }
                        throw new ValueConversionException("Output form was not known: " + value);
                    }

                    @Override
                    public String valuePattern() {
                        return null;
                    }

                    @Override
                    public Class<String> valueType() {
                        return String.class;
                    }
                })
                .describedAs("outputForm");

        OptionSet options = null;
        
        try
        {
            options = parser.parse(args);
        }
        catch(final OptionException e)
        {
            System.out.println(e.getMessage());
            parser.printHelpOn(System.out);
            throw e;
        }
        
        if(options.has(help))
        {
            parser.printHelpOn(System.out);
            return;
        }

        final JsonLdOptions opts = new JsonLdOptions("");
        Object inobj = null;
        Object ctxobj = null;
        String opt = null;
        
        if(options.has(base)) {
            opts.setBase(options.valueOf(base));
        }
        
        if(options.has(outputForm)) {
            opts.outputForm = options.valueOf(outputForm);
        }
        
        if(options.has(outputFormat)) {
            opts.format = options.valueOf(outputFormat).getDefaultMIMEType();
        }
        
        
            opt = args[i];
            i++;
            if (args.length <= i) {
                System.out.println("Error: missing file names after argument "
                        + args[i - 1]);
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
            if (opts.getBase() == null || opts.getBase().equals("")) {
                opts.setBase(in.toURI().toASCIIString());
            }
            if ("--fromRDF".equals(opt)) {
                final BufferedReader buf = new BufferedReader(new InputStreamReader(
                        new FileInputStream(in), "UTF-8"));
                inobj = "";
                String line;
                while ((line = buf.readLine()) != null) {
                    line = line.trim();
                    if (line.length() == 0 || line.charAt(0) == '#') {
                        continue;
                    }
                    inobj = ((String) inobj) + line + "\n";
                }

            } else {
                inobj = JsonUtils.fromInputStream(new FileInputStream(in));
            }
            if ("--fromRDF".equals(opt) || "--toRDF".equals(opt)
                    || "--normalize".equals(opt)) {
                // get format option
                if (args.length > i && !args[i].startsWith("--")) {
                    opts.format = args[i++];
                    // remove any quotes
                    if (Pattern.matches("^['\"`].*['\"`]$", opts.format)) {
                        opts.format = opts.format
                                .substring(1, opts.format.length() - 1);
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
                        if (args[i - 1].startsWith("--")) {
                            // the frame is optional, so if it turns
                            // out we have another option after the
                            // --frame options
                            // we have to make sure we process it
                            i--;
                        } else {
                            System.out.println("Error: file \"" + args[i - 1]
                                    + "\" doesn't exist");
                            usage();
                            return;
                        }
                    }
                    ctxobj = JsonUtils.fromInputStream(new FileInputStream(in));
                }
            }
        }

        if (opt == null) {
            System.out.println("Error: missing processing option");
            usage();
            return;
        }

        Object outobj = null;
        if ("--expand".equals(opt)) {
            outobj = JsonLdProcessor.expand(inobj, opts);
        } else if ("--compact".equals(opt)) {
            if (ctxobj == null) {
                System.out.println("Error: The compaction context must not be null.");
                usage();
                return;
            }
            outobj = JsonLdProcessor.compact(inobj, ctxobj, opts);
        } else if ("--normalize".equals(opt)) {
            outobj = JsonLdProcessor.normalize(inobj, opts);
        } else if ("--frame".equals(opt)) {
            if (ctxobj != null && !(ctxobj instanceof Map)) {
                System.out
                        .println("Invalid JSON-LD syntax; a JSON-LD frame must be a single object.");
                usage();
                return;
            }
            outobj = JsonLdProcessor.frame(inobj, ctxobj, opts);
        } else if ("--flatten".equals(opt)) {
            outobj = JsonLdProcessor.flatten(inobj, ctxobj, opts);
        } else if ("--toRDF".equals(opt)) {
            opts.useNamespaces = true;
            outobj = JsonLdProcessor.toRDF(inobj, opts);
        } else if ("--fromRDF".equals(opt)) {
            outobj = JsonLdProcessor.fromRDF(inobj, opts);
        } else {
            System.out.println("Error: invalid option \"" + opt + "\"");
            usage();
            return;
        }

        if ("--toRDF".equals(opt) || "--normalize".equals(opt)) {
            System.out.println((String) outobj);
        } else {
            System.out.println(JsonUtils.toPrettyString(outobj));
        }
    }

    private static void usage() {
        System.out.println("Usage: jsonldplayground <options>");
        System.out.println("\tinput: a filename or JsonLdUrl to the rdf input (in rdfxml or n3)");
        System.out.println("\toptions:");
        System.out
                .println("\t\t--ignorekeys <keys to ignore> : a (space separated) list of keys to ignore (e.g. @geojson)");
        System.out.println("\t\t--base <uri>: base URI");
        System.out.println("\t\t--debug: Print out stack traces when errors occur");
        System.out.println("\t\t--expand <input>: expand the input  JSON-LD");
        System.out
                .println("\t\t--compact <input> <context> : compact the input JSON-LD applying the optional context file");
        System.out
                .println("\t\t--normalize <input> <format> : normalize the input JSON-LD outputting as format (defaults to nquads)");
        System.out
                .println("\t\t--frame <input> <frame> : frame the input JSON-LD with the optional frame file");
        System.out
                .println("\t\t--flatten <input> <context> : flatten the input JSON-LD applying the optional context file");
        System.out
                .println("\t\t--fromRDF <input> <format> : generate JSON-LD from the input rdf (format defaults to nquads)");
        System.out
                .println("\t\t--toRDF <input> <format> : generate RDF from the input JSON-LD (format defaults to nquads)");
        System.out
                .println("\t\t--outputForm [compacted|expanded|flattened] : the way to output the results from fromRDF (defaults to expanded)");
        System.out.println("\t\t--simplify : simplify the input JSON-LD");
        System.exit(1);
    }
}
