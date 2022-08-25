package uk.ac.ebi.owl2json;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

import uk.ac.ebi.owl2json.properties.*;
import uk.ac.ebi.owl2json.properties.PropertyValue.Type;
import uk.ac.ebi.owl2json.transforms.*;

import org.apache.jena.riot.Lang;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFParserBuilder;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Quad;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;


public class OwlTranslator {

    public Map<String, Object> config;

    OwlGraph graph;

    // a graph with the OWL and RDFS semantics; we don't write this as output
    // but need it to know the semantics of owl properties
    OwlGraph owlSemanticsGraph;

    OwlTranslator(Map<String, Object> config, boolean loadLocalFiles, boolean noDates) {

        graph = new OwlGraph();

        owlSemanticsGraph = new OwlGraph();
        owlSemanticsGraph.loadUrl("https://www.w3.org/2002/07/owl", null, false);
        owlSemanticsGraph.loadUrl("http://www.w3.org/2000/01/rdf-schema", null, false);

        long startTime = System.nanoTime();

        this.config = config;

        String url = (String) config.get("ontology_purl");

        if(url == null) {

            Collection<Map<String,Object>> products =
                (Collection<Map<String,Object>>) config.get("products");

            for(Map<String,Object> product : products) {

                String purl = (String) product.get("ontology_purl");

                if(purl != null && purl.endsWith(".owl")) {
                    url = purl;
                    break;
                }

            }

        }

        graph.loadUrl(url, Lang.RDFXML, loadLocalFiles);

        // Before we evaluate imports, mark all the nodes so far as not imported
        for(String id : graph.nodes.keySet()) {
            OwlNode c = graph.nodes.get(id);
            if(c.uri != null) {
                c.properties.addProperty("imported", PropertyValueLiteral.fromString("false"));
            }
        }


	while(graph.importUrls.size() > 0) {
		String importUrl = graph.importUrls.get(0);
		graph.importUrls.remove(0);

		System.out.println("import: " + importUrl);
        graph.loadUrl(importUrl, Lang.RDFXML, loadLocalFiles);
	}

        // Now the imports are done, mark everything else as imported
    for(String id : graph.nodes.keySet()) {
        OwlNode c = graph.nodes.get(id);
        if(c.uri != null) {
            if(!c.properties.hasProperty("imported")) {
                c.properties.addProperty("imported", PropertyValueLiteral.fromString("true"));
            }
        }
    }

	graph.ontologyNode.properties.addProperty(
		"numberOfEntities", PropertyValueLiteral.fromString(Integer.toString(
            graph.numberOfClasses + graph.numberOfProperties + graph.numberOfIndividuals)));

	graph.ontologyNode.properties.addProperty(
		"numberOfClasses", PropertyValueLiteral.fromString(Integer.toString(graph.numberOfClasses)));

	graph.ontologyNode.properties.addProperty(
		"numberOfProperties", PropertyValueLiteral.fromString(Integer.toString(graph.numberOfProperties)));

	graph.ontologyNode.properties.addProperty(
		"numberOfIndividuals", PropertyValueLiteral.fromString(Integer.toString(graph.numberOfIndividuals)));


    if(!noDates) {
        String now = java.time.LocalDateTime.now().toString();

        graph.ontologyNode.properties.addProperty(
            "loaded", PropertyValueLiteral.fromString(now));
    }


    long endTime = System.nanoTime();
    System.out.println("load ontology: " + ((endTime - startTime) / 1000 / 1000 / 1000));

	ShortFormAnnotator.annotateShortForms(graph, config);
	DefinitionAnnotator.annotateDefinitions(graph, config);
	SynonymAnnotator.annotateSynonyms(graph, config);
	AxiomEvaluator.evaluateAxioms(graph);
	ClassExpressionEvaluator.evaluateClassExpressions(graph);
    OntologyIdAnnotator.annotateOntologyIds(graph, config);
    HierarchyFlagsAnnotator.annotateHierarchyFlags(graph);

    }

