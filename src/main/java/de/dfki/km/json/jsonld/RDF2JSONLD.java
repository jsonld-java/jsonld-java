package de.dfki.km.json.jsonld;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.FileManager;

import de.dfki.km.json.JSONUtils;
import de.dfki.km.json.jsonld.impl.JSONLDProcessorImpl;
import de.dfki.km.json.jsonld.impl.JSONLDSerializer;

public class RDF2JSONLD {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 1) {
			usage();
		} else {
			String input = null;
			boolean expand = false;
			boolean normalize = false;
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-expand")) {
					expand = true;
				} else if (args[i].equals("-normalize")) {
					expand = true;
				} else if (i == args.length-1) {
					input = args[i];
				} else {
					System.out.println("unknown option: " + args[i]);
					usage();
				}
			}
			
			if (input == null) {
				usage();
			}
			
			Model model = FileManager.get().loadModel(input);
			List<Resource> subjects = new ArrayList<Resource>();
			
			ResIterator subjitr = model.listSubjects();
			while (subjitr.hasNext()) {
				subjects.add(subjitr.next());
			}
			
			JSONLDSerializer serializer = new JSONLDSerializer();
			Resource[] r = new Resource[subjects.size()];
			subjects.toArray(r);
			Object output = serializer.fromJenaModel(model, r);
			
			JSONLDProcessorImpl processor = new JSONLDProcessorImpl();
			
			if (normalize) {
				output = processor.normalize(output);
			} else if (expand) {
				// normalization starts out by expanding the input, so we only need to do this
				// if normalizaion hasn't happened
				output = processor.expand(output);
			}
			
			if (output != null) {
				try {
					System.out.println(JSONUtils.toString(output));
				} catch (JsonGenerationException e) {
					System.out.println("Error: unable to create JSON output from input");
					e.printStackTrace();
					usage();
				} catch (JsonMappingException e) {
					System.out.println("Error: unable to create JSON output from input");
					e.printStackTrace();
					usage();
				}
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
