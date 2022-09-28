
package uk.ac.ebi.spot.ols.repository.v1;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.ols.model.v1.V1Ontology;
import uk.ac.ebi.spot.ols.model.v2.V2Ontology;
import uk.ac.ebi.spot.ols.repository.Neo4jQueryHelper;
import uk.ac.ebi.spot.ols.repository.OlsSolrQuery;
import uk.ac.ebi.spot.ols.repository.SolrQueryHelper;
import uk.ac.ebi.spot.ols.repository.Validation;
import uk.ac.ebi.spot.ols.service.OntologyEntity;

@Component
public class V1OntologyRepository {

    @Autowired
    SolrQueryHelper solrQueryHelper;

    public V1Ontology get(String ontologyId, String lang) {

        Validation.validateLang(lang);
        Validation.validateOntologyId(ontologyId);

        OlsSolrQuery query = new OlsSolrQuery();
	query.addFilter("lang", lang, true);
	query.addFilter("type", "ontology", true);
	query.addFilter("ontologyId", ontologyId, true);

        return new V1Ontology(solrQueryHelper.getOne(query), lang);
    }

    public Page<V1Ontology> getAll(String lang, Pageable pageable) {

        Validation.validateLang(lang);

        OlsSolrQuery query = new OlsSolrQuery();
	query.addFilter("lang", lang, true);
	query.addFilter("type", "ontology", true);

        return solrQueryHelper.searchSolrPaginated(query, pageable)
                .map(result -> new V1Ontology(result, lang));
    }

    public Map<String, V1Ontology> getOntologyMapForEntities(List<OntologyEntity> entities, String lang) {

	Map<String, V1Ontology> res = new HashMap<>();

	for(OntologyEntity entity : entities) {

		String ontologyId = entity.getString("ontologyId");

		if(res.containsKey(ontologyId)) {
			continue;
		}

		res.put(ontologyId, get(ontologyId, lang));
	}

	return res;
    }
}
