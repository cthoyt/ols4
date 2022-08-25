
package uk.ac.ebi.owl2json.transforms;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.jena.sparql.util.TranslationTable;

import uk.ac.ebi.owl2json.OwlNode;
import uk.ac.ebi.owl2json.OwlTranslator;
import uk.ac.ebi.owl2json.properties.*;

/*
 * In the (very rare) cases of punning, sometimes the target of an ObjectProperty has
 * multiple types, and we can only work out which one it's supposed to
 * point to by looking at the semantics (range) of the property.
 * 
 * This transform replaces properties that point to URIs (which are potentially
 * ambiguous) with unique type+URI IDs of the form e.g. class+http://...
 * or individual+http://
 * 
 * In our owl2json data model, that means replacing PropertyValueURIs with PropertyValueIDs.
 * 
 * It does this by looking up each property and checking the semantics
 * where possible.
 */

public class ObjectPropertyDisambiguator {

	public static void disambiguateObjectProperties(OwlTranslator translator) {


		long startTime1 = System.nanoTime();

        Map<String, Set<String>> propertyRanges = new HashMap<>();

		for(String id : translator.nodes.keySet()) {
		    OwlNode c = translator.nodes.get(id);
		    if ( c.types.contains(OwlNode.NodeType.PROPERTY)) {

                // should be no bnodes here but skip if there are
                if(c.uri == null)
                    continue;

                PropertyValue range = c.properties.getPropertyValue(
                    "http://www.w3.org/2000/01/rdf-schema#range");

                if(range == null)
                    continue;

                // TODO: handle complex ranges
                if(range.getType() != PropertyValue.Type.URI) {
                    System.out.println("Range is not a URI for property: " + c.uri);
                    continue;
                }
    
                String rangeUri = ((PropertyValueURI) range).getUri();

                propertyRanges.put(c.uri, Set.of(rangeUri));
            }
        }
        
		long endTime1 = System.nanoTime();
		System.out.println("disambiguate object properties - build range map: " + ((endTime1 - startTime1) / 1000 / 1000 / 1000));



		long startTime2 = System.nanoTime();

		for(String id : translator.nodes.keySet()) {
		    OwlNode c = translator.nodes.get(id);
		    if (c.types.contains(OwlNode.NodeType.CLASS) ||
				c.types.contains(OwlNode.NodeType.PROPERTY) ||
				c.types.contains(OwlNode.NodeType.NAMED_INDIVIDUAL)) {

                    for(String property : c.properties.getPropertyPredicates()) {

                        Set<String> range = propertyRanges.get(property);

                        if(range == null) {
                            // No range is specified for this property
                            // We assume it's fine, nothing to do
                            continue;
                        }

                        List<PropertyValue> values = c.properties.getPropertyValues(property);

                        for(int n = 0; n < values.size(); ) {

                            PropertyValue value = values.get(n);

                            if(value.getType() != PropertyValue.Type.URI)
                                continue;

                            Set<String> ancestors = getAncestors(
                                ((PropertyValueURI) value).getUri(), translator);

                            // Does the value overlap with the range of the property?
                            //
                            Set<String> overlap = intersection(range, ancestors);

                            if(overlap.size() == 0) {
                                // If not, drop this value
                                values.remove(n);
                                continue;
                            }

                            // If so, disambiguate what kind of entity we point to.
                            getEntityTypes(overlap, translator);
                        }

                        if(values.size() == 0) {
                            c.properties.removeProperty(property);
                        }
                    }
                }
		}

		long endTime2 = System.nanoTime();
		System.out.println("disambiguate object properties - update property values: " + ((endTime2 - startTime2) / 1000 / 1000 / 1000));
    }

    static Set<String> getAncestors(String uri, OwlTranslator translator) {

        OwlNode node = translator.nodes.get(uri);

        if(node == null)
            return Set.of();

        String predicate;

        if(node.types.contains(OwlNode.NodeType.CLASS)) {
            predicate = "http://www.w3.org/2000/01/rdf-schema#subClassOf";
        } else if(node.types.contains(OwlNode.NodeType.PROPERTY)) {
            predicate = "http://www.w3.org/2000/01/rdf-schema#subPropertyOf";
        } else {
            return new HashSet<>();
        }

        Set<String> ancestors = new TreeSet<>();

        List<PropertyValue> parents = node.properties.getPropertyValues(predicate);

        for(PropertyValue parent : parents) {

            if(parent.getType() != PropertyValue.Type.URI)
                continue;

            String parentUri = ((PropertyValueURI) parent).getUri();

            ancestors.add(parentUri);
            ancestors.addAll(getAncestors(parentUri, translator));
        }

        return ancestors;

    }

    static Set<String> intersection(Set<String> a, Set<String> b) {
        Set<String> intersection = new TreeSet<>(a);
        intersection.retainAll(b);
        return intersection;
    }

    static Set<String> getEntityTypes(Set<String> uris, OwlTranslator translator) {

        Set<String> types = new TreeSet<>();

        for(String uri : uris) {
            OwlNode node = translator.nodes.get(uri);

            if(node.types.contains(OwlNode.NodeType.CLASS))
                types.add("class");

            if(node.types.contains(OwlNode.NodeType.PROPERTY))
                types.add("property");

            if(node.types.contains(OwlNode.NodeType.NAMED_INDIVIDUAL))
                types.add("individual");

            if(node.types.contains(OwlNode.NodeType.ONTOLOGY))
                types.add("individual");
        }

        return types;
    }
    
}