    public void write(JsonWriter writer) throws IOException {

        writer.beginObject();

        writer.name("ontologyId");
        writer.value((String) config.get("id"));

        writer.name("uri");
        writer.value(graph.ontologyNode.uri);

        for(String configKey : config.keySet()) {
            Object configVal = config.get(configKey);

            // we include this as ontologyId so it doesn't clash with downstream id fields in neo4j/solr
            if(configKey.equals("id"))
                continue;

            // everything else from the config is stored as a normal property
            writer.name(configKey); 
            System.out.println(configKey);
            writeGenericValue(writer, configVal);
        }

        writeProperties(writer, graph.ontologyNode, graph.ontologyNode.properties, OwlNode.NodeType.ONTOLOGY);

        writer.name("classes");
        writer.beginArray();

        for(String id : graph.nodes.keySet()) {
            OwlNode c = graph.nodes.get(id);
            if (c.uri == null) {
                // don't print bnodes at top level
                continue;
            }
            if (c.types.contains(OwlNode.NodeType.CLASS)) {
                writeNode(writer, c, OwlNode.NodeType.CLASS);
            }
        }

        writer.endArray();


        writer.name("properties");
        writer.beginArray();

        for(String id : graph.nodes.keySet()) {
            OwlNode c = graph.nodes.get(id);
            if (c.uri == null) {
                // don't print bnodes at top level
                continue;
            }
            if (c.types.contains(OwlNode.NodeType.PROPERTY)) {
                writeNode(writer, c, OwlNode.NodeType.PROPERTY);
            }
        }

        writer.endArray();


        writer.name("individuals");
        writer.beginArray();

        for(String id : graph.nodes.keySet()) {
            OwlNode c = graph.nodes.get(id);
            if (c.uri == null) {
                // don't print bnodes at top level
                continue;
            }
            if (c.types.contains(OwlNode.NodeType.NAMED_INDIVIDUAL)) {
                writeNode(writer, c, OwlNode.NodeType.NAMED_INDIVIDUAL);
            }
        }

        writer.endArray();


        writer.endObject();

    }


    // asType is specified because nodes can be punned and therefore have multiple types, in
    // which case they will be specified multiple times in the output (e.g. as both a class and an individual)
    //
    private void writeNode(JsonWriter writer, OwlNode nodeToWrite, OwlNode.NodeType asType) throws IOException {

        if(nodeToWrite.types.contains(OwlNode.NodeType.RDF_LIST)) {

            writer.beginArray();

            for(OwlNode cur = nodeToWrite;;) {

                PropertyValue first = cur.properties.getPropertyValue("http://www.w3.org/1999/02/22-rdf-syntax-ns#first");
                writeValue(writer, first);

                PropertyValue rest = cur.properties.getPropertyValue("http://www.w3.org/1999/02/22-rdf-syntax-ns#rest");

                if(rest.getType() == PropertyValue.Type.URI &&
                        ((PropertyValueURI) rest).getUri().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#nil")) {
                    break;
                }

                cur = graph.nodes.get(graph.nodeIdFromPropertyValue(rest));
            }

            writer.endArray();

        } else {

            writer.beginObject();

            if (nodeToWrite.uri != null) {
                writer.name("uri");
                writer.value(nodeToWrite.uri);
            }

            writeProperties(writer, nodeToWrite, nodeToWrite.properties, asType);
            writer.endObject();
        }
    }


    private void writeProperties(JsonWriter writer, OwlNode subject, PropertySet properties, OwlNode.NodeType asType) throws IOException {

        if(asType != null) {
            writer.name("type");
            writeTypes(writer, asType);
        }

        for (String predicate : properties.getPropertyPredicates()) {

            // Based on the rdfs:domain of the property, should it be written for this SUBJECT?
            // This cleans up the properties of punned objects so that only the correct ones are
            // written for each type.
            //
            if(subject != null && !shouldWriteProperty(subject, predicate)) {
                continue;
            }

            List<PropertyValue> values = properties.getPropertyValues(predicate);

            if(values.size() == 0)
                continue;

            writer.name(predicate);

            if(values.size() == 1) {
                writePropertyValue(writer, subject, predicate, values.get(0), null);
            } else {
                writer.beginArray();
                for (PropertyValue value : values) {
                    writePropertyValue(writer, subject, predicate, value, null);
                }
                writer.endArray();
            }
        }


        // Labels for rendering the properties in the frontend (or for API consumers)
        //
        writer.name("propertyLabels");
        writer.beginObject();

        for(String k : properties.getPropertyPredicates()) {

            OwlNode labelNode = graph.nodes.get(k);
            if(labelNode == null) {
                continue;
            }

            List<PropertyValue> labelProps = labelNode.properties.getPropertyValues("http://www.w3.org/2000/01/rdf-schema#label");

            if(labelProps != null && labelProps.size() > 0) {
                for (PropertyValue prop : labelProps) {

                    if(prop.getType() != PropertyValue.Type.LITERAL)
                        continue;

                    String lang = ((PropertyValueLiteral) prop).getLang();

                    if(lang==null||lang.equals(""))
                        lang="en";

                    writer.name(lang+"+"+k);
                    writer.value( ((PropertyValueLiteral) prop).getValue() );
                }
            }

        }

        writer.endObject();
    }

    private Set<String> getAncestors(String uri) {

        Set<String> ancestors = new HashSet<String>();
        ancestors.add(uri);

        OwlNode node = graph.nodes.get(uri);

        if(node == null)
            node = owlSemanticsGraph.nodes.get(uri);

        if(node != null) {

            List<PropertyValue> subClassOf = node.properties.getPropertyValues("http://www.w3.org/2000/01/rdf-schema#subClassOf");

            if(subClassOf != null && subClassOf.size() > 0) {
                for(PropertyValue sco : subClassOf) {
                    if(sco.getType() == Type.URI) {
                        ancestors.addAll(getAncestors(((PropertyValueURI) sco).getUri()));
                    }
                }
            }
        }

        return ancestors;

    }

