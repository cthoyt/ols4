package uk.ac.ebi.owl2json.transforms;

import java.util.List;

import uk.ac.ebi.owl2json.OwlGraph;
import uk.ac.ebi.owl2json.OwlNode;
import uk.ac.ebi.owl2json.OwlTranslator;
import uk.ac.ebi.owl2json.properties.PropertyValue;


public class ClassExpressionEvaluator {

	// turn bnode types (Restrictions, Classes with oneOf etc) into direct edges
	//
	public static void evaluateClassExpressions(OwlGraph graph) {


		long startTime4 = System.nanoTime();

		for(String id : graph.nodes.keySet()) {
		OwlNode c = graph.nodes.get(id);

		// skip BNodes; we are looking for things with BNodes as types, not the BNodes themselves
		if(c.uri == null)
			continue;

			List<PropertyValue> types = c.properties.getPropertyValues("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");

            if(types != null) {
                for(PropertyValue type : types) {
                    OwlNode typeNode = graph.nodes.get(graph.nodeIdFromPropertyValue(type));

                    // Is the type a BNode?
                    if(typeNode != null && typeNode.uri == null) {
                        evaluateTypeExpression(graph, c, type);
                    }
                }
            }
		}



		long endTime4 = System.nanoTime();
		System.out.println("evaluate restrictions: " + ((endTime4 - startTime4) / 1000 / 1000 / 1000));
	}

    private static void evaluateTypeExpression(OwlGraph graph, OwlNode node, PropertyValue typeProperty) {

	OwlNode typeNode = graph.nodes.get(graph.nodeIdFromPropertyValue(typeProperty));

	if(typeNode != null && typeNode.types.contains(OwlNode.NodeType.RESTRICTION)) {

		List<PropertyValue> hasValue = typeNode.properties.getPropertyValues("http://www.w3.org/2002/07/owl#hasValue");
		if(hasValue != null && hasValue.size() > 0) {
			evaluateTypeExpression(graph, node, hasValue.get(0));
			return;
		}

		List<PropertyValue> someValuesFrom = typeNode.properties.getPropertyValues("http://www.w3.org/2002/07/owl#someValuesFrom");
		if(someValuesFrom != null && someValuesFrom.size() > 0) {
			evaluateTypeExpression(graph, node, someValuesFrom.get(0));
			return;
		}

		List<PropertyValue> allValuesFrom = typeNode.properties.getPropertyValues("http://www.w3.org/2002/07/owl#allValuesFrom");
		if(allValuesFrom != null && allValuesFrom.size() > 0) {
			evaluateTypeExpression(graph, node, allValuesFrom.get(0));
			return;
		}

	} else if(typeNode != null && typeNode.types.contains(OwlNode.NodeType.CLASS)) {

		List<PropertyValue> oneOf = typeNode.properties.getPropertyValues("http://www.w3.org/2002/07/owl#oneOf");
		if(oneOf != null && oneOf.size() > 0) {
			for(PropertyValue prop : oneOf) {
				evaluateTypeExpression(graph, node, prop);
			}
			return;
		}

	}

	// not an expression - we should recursively end up here!
	//
	node.properties.addProperty("relatedTo", typeProperty);
    }
	
}
