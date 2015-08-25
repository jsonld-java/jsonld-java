package com.github.jsonldjava.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.ValueConversionException;
import joptsimple.ValueConverter;

import org.openrdf.model.Model;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParserRegistry;
import org.openrdf.rio.Rio;

import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

public class Playground {

    private static Set<String> getProcessingOptions() {
        return new LinkedHashSet<String>(Arrays.asList("expand", "compact", "frame", "normalize",
                "flatten", "fromrdf", "tordf"));
    }

    private static boolean hasContext(String opt) {
        return "compact".equals(opt) || "frame".equals(opt) || "flatten".equals(opt);
    }

    private static Map<String, RDFFormat> getOutputFormats() {
        final Map<String, RDFFormat> outputFormats = new HashMap<String, RDFFormat>();

        for (final RDFFormat format : RDFParserRegistry.getInstance().getKeys()) {
            outputFormats.put(format.getName().replaceAll("-", "").replaceAll("/", "")
                    .toLowerCase(), format);
        }

        return outputFormats;
    }

    public static void main(String[] args) throws Exception {

        final Map<String, RDFFormat> formats = getOutputFormats();
        final Set<String> outputForms = new LinkedHashSet<String>(Arrays.asList("compacted",
                "expanded", "flattened"));

        final OptionParser parser = new OptionParser();

        final OptionSpec<Void> help = parser.accepts("help").forHelp();

        final OptionSpec<String> base = parser.accepts("base").withRequiredArg()
                .ofType(String.class).defaultsTo("").describedAs("base URI");

        final OptionSpec<File> inputFile = parser.accepts("inputFile").withRequiredArg()
                .ofType(File.class).required().describedAs("The input file");

        final OptionSpec<File> context = parser.accepts("context").withRequiredArg()
                .ofType(File.class).describedAs("The context");

        final OptionSpec<RDFFormat> outputFormat = parser
                .accepts("format")
                .withOptionalArg()
                .ofType(String.class)
                .withValuesConvertedBy(new ValueConverter<RDFFormat>() {
                    @Override
                    public RDFFormat convert(String arg0) {
                        // Normalise the name to provide alternatives
                        final String formatName = arg0.replaceAll("-", "").replaceAll("/", "")
                                .toLowerCase();
                        if (formats.containsKey(formatName)) {
                            return formats.get(formatName);
                        }
                        throw new ValueConversionException("Format was not known: " + arg0
                                + " (Valid values are: " + formats.keySet() + ")");
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
                .defaultsTo(RDFFormat.NQUADS)
                .describedAs(
                        "The output file format to use. Defaults to nquads. Valid values are: "
                                + formats.keySet());

        final OptionSpec<String> processingOption = parser
                .accepts("process")
                .withRequiredArg()
                .ofType(String.class)
                .required()
                .withValuesConvertedBy(new ValueConverter<String>() {
                    @Override
                    public String convert(String value) {
                        if (getProcessingOptions().contains(value.toLowerCase())) {
                            return value.toLowerCase();
                        }
                        throw new ValueConversionException("Processing option was not known: "
                                + value + " (Valid values are: " + getProcessingOptions() + ")");
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
                .describedAs(
                        "The processing to perform. Valid values are: "
                                + getProcessingOptions().toString());

        final OptionSpec<String> outputForm = parser
                .accepts("outputForm")
                .withOptionalArg()
                .ofType(String.class)
                .defaultsTo("expanded")
                .withValuesConvertedBy(new ValueConverter<String>() {
                    @Override
                    public String convert(String value) {
                        if (outputForms.contains(value.toLowerCase())) {
                            return value.toLowerCase();
                        }
                        throw new ValueConversionException("Output form was not known: " + value
                                + " (Valid values are: " + outputForms + ")");
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
                .describedAs(
                        "The way to output the results from fromRDF. Defaults to expanded. Valid values are: "
                                + outputForms);

        OptionSet options = null;

        try {
            options = parser.parse(args);
        } catch (final OptionException e) {
            System.out.println(e.getMessage());
            parser.printHelpOn(System.out);
            throw e;
        }

        if (options.has(help)) {
            parser.printHelpOn(System.out);
            return;
        }

        final JsonLdOptions opts = new JsonLdOptions("");
        Object inobj = null;
        Object ctxobj = null;

        opts.setBase(options.valueOf(base));
        opts.outputForm = options.valueOf(outputForm);
        opts.format = options.has(outputFormat) ? options.valueOf(outputFormat)
                .getDefaultMIMEType() : "application/nquads";
        final RDFFormat sesameOutputFormat = options.valueOf(outputFormat);
        final RDFFormat sesameInputFormat = Rio.getParserFormatForFileName(
                options.valueOf(inputFile).getName(), RDFFormat.JSONLD);

        final String processingOptionValue = options.valueOf(processingOption);

        if (!options.valueOf(inputFile).exists()) {
            System.out.println("Error: input file \"" + options.valueOf(inputFile)
                    + "\" doesn't exist");
            parser.printHelpOn(System.out);
            return;
        }
        // if base is currently null, set it
        if (opts.getBase() == null || opts.getBase().equals("")) {
            opts.setBase(options.valueOf(inputFile).toURI().toASCIIString());
        }

        if ("fromrdf".equals(processingOptionValue)) {
            inobj = readFile(options.valueOf(inputFile));
        } else {
            inobj = JsonUtils.fromInputStream(new FileInputStream(options.valueOf(inputFile)));
        }

        if (hasContext(processingOptionValue) && options.has(context)) {
            if (!options.valueOf(context).exists()) {
                System.out.println("Error: context file \"" + options.valueOf(context)
                        + "\" doesn't exist");
                parser.printHelpOn(System.out);
                return;
            }
            ctxobj = JsonUtils.fromInputStream(new FileInputStream(options.valueOf(context)));
        }

        Object outobj = null;
        if ("fromrdf".equals(processingOptionValue)) {
            final Model inModel = Rio.parse(new StringReader((String) inobj), opts.getBase(),
                    sesameInputFormat);

            outobj = JsonLdProcessor.fromRDF(inModel, opts, new SesameJSONLDRDFParser());
        } else if ("tordf".equals(processingOptionValue)) {
            opts.useNamespaces = true;
            outobj = JsonLdProcessor
                    .toRDF(inobj,
                            new SesameJSONLDTripleCallback(Rio.createWriter(sesameOutputFormat,
                                    System.out)), opts);
        } else if ("expand".equals(processingOptionValue)) {
            outobj = JsonLdProcessor.expand(inobj, opts);
        } else if ("compact".equals(processingOptionValue)) {
            if (ctxobj == null) {
                System.out.println("Error: The compaction context must not be null.");
                parser.printHelpOn(System.out);
                return;
            }
            outobj = JsonLdProcessor.compact(inobj, ctxobj, opts);
        } else if ("normalize".equals(processingOptionValue)) {
            outobj = JsonLdProcessor.normalize(inobj, opts);
        } else if ("frame".equals(processingOptionValue)) {
            if (ctxobj != null && !(ctxobj instanceof Map)) {
                System.out
                        .println("Invalid JSON-LD syntax; a JSON-LD frame must be a single object.");
                parser.printHelpOn(System.out);
                return;
            }
            outobj = JsonLdProcessor.frame(inobj, ctxobj, opts);
        } else if ("flatten".equals(processingOptionValue)) {
            outobj = JsonLdProcessor.flatten(inobj, ctxobj, opts);
        } else {
            System.out
                    .println("Error: invalid processing option \"" + processingOptionValue + "\"");
            parser.printHelpOn(System.out);
            return;
        }

        if ("tordf".equals(processingOptionValue)) {
            // Already serialised above
        } else if ("normalize".equals(processingOptionValue)) {
            System.out.println((String) outobj);
        } else {
            System.out.println(JsonUtils.toPrettyString(outobj));
        }
    }

    private static String readFile(File in) throws IOException {
        final BufferedReader buf = new BufferedReader(new InputStreamReader(
                new FileInputStream(in), "UTF-8"));
        String inobj = "";
        try {
            String line;
            while ((line = buf.readLine()) != null) {
                line = line.trim();
                inobj = (inobj) + line + "\n";
            }
        } finally {
            buf.close();
        }
        return inobj;
    }

    // private static void usage() {
    // System.out.println("Usage: jsonldplayground <options>");
    // System.out.println("\tinput: a filename or JsonLdUrl to the rdf input (in rdfxml or n3)");
    // System.out.println("\toptions:");
    // System.out
    // .println("\t\t--ignorekeys <keys to ignore> : a (space separated) list of keys to ignore (e.g. @geojson)");
    // System.out.println("\t\t--base <uri>: base URI");
    // System.out.println("\t\t--debug: Print out stack traces when errors occur");
    // System.out.println("\t\t--expand <input>: expand the input  JSON-LD");
    // System.out
    // .println("\t\t--compact <input> <context> : compact the input JSON-LD applying the optional context file");
    // System.out
    // .println("\t\t--normalize <input> <format> : normalize the input JSON-LD outputting as format (defaults to nquads)");
    // System.out
    // .println("\t\t--frame <input> <frame> : frame the input JSON-LD with the optional frame file");
    // System.out
    // .println("\t\t--flatten <input> <context> : flatten the input JSON-LD applying the optional context file");
    // System.out
    // .println("\t\t--fromRDF <input> <format> : generate JSON-LD from the input rdf (format defaults to nquads)");
    // System.out
    // .println("\t\t--toRDF <input> <format> : generate RDF from the input JSON-LD (format defaults to nquads)");
    // System.out
    // .println("\t\t--outputForm [compacted|expanded|flattened] : the way to output the results from fromRDF (defaults to expanded)");
    // System.out.println("\t\t--simplify : simplify the input JSON-LD");
    // System.exit(1);
    // }
}
