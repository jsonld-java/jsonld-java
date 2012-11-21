package de.dfki.km.json.jsonld.tools;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileManager;

import de.dfki.km.json.JSONUtils;
import de.dfki.km.json.jsonld.JSONLD;
import de.dfki.km.json.jsonld.JSONLDProcessingError;
import de.dfki.km.json.jsonld.JSONLDProcessor;
import de.dfki.km.json.jsonld.impl.JenaJSONLDSerializer;

public class RDF2JSONLD {

    /**
     * @param args
     * @throws JSONLDProcessingError 
     * @throws IOException 
     * @throws FileNotFoundException 
     */
    public static void main(String[] args) throws JSONLDProcessingError, FileNotFoundException, IOException {
        if (args.length < 1) {
            usage();
        } else {
            String input = null;
            String framefile = null;
            boolean expand = false;
            boolean frame = false;
            boolean simplify = false;
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-expand")) {
                    expand = true;
                } else if (args[i].equals("-frame")) {
                    frame = true;
                } else if (args[i].equals("-simplify")) {
                	simplify = true;
                } else if (frame && i == args.length - 2) {
                	input = args[i];
                	framefile = args[i+1];
                } else if (i == args.length - 1 && input == null) {
                    input = args[i];
                } else {
                    System.out.println("unknown option: " + args[i]);
                    usage();
                }
            }

            if (input == null) {
                usage();
            }
            
            Map<String,Object> inframe = null;
            if (frame) {
            	 if (framefile == null) {
            		 inframe = new HashMap<String, Object>();
            	 } else {
            		 inframe = (Map<String, Object>) JSONUtils.fromInputStream(new FileInputStream(framefile));
            	 }
            }

            Model model = FileManager.get().loadModel(input);
            JenaJSONLDSerializer serializer = new JenaJSONLDSerializer();
            
            Object output = JSONLD.fromRDF(model, serializer);

            if (expand) {
                output = JSONLD.expand(output);
            }
            if (frame) {
            	output = JSONLD.frame(output, inframe);
            }
            if (simplify) {
            	output = JSONLD.simplify(output);
            }

            if (output != null) {
                System.out.println(JSONUtils.toString(output));
            }
        }
    }

    private static void usage() {
        System.out.println("Usage: rdf2jsonld <options> <input>");
        System.out.println("\tinput: a filename or URL to the rdf input (in rdfxml or n3)");
        System.out.println("\toptions:");
        System.out.println("\t\t-expand : expand the jsonld output");
        System.out.println("\t\t-normalize : normalize the jsonld output");
        System.exit(1);
    }

}
