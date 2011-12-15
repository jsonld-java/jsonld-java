package de.dfki.km.json.jsonld;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

public interface JSONLDSerializer {
	Object fromJenaModel(Model model, Resource[] subjects);
	Object fromResource(Resource r);
}
