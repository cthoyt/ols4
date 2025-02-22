package uk.ac.ebi.spot.ols.repository.v1;

import java.util.Arrays;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.ols.model.v1.V1Property;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.spot.ols.repository.neo4j.OlsNeo4jClient;
import uk.ac.ebi.spot.ols.repository.solr.Fuzziness;
import uk.ac.ebi.spot.ols.repository.solr.OlsSolrQuery;
import uk.ac.ebi.spot.ols.repository.solr.OlsSolrClient;
import uk.ac.ebi.spot.ols.repository.v1.mappers.V1PropertyMapper;

/**
 * @author Simon Jupp
 * @date 18/08/2015
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
//@RepositoryRestResource(collectionResourceRel = "properties", exported = false)
@Component
public class V1PropertyRepository {

    @Autowired
    OlsSolrClient solrClient;

    @Autowired
    OlsNeo4jClient neo4jClient;

    @Autowired
    V1OntologyRepository ontologyRepository;

//    @Query(
//            countQuery = "MATCH (n:Property)-[:SUBPROPERTYOF]->(parent) WHERE n.ontology_name = {0} AND n.iri = {1} RETURN count(parent)",
//            value = "MATCH (n:Property)-[:SUBPROPERTYOF]->(parent) WHERE n.ontology_name = {0} AND n.iri = {1} RETURN parent")
    public Page<V1Property> getParents(String ontologyId, String iri, String lang, Pageable pageable) {

	return neo4jClient.traverseOutgoingEdges("OntologyTerm", ontologyId + "+property+" + iri, Arrays.asList("http://www.w3.org/2000/01/rdf-schema#subPropertyOf"), Map.of(), pageable)
			.map(record -> V1PropertyMapper.mapProperty(record, lang));
    }

//    @Query( countQuery = "MATCH (n:Property)<-[:SUBPROPERTYOF]-(child) WHERE n.ontology_name = {0} AND n.iri = {1} RETURN count(child)",
//            value = "MATCH (n:Property)<-[:SUBPROPERTYOF]-(child) WHERE n.ontology_name = {0} AND n.iri = {1} RETURN child")
    public Page<V1Property> getChildren(String ontologyId, String iri, String lang, Pageable pageable) {

	return this.neo4jClient.traverseIncomingEdges("OntologyTerm", ontologyId + "+property+" + iri, Arrays.asList("http://www.w3.org/2000/01/rdf-schema#subPropertyOf"), Map.of(), pageable)
            .map(record -> V1PropertyMapper.mapProperty(record, lang));
    }


//    @Query(countQuery = "MATCH (n:Property)<-[:SUBPROPERTYOF*]-(child) WHERE n.ontology_name = {0} AND n.iri = {1} RETURN count(distinct child)",
//            value = "MATCH (n:Property)<-[:SUBPROPERTYOF*]-(child) WHERE n.ontology_name = {0} AND n.iri = {1} RETURN distinct child")
    public Page<V1Property> getDescendants(String ontologyId, String iri, String lang, Pageable pageable)  {

	return this.neo4jClient.recursivelyTraverseIncomingEdges("OntologyTerm", ontologyId + "+property+" + iri, Arrays.asList("http://www.w3.org/2000/01/rdf-schema#subPropertyOf"), Map.of(), pageable)
            .map(record -> V1PropertyMapper.mapProperty(record, lang));
    }

//    @Query(countQuery = "MATCH (n:Property)-[:SUBPROPERTYOF*]->(parent) WHERE n.ontology_name = {0} AND n.iri = {1} RETURN count(distinct parent)",
//            value = "MATCH (n:Property)-[:SUBPROPERTYOF*]->(parent) WHERE n.ontology_name = {0} AND n.iri = {1} RETURN distinct parent")
    public Page<V1Property> getAncestors(String ontologyId, String iri, String lang, Pageable pageable)  {

	return neo4jClient.recursivelyTraverseOutgoingEdges("OntologyTerm", ontologyId + "+property+" + iri, Arrays.asList("http://www.w3.org/2000/01/rdf-schema#subPropertyOf"), Map.of(), pageable)
            .map(record -> V1PropertyMapper.mapProperty(record, lang));
    }

//    @Query (value = "MATCH (n:Property) WHERE n.ontology_name = {0} AND n.iri = {1} RETURN n")
    public V1Property findByOntologyAndIri(String ontologyId, String iri, String lang)  {

        OlsSolrQuery query = new OlsSolrQuery();
	query.addFilter("lang", lang, Fuzziness.EXACT);
	query.addFilter("type", "property", Fuzziness.EXACT);
	query.addFilter("ontologyId", ontologyId, Fuzziness.EXACT);
	query.addFilter("iri", iri, Fuzziness.EXACT);

        return V1PropertyMapper.mapProperty(solrClient.getOne(query), lang);
    }

//    @Query (
//            countQuery = "MATCH (n:Property) WHERE n.ontology_name = {0} RETURN count(n)",
//            value = "MATCH (n:Property {ontology_name : {0}}) RETURN n")
    public Page<V1Property> findAllByOntology(String ontologyId, String lang, Pageable pageable)  {

        OlsSolrQuery query = new OlsSolrQuery();
	query.addFilter("lang", lang, Fuzziness.EXACT);
	query.addFilter("type", "property", Fuzziness.EXACT);
	query.addFilter("ontologyId", ontologyId, Fuzziness.EXACT);

        return solrClient.searchSolrPaginated(query, pageable)
                .map(result -> V1PropertyMapper.mapProperty(result, lang));
    }

//    @Query (value = "MATCH (n:Property) WHERE n.ontology_name = {0} AND n.short_form = {1} RETURN n")
    public V1Property findByOntologyAndShortForm(String ontologyId, String shortForm, String lang)  {

        OlsSolrQuery query = new OlsSolrQuery();
        query.addFilter("lang", lang, Fuzziness.EXACT);
        query.addFilter("type", "property", Fuzziness.EXACT);
        query.addFilter("ontologyId", ontologyId, Fuzziness.EXACT);
        query.addFilter("shortForm", shortForm, Fuzziness.EXACT);

        return V1PropertyMapper.mapProperty(solrClient.getOne(query), lang);

    }

//    @Query (value = "MATCH (n:Property) WHERE n.ontology_name = {0} AND n.obo_id = {1} RETURN n")
    public V1Property findByOntologyAndOboId(String ontologyId, String oboId, String lang)  {

        OlsSolrQuery query = new OlsSolrQuery();
        query.addFilter("lang", lang, Fuzziness.EXACT);
        query.addFilter("type", "property", Fuzziness.EXACT);
        query.addFilter("ontologyId", ontologyId, Fuzziness.EXACT);
        query.addFilter("oboId", oboId, Fuzziness.EXACT);

        return V1PropertyMapper.mapProperty(solrClient.getOne(query), lang);

    }

//    @Query (countQuery =  "MATCH (n:Property)-[SUBPROPERTYOF]->(r:Root) WHERE r.ontology_name = {0} AND n.is_obsolete = {1}  RETURN count(n)",
//            value = "MATCH (n:Property)-[SUBPROPERTYOF]->(r:Root) WHERE r.ontology_name = {0} AND n.is_obsolete = {1}  RETURN n")
    public Page<V1Property> getRoots(String ontologyId, boolean obsolete, String lang, Pageable pageable)  {

        OlsSolrQuery query = new OlsSolrQuery();
        query.addFilter("lang", lang, Fuzziness.EXACT);
        query.addFilter("type", "property", Fuzziness.EXACT);
        query.addFilter("ontologyId", ontologyId, Fuzziness.EXACT);
        query.addFilter("hasDirectParent", "false", Fuzziness.EXACT);
        query.addFilter("hasHierarchicalParent", "false", Fuzziness.EXACT);

        if(!obsolete)
            query.addFilter("isObsolete", "false", Fuzziness.EXACT);

        return solrClient.searchSolrPaginated(query, pageable)
                .map(result -> V1PropertyMapper.mapProperty(result, lang));

    }

//    @Query (countQuery = "MATCH (n:Property) RETURN count(n)",
//            value = "MATCH (n:Property) RETURN n")
    public Page<V1Property> findAll(String lang, Pageable pageable)  {

        OlsSolrQuery query = new OlsSolrQuery();
        query.addFilter("lang", lang, Fuzziness.EXACT);
        query.addFilter("type", "property", Fuzziness.EXACT);

        return solrClient.searchSolrPaginated(query, pageable)
                .map(result -> V1PropertyMapper.mapProperty(result, lang));

    }

//    @Query (countQuery = "MATCH (n:Property) WHERE n.is_defining_ontology = true RETURN count(n)",
//            value = "MATCH (n:Property) WHERE n.is_defining_ontology = true RETURN n")
    public Page<V1Property> findAllByIsDefiningOntology(String lang, Pageable pageable)  {

        OlsSolrQuery query = new OlsSolrQuery();
        query.addFilter("lang", lang, Fuzziness.EXACT);
        query.addFilter("type", "property", Fuzziness.EXACT);
        query.addFilter("isDefiningOntology", "true", Fuzziness.EXACT);

        return solrClient.searchSolrPaginated(query, pageable)
                .map(result -> V1PropertyMapper.mapProperty(result, lang));
    }

//    @Query (countQuery = "MATCH (n:Property) WHERE n.iri = {0} RETURN count(n)",
//            value = "MATCH (n:Property) WHERE n.iri = {0} RETURN n")
    public Page<V1Property> findAllByIri(String iri, String lang, Pageable pageable)  {

        OlsSolrQuery query = new OlsSolrQuery();
        query.addFilter("lang", lang, Fuzziness.EXACT);
        query.addFilter("type", "property", Fuzziness.EXACT);
        query.addFilter("iri", iri, Fuzziness.EXACT);

        return solrClient.searchSolrPaginated(query, pageable)
                .map(result -> V1PropertyMapper.mapProperty(result, lang));

    }

//    @Query (countQuery = "MATCH (n:Property) WHERE n.iri = {0} AND n.is_defining_ontology = true "
//            + "RETURN count(n)",
//            value = "MATCH (n:Property) WHERE n.iri = {0} AND n.is_defining_ontology = true RETURN n")
    public Page<V1Property> findAllByIriAndIsDefiningOntology(String iri, String lang, Pageable pageable)  {

        OlsSolrQuery query = new OlsSolrQuery();
        query.addFilter("lang", lang, Fuzziness.EXACT);
        query.addFilter("type", "property", Fuzziness.EXACT);
        query.addFilter("iri", iri, Fuzziness.EXACT);
        query.addFilter("isDefiningOntology", "true", Fuzziness.EXACT);

        return solrClient.searchSolrPaginated(query, pageable)
                .map(result -> V1PropertyMapper.mapProperty(result, lang));
    }

//    @Query (countQuery = "MATCH (n:Property) WHERE n.short_form = {0} RETURN count(n)",
//            value = "MATCH (n:Property) WHERE n.short_form = {0} RETURN n")
    public Page<V1Property> findAllByShortForm(String shortForm, String lang, Pageable pageable)  {

        OlsSolrQuery query = new OlsSolrQuery();
        query.addFilter("lang", lang, Fuzziness.EXACT);
        query.addFilter("type", "property", Fuzziness.EXACT);
        query.addFilter("shortForm", shortForm, Fuzziness.EXACT);

        return solrClient.searchSolrPaginated(query, pageable)
                .map(result -> V1PropertyMapper.mapProperty(result, lang));

    }

//    @Query (countQuery = "MATCH (n:Property) WHERE n.short_form = {0} AND "
//            + "n.is_defining_ontology = true  RETURN count(n)",
//            value = "MATCH (n:Property) WHERE n.short_form = {0} AND n.is_defining_ontology = true "
//                    + "RETURN n")
    public Page<V1Property> findAllByShortFormAndIsDefiningOntology(String shortForm, String lang, Pageable pageable)  {

        OlsSolrQuery query = new OlsSolrQuery();
        query.addFilter("lang", lang, Fuzziness.EXACT);
        query.addFilter("type", "property", Fuzziness.EXACT);
        query.addFilter("shortForm", shortForm, Fuzziness.EXACT);
        query.addFilter("isDefiningOntology", "true", Fuzziness.EXACT);

        return solrClient.searchSolrPaginated(query, pageable)
                .map(result -> V1PropertyMapper.mapProperty(result, lang));

    }

//    @Query (countQuery = "MATCH (n:Property) WHERE n.obo_id = {0} RETURN count(n)",
//            value = "MATCH (n:Property) WHERE n.obo_id = {0} RETURN n")
    public Page<V1Property> findAllByOboId(String oboId, String lang, Pageable pageable)  {

        OlsSolrQuery query = new OlsSolrQuery();
        query.addFilter("lang", lang, Fuzziness.EXACT);
        query.addFilter("type", "property", Fuzziness.EXACT);
        query.addFilter("oboId", oboId, Fuzziness.EXACT);

        return solrClient.searchSolrPaginated(query, pageable)
                .map(result -> V1PropertyMapper.mapProperty(result, lang));

    }

//    @Query (countQuery = "MATCH (n:Property) WHERE n.obo_id = {0} AND n.is_defining_ontology = true "
//            + "RETURN count(n)",
//            value = "MATCH (n:Property) WHERE n.obo_id = {0} AND n.is_defining_ontology = true "
//                    + "RETURN n")
    public Page<V1Property> findAllByOboIdAndIsDefiningOntology(String oboId, String lang, Pageable pageable)  {

        OlsSolrQuery query = new OlsSolrQuery();
        query.addFilter("lang", lang, Fuzziness.EXACT);
        query.addFilter("type", "property", Fuzziness.EXACT);
        query.addFilter("oboId", oboId, Fuzziness.EXACT);
        query.addFilter("isDefiningOntology", "true", Fuzziness.EXACT);

        return solrClient.searchSolrPaginated(query, pageable)
                .map(result -> V1PropertyMapper.mapProperty(result, lang));

    }
}
