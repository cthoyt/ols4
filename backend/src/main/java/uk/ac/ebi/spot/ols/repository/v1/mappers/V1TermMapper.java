package uk.ac.ebi.spot.ols.repository.v1.mappers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import uk.ac.ebi.spot.ols.model.v1.*;
import uk.ac.ebi.spot.ols.repository.transforms.LocalizationTransform;
import uk.ac.ebi.spot.ols.repository.v1.JsonHelper;

import java.util.*;

public class V1TermMapper {

    public static V1Term mapTerm(JsonElement json, String lang) {

        V1Term term = new V1Term();

        JsonObject localizedJson = Objects.requireNonNull(LocalizationTransform.transform(json, lang))
                .getAsJsonObject();

        term.lang = lang;
        term.iri = JsonHelper.getString(localizedJson, "iri");

        term.ontologyName = JsonHelper.getString(localizedJson, "ontologyId");
        term.ontologyPrefix = JsonHelper.getString(localizedJson, "ontologyPreferredPrefix");
        term.ontologyIri = JsonHelper.getString(localizedJson, "ontologyIri");

        term.shortForm = JsonHelper.getString(localizedJson, "shortForm");

        int lastUnderscore = term.shortForm.lastIndexOf("_");
        if(lastUnderscore != -1) {
            term.oboId = term.shortForm.substring(0, lastUnderscore) + ":"  + term.shortForm.substring(lastUnderscore + 1);
        } else {
            term.oboId = term.shortForm;
        }

        term.label = JsonHelper.getString(localizedJson, "label");
        term.description = JsonHelper.getStrings(localizedJson, "definition").toArray(new String[0]);
        term.synonyms = JsonHelper.getStrings(localizedJson, "synonym").toArray(new String[0]);
        term.annotation = AnnotationExtractor.extractAnnotations(localizedJson);
        term.inSubsets = AnnotationExtractor.extractSubsets(localizedJson);

        term.oboDefinitionCitations = V1OboDefinitionCitationExtractor.extractFromJson(localizedJson);
        term.oboXrefs = V1OboXrefExtractor.extractFromJson(localizedJson);
        term.oboSynonyms = V1OboSynonymExtractor.extractFromJson(localizedJson);
        term.isPreferredRoot = false;

        term.isDefiningOntology = Boolean.parseBoolean(JsonHelper.getString(localizedJson, "isDefiningOntology"));

        term.hasChildren = Boolean.parseBoolean(JsonHelper.getString(localizedJson, "hasDirectChildren"))
                || Boolean.parseBoolean(JsonHelper.getString(localizedJson, "hasHierarchicalChildren"));

        term.isRoot = !(
                Boolean.parseBoolean(JsonHelper.getString(localizedJson, "hasDirectParent")) ||
                        Boolean.parseBoolean(JsonHelper.getString(localizedJson, "hasHierarchicalParent"))
        );

        term.isObsolete = Boolean.parseBoolean(JsonHelper.getString(localizedJson, "isObsolete"));


        List<JsonElement> replacedBy = JsonHelper.getValues(localizedJson, "http://purl.obolibrary.org/obo/IAO_0100001");

        if(replacedBy.size() > 0) {
            // TODO: fake loop only keeps first, check ols3 behaviour
            for(JsonElement el : replacedBy) {
                if(el.isJsonPrimitive()) {
                    // URI, shorten
                    term.termReplacedBy = ShortFormExtractor.extractShortForm(el.getAsString());
                } else {
                    // literal, get value but don't shorten
                    // TODO: what if reified?
                    //
                    term.termReplacedBy = el.getAsJsonObject().get("value").getAsString();
                }
                break;
            }
        }

        JsonObject referencedEntities = localizedJson.getAsJsonObject("referencedEntities");

        term.related = new ArrayList<>();

        for(JsonObject relatedTo : JsonHelper.getObjects(localizedJson, "relatedTo")) {

            String predicate = relatedTo.getAsJsonPrimitive("property").getAsString();

	    JsonElement referencedEntity = referencedEntities.getAsJsonObject().get(predicate);
            String label = referencedEntity != null ?
	    	JsonHelper.getString(referencedEntity.getAsJsonObject(), "label") : ShortFormExtractor.extractShortForm(predicate);

            V1Related relatedObj = new V1Related();
            relatedObj.iri = predicate;
            relatedObj.label = label;
            relatedObj.ontologyName = term.ontologyName;
            relatedObj.relatedFromIri = term.iri;
            relatedObj.relatedToIri = relatedTo.getAsJsonPrimitive("value").getAsString();
            term.related.add(relatedObj);
        }

        return term;
    }


}
