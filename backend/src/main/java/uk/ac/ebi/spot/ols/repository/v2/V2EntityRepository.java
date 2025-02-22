
package uk.ac.ebi.spot.ols.repository.v2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.ols.model.v2.V2Entity;
import uk.ac.ebi.spot.ols.repository.neo4j.OlsNeo4jClient;
import uk.ac.ebi.spot.ols.repository.solr.Fuzziness;
import uk.ac.ebi.spot.ols.repository.solr.OlsFacetedResultsPage;
import uk.ac.ebi.spot.ols.repository.solr.OlsSolrQuery;
import uk.ac.ebi.spot.ols.repository.solr.OlsSolrClient;
import uk.ac.ebi.spot.ols.repository.Validation;
import uk.ac.ebi.spot.ols.repository.transforms.LocalizationTransform;
import uk.ac.ebi.spot.ols.repository.transforms.RemoveLiteralDatatypesTransform;
import uk.ac.ebi.spot.ols.repository.v2.helpers.V2DynamicFilterParser;
import uk.ac.ebi.spot.ols.repository.v2.helpers.V2SearchFieldsParser;

import java.io.IOException;
import java.util.Map;

@Component
public class V2EntityRepository {

    @Autowired
    OlsSolrClient solrClient;

    @Autowired
    OlsNeo4jClient neo4jClient;


    public OlsFacetedResultsPage<V2Entity> find(
            Pageable pageable, String lang, String search, String searchFields, String boostFields, String facetFields, Map<String,String> properties) throws IOException {

        Validation.validateLang(lang);

        OlsSolrQuery query = new OlsSolrQuery();
        query.addFilter("lang", lang, Fuzziness.EXACT);
        query.addFilter("type", "entity", Fuzziness.EXACT);
        V2SearchFieldsParser.addSearchFieldsToQuery(query, searchFields);
        V2SearchFieldsParser.addBoostFieldsToQuery(query, boostFields);
        V2SearchFieldsParser.addFacetFieldsToQuery(query, facetFields);
        V2DynamicFilterParser.addDynamicFiltersToQuery(query, properties);
        query.setSearchText(search);

        return solrClient.searchSolrPaginated(query, pageable)
                .map(RemoveLiteralDatatypesTransform::transform)
                .map(e -> LocalizationTransform.transform(e, lang))
                .map(V2Entity::new);
    }

    public OlsFacetedResultsPage<V2Entity> findByOntologyId(
            String ontologyId, Pageable pageable, String lang, String search, String searchFields, String boostFields, String facetFields, Map<String,String> properties) throws IOException {

        Validation.validateOntologyId(ontologyId);
        Validation.validateLang(lang);

        OlsSolrQuery query = new OlsSolrQuery();
        query.addFilter("lang", lang, Fuzziness.EXACT);
        query.addFilter("type", "entity", Fuzziness.EXACT);
        query.addFilter("ontologyId", ontologyId, Fuzziness.EXACT);
        V2SearchFieldsParser.addSearchFieldsToQuery(query, searchFields);
        V2SearchFieldsParser.addBoostFieldsToQuery(query, boostFields);
        V2SearchFieldsParser.addFacetFieldsToQuery(query, facetFields);
        V2DynamicFilterParser.addDynamicFiltersToQuery(query, properties);
        query.setSearchText(search);

        return solrClient.searchSolrPaginated(query, pageable)
                .map(RemoveLiteralDatatypesTransform::transform)
                .map(e -> LocalizationTransform.transform(e, lang))
                .map(V2Entity::new);
    }

    public V2Entity getByOntologyIdAndIri(String ontologyId, String iri, String lang) throws ResourceNotFoundException {

        Validation.validateOntologyId(ontologyId);
        Validation.validateLang(lang);

        OlsSolrQuery query = new OlsSolrQuery();
        query.addFilter("lang", lang, Fuzziness.EXACT);
        query.addFilter("type", "entity", Fuzziness.EXACT);
        query.addFilter("ontologyId", ontologyId, Fuzziness.EXACT);
        query.addFilter("iri", iri, Fuzziness.EXACT);

        return new V2Entity(
                LocalizationTransform.transform(
                        RemoveLiteralDatatypesTransform.transform(
                                solrClient.getOne(query)
                        ),
                        lang
                )
        );
    }


}