    private boolean shouldWriteProperty(OwlNode subject, String predicate) {

        OwlNode propertyNode = graph.nodes.get(predicate);

        if(propertyNode == null) {
            propertyNode = owlSemanticsGraph.nodes.get(predicate);
        }

        if(propertyNode == null) {
            // the property is not defined; assume it's fine
            return true;
        }

        List<PropertyValue> nodeTypes = 
            subject.properties.getPropertyValues("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");

        Set<String> allAncestors = new HashSet<>();

        for(PropertyValue type : nodeTypes) {

            if(type.getType() != Type.URI)
                continue;

            allAncestors.addAll(getAncestors(((PropertyValueURI) type).getUri()));
        }

        // Do the types overlap with the domain of the property?

        List<PropertyValue> domains =
            propertyNode.properties.getPropertyValues("http://www.w3.org/2000/01/rdf-schema#domain");

        // no domains specified; assume it's fine
        if(domains == null || domains.size() == 0) {
            return true;
        }

        // TODO: this doesn't handle complex domains, but it's fine for OWL/RDFS which is all we
        // really care about (for now at least)
        //
            for(PropertyValue domainVal : domains) {
                if(domainVal.getType() == Type.URI) {
                    if(allAncestors.contains(((PropertyValueURI) domainVal).getUri()))
                        return true;
                }
            }

        return false;
    }


    public void writePropertyValue(JsonWriter writer, OwlNode subject, String predicate, PropertyValue value, OwlNode.NodeType asType) throws IOException {
        if (value.properties != null) {
            // reified
            writer.beginObject();
            writer.name("value");
            writeValue(writer, value);
            writeProperties(writer, null, value.properties, asType);
            writer.endObject();
        } else {
            // not reified
            writeValue(writer, value);
        }

    }

    public void writeValue(JsonWriter writer, PropertyValue value) throws IOException {
        assert (value.properties == null);

        switch(value.getType()) {
            case BNODE:
                OwlNode c = graph.nodes.get(((PropertyValueBNode) value).getId());
                if (c == null) {
                    writer.value("?");
                } else {
                    writeNode(writer, c, null);
                }
                break;
            case LITERAL:
                PropertyValueLiteral literal = (PropertyValueLiteral) value;
                if(literal.getDatatype().equals("http://www.w3.org/2001/XMLSchema#string") &&
                        literal.getLang().equals("")
                ) {
                    writer.value(literal.getValue());
                } else {
                    writer.beginObject();
                    writer.name("datatype");
                    writer.value(literal.getDatatype());
                    writer.name("value");
                    writer.value(literal.getValue());
                    if(!literal.getLang().equals("")) {
                        writer.name("lang");
                        writer.value(literal.getLang());
                    }
                    writer.endObject();
                }
                break;
            case URI:
                writer.value(((PropertyValueURI) value).getUri());
                break;
            default:
                writer.value("?");
                break;
        }
    }


	private void writeTypes(JsonWriter writer, OwlNode.NodeType type) throws IOException {
		writer.beginArray();
		switch (type) {
			case ONTOLOGY:
				writer.value("ontology");
				break;
			case CLASS:
				writer.value("class");
				writer.value("entity");
				writer.value("term");
				break;
			case PROPERTY:
				writer.value("entity");
				writer.value("property");
				writer.value("term");
				break;
			case NAMED_INDIVIDUAL:
				writer.value("entity");
				writer.value("individual");
				break;
			default:
				throw new RuntimeException("unknown type for output");
		}
		writer.endArray();
	}

    private static void writeGenericValue(JsonWriter writer, Object val) throws IOException {

        if(val instanceof Collection) {
            writer.beginArray();
            for(Object entry : ((Collection<Object>) val)) {
                writeGenericValue(writer, entry);
            }
            writer.endArray();
        } else if(val instanceof Map) {
            Map<String,Object> map = new TreeMap<String,Object> ( (Map<String,Object>) val );
            writer.beginObject();
            for(String k : map.keySet()) {
                writer.name(k);
                writeGenericValue(writer, map.get(k));
            }
            writer.endObject();
        } else if(val instanceof String) {
            writer.value((String) val);
        } else if(val instanceof Integer) {
            writer.value((Integer) val);
        } else if(val instanceof Double) {
            writer.value((Double) val);
        } else if(val instanceof Long) {
            writer.value((Long) val);
        } else if(val instanceof Boolean) {
            writer.value((Boolean) val);
        } else if(val == null) {
            writer.nullValue();
        } else {
            throw new RuntimeException("Unknown value type");
        }

    }





}
