package uk.ac.ebi.spot.ols.controller.api.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;
import uk.ac.ebi.spot.ols.controller.api.v2.helpers.DynamicQueryHelper;
import uk.ac.ebi.spot.ols.controller.api.v2.responses.V2PagedAndFacetedResponse;
import uk.ac.ebi.spot.ols.controller.api.v2.responses.V2PagedResponse;
import uk.ac.ebi.spot.ols.model.v2.V2Entity;
import uk.ac.ebi.spot.ols.repository.solr.OlsFacetedResultsPage;
import uk.ac.ebi.spot.ols.repository.v2.V2ClassRepository;
import uk.ac.ebi.spot.ols.repository.v2.V2EntityRepository;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/api/v2")
public class V2ClassController {

    @Autowired
    V2ClassRepository classRepository;

    @RequestMapping(path = "/classes", produces = {MediaType.APPLICATION_JSON_VALUE }, method = RequestMethod.GET)
    public HttpEntity<V2PagedAndFacetedResponse<V2Entity>> getClasses(
            @PageableDefault(size = 20, page = 0) Pageable pageable,
            @RequestParam(value = "lang", required = false, defaultValue = "en") String lang,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "searchFields", required = false) String searchFields,
            @RequestParam(value = "boostFields", required = false) String boostFields,
            @RequestParam Map<String,String> searchProperties
    ) throws ResourceNotFoundException, IOException {

	Map<String,String> properties = new HashMap<>(Map.of("isObsolete", "false"));
	properties.putAll(searchProperties);

        return new ResponseEntity<>(
                new V2PagedAndFacetedResponse(
                    classRepository.find(pageable, lang, search, searchFields, boostFields, DynamicQueryHelper.filterProperties(properties))
                ),
                HttpStatus.OK
        );
    }

    @RequestMapping(path = "/ontologies/{onto}/classes", produces = {MediaType.APPLICATION_JSON_VALUE}, method = RequestMethod.GET)
    public HttpEntity<V2PagedAndFacetedResponse<V2Entity>> getClasses(
            @PageableDefault(size = 20, page = 0) Pageable pageable,
            @PathVariable("onto") @NotNull String ontologyId,
            @RequestParam(value = "lang", required = false, defaultValue = "en") String lang,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "searchFields", required = false) String searchFields,
            @RequestParam(value = "boostFields", required = false) String boostFields,
            @RequestParam Map<String,String> searchProperties
    ) throws ResourceNotFoundException, IOException {

	Map<String,String> properties = new HashMap<>(Map.of("isObsolete", "false"));
	properties.putAll(searchProperties);

        return new ResponseEntity<>(
                new V2PagedAndFacetedResponse(
                    classRepository.findByOntologyId(ontologyId, pageable, lang, search, searchFields, boostFields,  DynamicQueryHelper.filterProperties(properties))
                ),
                HttpStatus.OK);
    }

    @RequestMapping(path = "/ontologies/{onto}/classes/{class}", produces = {MediaType.APPLICATION_JSON_VALUE }, method = RequestMethod.GET)
    public HttpEntity<V2Entity> getClass(
            @PathVariable("onto") String ontologyId,
            @PathVariable("class") String iri,
            @RequestParam(value = "lang", required = false, defaultValue = "en") String lang
    ) throws ResourceNotFoundException {

        iri = UriUtils.decode(iri, "UTF-8");

        V2Entity entity = classRepository.getByOntologyIdAndIri(ontologyId, iri, lang);
        if (entity == null) throw new ResourceNotFoundException();
        return new ResponseEntity<>( entity, HttpStatus.OK);
    }

    @RequestMapping(path = "/ontologies/{onto}/classes/{class}/children", produces = {MediaType.APPLICATION_JSON_VALUE}, method = RequestMethod.GET)
    public HttpEntity<V2PagedResponse<V2Entity>> getChildrenByOntology(
            @PageableDefault(size = 20, page = 0) Pageable pageable,
            @PathVariable("onto") String ontologyId,
            @PathVariable("class") String iri,
            @RequestParam(value = "lang", required = false, defaultValue = "en") String lang
    ) throws ResourceNotFoundException {

        iri = UriUtils.decode(iri, "UTF-8");

        return new ResponseEntity<>(
                new V2PagedResponse<>(
                    classRepository.getChildrenByOntologyId(ontologyId, pageable, iri, lang)
                ),
                HttpStatus.OK);
    }

    @RequestMapping(path = "/ontologies/{onto}/classes/{class}/ancestors", produces = {MediaType.APPLICATION_JSON_VALUE }, method = RequestMethod.GET)
    public HttpEntity<V2PagedResponse<V2Entity>> getAncestorsByOntology(
            @PageableDefault(size = 20, page = 0) Pageable pageable,
            @PathVariable("onto") String ontologyId,
            @PathVariable("class") String iri,
            @RequestParam(value = "lang", required = false, defaultValue = "en") String lang
    ) throws ResourceNotFoundException {

        iri = UriUtils.decode(iri, "UTF-8");

        return new ResponseEntity<>(
                new V2PagedResponse<>(
                    classRepository.getAncestorsByOntologyId(ontologyId, pageable, iri, lang)
                ),
                HttpStatus.OK
        );
    }


    // The ancestors of individuals are classes. So, the /ancestors endpoint is part of the Class controller.
    //
    @RequestMapping(path = "/ontologies/{onto}/individuals/{individual}/ancestors", produces = {MediaType.APPLICATION_JSON_VALUE }, method = RequestMethod.GET)
    public HttpEntity<V2PagedResponse<V2Entity>> getIndividualAncestorsByOntology(
            @PageableDefault(size = 20, page = 0) Pageable pageable,
            @PathVariable("onto") String ontologyId,
            @PathVariable("individual") String iri,
            @RequestParam(value = "lang", required = false, defaultValue = "en") String lang
    ) throws ResourceNotFoundException {

        iri = UriUtils.decode(iri, "UTF-8");

        return new ResponseEntity<>(
                new V2PagedResponse<>(
                    classRepository.getIndividualAncestorsByOntologyId(ontologyId, pageable, iri, lang)
                ),
                HttpStatus.OK);

    }
}

