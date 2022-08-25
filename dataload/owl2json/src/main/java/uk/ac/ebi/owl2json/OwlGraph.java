package uk.ac.ebi.owl2json;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFParserBuilder;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Quad;

import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.TreeMultimap;

import uk.ac.ebi.owl2json.properties.*;

public class OwlGraph implements StreamRDF {

    public List<String> importUrls = new ArrayList<>();
    public Set<String> languages = new TreeSet<>();

    public Set<String> hasChildren = new HashSet<>();
    public Set<String> hasParents = new HashSet<>();

    public int numberOfClasses = 0;
    public int numberOfProperties = 0;
    public int numberOfIndividuals = 0;

    public Map<String, OwlNode> nodes = new TreeMap<>();
    OwlNode ontologyNode = null;

    OwlGraph() {

        languages.add("en");

    }

    public void loadUrl(String url, Lang lang, boolean loadLocalFiles)  {

        System.out.println("load ontology from: " + url);

	    if(loadLocalFiles && !url.contains("://")) {
		    try {
			    createParser(lang).source(new FileInputStream(url)).parse(this);
		    } catch(FileNotFoundException e) {
			    throw new RuntimeException(e);
		    }
	    } else {
		    createParser(lang).source(url).parse(this);
	    }
    }

    private RDFParserBuilder createParser(Lang lang) {

        RDFParserBuilder builder =  RDFParser.create()
                .strict(false)
                .checking(false);

        if(lang != null) {
            builder = builder.forceLang(Lang.RDFXML);
        }

        return builder;
    }

    private OwlNode getOrCreateNode(Node node) {
        String id = nodeIdFromJenaNode(node);
        OwlNode term = nodes.get(id);
        if (term != null) {
            return term;
        }

        term = new OwlNode();

        if(!node.isBlank())
            term.uri = id;

        nodes.put(id, term);
        return term;
    }

    @Override
    public void start() {

    }

    @Override
    public void triple(Triple triple) {

        if(triple.getObject().isLiteral()) {
            handleLiteralTriple(triple);
        } else {
            handleNamedNodeTriple(triple);
        }

        // TODO: BNodes?

    }


    public void handleLiteralTriple(Triple triple) {

        String subjId = nodeIdFromJenaNode(triple.getSubject());
        OwlNode subjNode = getOrCreateNode(triple.getSubject());

        String lang = triple.getObject().getLiteralLanguage();
        if(lang != null) {
            languages.add(lang);
        }

        subjNode.properties.addProperty(triple.getPredicate().getURI(), PropertyValue.fromJenaNode(triple.getObject()));

    }

    public void handleNamedNodeTriple(Triple triple) {

        OwlNode subjNode = getOrCreateNode(triple.getSubject());

        switch (triple.getPredicate().getURI()) {
            case "http://www.w3.org/1999/02/22-rdf-syntax-ns#type":
                handleType(subjNode, triple.getObject());
                break;
            case "http://www.w3.org/1999/02/22-rdf-syntax-ns#rest":
            case "http://www.w3.org/1999/02/22-rdf-syntax-ns#first":
                subjNode.types.add(OwlNode.NodeType.RDF_LIST);
                break;

            case "http://www.w3.org/2002/07/owl#imports":
                importUrls.add(triple.getObject().getURI());
                break;

            case "http://www.w3.org/2000/01/rdf-schema#subClassOf":

                boolean top = triple.getObject().isURI() &&
                        triple.getObject().getURI().equals("http://www.w3.org/2002/07/owl#Thing");

                if(!top) {

                    if(subjNode.uri != null)
                        hasParents.add(subjNode.uri);

                    if(triple.getObject().isURI())
                        hasChildren.add(triple.getObject().getURI());

                }

                break;
        }

        subjNode.properties.addProperty(triple.getPredicate().getURI(), PropertyValue.fromJenaNode(triple.getObject()));


    }

    public void handleType(OwlNode subjNode, Node type) {

        if(!type.isURI())
            return;

        switch (type.getURI()) {

            case "http://www.w3.org/2002/07/owl#Ontology":

                subjNode.types.add(OwlNode.NodeType.ONTOLOGY);

                if(ontologyNode == null) {
                    ontologyNode = subjNode;
                }

                break;

            case "http://www.w3.org/2002/07/owl#Class":
                subjNode.types.add(OwlNode.NodeType.CLASS);
                ++ numberOfClasses;
                break;

            case "http://www.w3.org/2002/07/owl#AnnotationProperty":
            case "http://www.w3.org/2002/07/owl#ObjectProperty":
            case "http://www.w3.org/2002/07/owl#DatatypeProperty":
                subjNode.types.add(OwlNode.NodeType.PROPERTY);
                ++ numberOfProperties;
                break;

            case "http://www.w3.org/2002/07/owl#NamedIndividual":
                subjNode.types.add(OwlNode.NodeType.NAMED_INDIVIDUAL);
                ++ numberOfIndividuals;
                break;

            case "http://www.w3.org/2002/07/owl#Axiom":
                subjNode.types.add(OwlNode.NodeType.AXIOM);
                break;

            case "http://www.w3.org/2002/07/owl#Restriction":
                subjNode.types.add(OwlNode.NodeType.RESTRICTION);
                break;
        }
    }


    @Override
    public void quad(Quad quad) {

    }

    @Override
    public void base(String s) {

    }

    @Override
    public void prefix(String s, String s1) {

    }

    @Override
    public void finish() {

    }


    public String nodeIdFromJenaNode(Node node)  {
        if(node.isURI()) {
            return node.getURI();
        }
        if(node.isBlank()) {
            return node.getBlankNodeId().toString();
        }
        throw new RuntimeException("unknown node type");
    }

    public String nodeIdFromPropertyValue(PropertyValue node)  {
        if(node.getType() == PropertyValue.Type.URI) {
            return ((PropertyValueURI) node).getUri();
        }
        if(node.getType() == PropertyValue.Type.BNODE) {
            return ((PropertyValueBNode) node).getId();
        }
        throw new RuntimeException("unknown node type");
    }





    public boolean areSubgraphsIsomorphic(PropertyValue rootNodeA, PropertyValue rootNodeB) {

	OwlNode a = nodes.get(nodeIdFromPropertyValue(rootNodeA));
	OwlNode b = nodes.get(nodeIdFromPropertyValue(rootNodeB));

	if(! a.properties.getPropertyPredicates().equals( b.properties.getPropertyPredicates() )) {
		return false;
	}

	for(String predicate : a.properties.getPropertyPredicates()) {
		List<PropertyValue> valuesA = a.properties.getPropertyValues(predicate);
		List<PropertyValue> valuesB = b.properties.getPropertyValues(predicate);

		if(valuesA.size() != valuesB.size())
			return false;

		for(int n = 0; n < valuesA.size(); ++ n) {
			PropertyValue valueA = valuesA.get(n);
			PropertyValue valueB = valuesB.get(n);

			if(valueA.getType() != PropertyValue.Type.BNODE) {
				// non bnode value, simple case
				return valueA.equals(valueB);
			} 

			// bnode value

			if(valueB.getType() != PropertyValue.Type.BNODE)
				return false;

			if(!areSubgraphsIsomorphic(valueA, valueB))
				return false;
		}
	}

	return true;
    }

    
}
