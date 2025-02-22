package uk.ac.ebi.owl2json.annotators;

import uk.ac.ebi.owl2json.OwlGraph;
import uk.ac.ebi.owl2json.OwlNode;
import uk.ac.ebi.owl2json.annotators.helpers.PropertyCollator;
import uk.ac.ebi.owl2json.properties.PropertyValue;
import uk.ac.ebi.owl2json.properties.PropertyValueLiteral;
import uk.ac.ebi.owl2json.properties.PropertyValueURI;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class OboSynonymTypeNameAnnotator {

    public static void annotateOboSynonymTypeNames(OwlGraph graph) {

        long startTime3 = System.nanoTime();
        for(String id : graph.nodes.keySet()) {

            OwlNode c = graph.nodes.get(id);

            if (c.types.contains(OwlNode.NodeType.AXIOM)) {

                List<PropertyValue> hasSynonymType = c.properties.getPropertyValues("http://www.geneontology.org/formats/oboInOwl#hasSynonymType");

                if(hasSynonymType != null) {
                    for(PropertyValue synonymType : hasSynonymType) {
                        if(synonymType.getType() == PropertyValue.Type.URI) {

			    String synonymTypeUri = ((PropertyValueURI) synonymType).getUri();
			    OwlNode synonymTypeNode = graph.nodes.get(synonymTypeUri);

			    if(synonymTypeNode != null) {
				// should be an owl:AnnotationProperty

				PropertyValue labelProp = synonymTypeNode.properties.getPropertyValue("http://www.w3.org/2000/01/rdf-schema#label");

				if(labelProp != null && labelProp.getType() == PropertyValue.Type.LITERAL) {
					String label = ((PropertyValueLiteral) labelProp).getValue();
					c.properties.addProperty("oboSynonymTypeName", PropertyValueLiteral.fromString(label));
				}
			    }

                        }
                    }
                }
            }
        }
        long endTime3 = System.nanoTime();
        System.out.println("annotate obo synonym type names: " + ((endTime3 - startTime3) / 1000 / 1000 / 1000));

    }


}
