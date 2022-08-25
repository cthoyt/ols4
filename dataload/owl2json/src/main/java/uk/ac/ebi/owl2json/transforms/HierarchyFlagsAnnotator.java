package uk.ac.ebi.owl2json.transforms;

import org.apache.jena.graph.NodeFactory;

import uk.ac.ebi.owl2json.OwlGraph;
import uk.ac.ebi.owl2json.OwlNode;
import uk.ac.ebi.owl2json.OwlTranslator;
import uk.ac.ebi.owl2json.properties.PropertyValueLiteral;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HierarchyFlagsAnnotator {

    public static void annotateHierarchyFlags(OwlGraph graph) {

        long startTime3 = System.nanoTime();

        for(String id : graph.nodes.keySet()) {
            OwlNode c = graph.nodes.get(id);

		    if (c.types.contains(OwlNode.NodeType.CLASS) ||
				c.types.contains(OwlNode.NodeType.PROPERTY) ||
				c.types.contains(OwlNode.NodeType.NAMED_INDIVIDUAL)) {

                // skip bnodes
                if(c.uri == null)
                    continue;

                c.properties.addProperty("hasChildren",
                        graph.hasChildren.contains(c.uri) ?
                            PropertyValueLiteral.fromString("true") :
                            PropertyValueLiteral.fromString("false")
                );

                c.properties.addProperty("isRoot",
                        graph.hasParents.contains(c.uri) ?
                                PropertyValueLiteral.fromString("false") :
                                PropertyValueLiteral.fromString("true")
                );
            }
        }
        long endTime3 = System.nanoTime();
        System.out.println("annotate hierarchy flags: " + ((endTime3 - startTime3) / 1000 / 1000 / 1000));


    }
}


