package uk.ac.ebi.spot.ols.repository.v1;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class V1AncestorsJsTreeBuilder {

    JsonObject thisEntity;
    List<String> parentRelationIRIs;
    Set<JsonElement> entities = new LinkedHashSet<>();
    Map<String, JsonElement> entityIriToEntity = new HashMap<>();
    Multimap<String, String> entityIriToChildIris = HashMultimap.create();

    public V1AncestorsJsTreeBuilder(JsonElement thisEntity, List<JsonElement> ancestors, List<String> parentRelationIRIs) {

        this.thisEntity = thisEntity.getAsJsonObject();
        this.parentRelationIRIs = parentRelationIRIs;

        // 1. put all entities (this entity + all ancestors) into an ordered set

        entities.add(thisEntity);
        entities.addAll(ancestors);

        // 2. establish map of IRI -> entity

        for(JsonElement entity : entities) {
            entityIriToEntity.put((String) entity.getAsJsonObject().getAsJsonPrimitive("iri").getAsString(), entity);
        }

        // 3. establish map of IRI -> children

        for(String entityIri : entityIriToEntity.keySet()) {

            JsonElement entity = entityIriToEntity.get(entityIri);

            for (String parentIri : getEntityParentIRIs(entity)) {
                entityIriToChildIris.put(parentIri, entity.getAsJsonObject().get("iri").getAsString());
            }
        }
    }

    List<Map<String,Object>> buildJsTree() {

        // 1. establish roots (entities with no parents)

        List<JsonElement> roots = entities.stream()
                .filter(entity -> getEntityParentIRIs(entity).size() == 0)
                .collect(Collectors.toList());

        // 2. build jstree entries starting with roots

        List<Map<String,Object>> jstree = new ArrayList<>();

        for(JsonElement root : roots) {
            createJsTreeEntries(jstree, root.getAsJsonObject(), null);
        }

        return jstree;
    }

    private void createJsTreeEntries(List<Map<String,Object>> jstree, JsonObject entity, String concatenatedParentIris) {

        Map<String,Object> jstreeEntry = new LinkedHashMap<>();

        if(concatenatedParentIris != null) {
            jstreeEntry.put("id", base64Encode(concatenatedParentIris + ";" + entity.get("iri").getAsString()));
            jstreeEntry.put("parent", base64Encode(concatenatedParentIris));
        } else {
            jstreeEntry.put("id", base64Encode(entity.get("iri").getAsString()));
            jstreeEntry.put("parent", "#");
        }

        jstreeEntry.put("iri", entity.get("iri").getAsString());
        jstreeEntry.put("text", entity.get("label").getAsString()); // TODO what in the case of multiple labels?

        Collection<String> childIris = entityIriToChildIris.get(entity.get("iri").getAsString());

        // only the leaf node is selected (= highlighted in the tree)
        boolean selected = thisEntity.get("iri").getAsString().equals(entity.get("iri").getAsString());

        // only nodes that aren't the leaf node are marked as opened (expanded)
        boolean opened = (!selected);

        // only nodes that aren't already opened are marked as having children, (iff they actually have children!)
        boolean children = (!opened) && 
		(entity.get("hasDirectChildren").getAsString().equals("true") || entity.get("hasHierarchicalChildren").getAsString().equals("true"));

        //boolean children = childIris.size() > 0;

        Map<String,Boolean> state = new LinkedHashMap<>();
        state.put("opened", opened);

        if(selected) {
            state.put("selected", true);
        }

        jstreeEntry.put("state", state);
        jstreeEntry.put("children", children);

        Map<String,Object> attrObj = new LinkedHashMap<>();
        attrObj.put("iri", entity.get("iri"));
        attrObj.put("ontology_name", entity.get("ontologyId"));
        attrObj.put("title", entity.get("iri"));
        attrObj.put("class", "is_a");
        jstreeEntry.put("a_attr", attrObj);

        jstreeEntry.put("ontology_name", entity.get("ontologyId"));

        jstree.add(jstreeEntry);

        for(String childIri : childIris) {

            JsonElement child = entityIriToEntity.get(childIri);

            if(child == null) {
                // child is not in this tree (i.e. cousin of the node requested, will not be displayed)
                continue;
            }

            if(concatenatedParentIris != null) {
                createJsTreeEntries(jstree, child.getAsJsonObject(), concatenatedParentIris + ";" + entity.get("iri"));
            } else {
                createJsTreeEntries(jstree, child.getAsJsonObject(), entity.getAsJsonObject("iri").getAsString());
            }
        }
    }

    private Set<String> getEntityParentIRIs(JsonElement entity) {

        List<JsonElement> parents = new ArrayList<>();

        for(String parentRelationIri : parentRelationIRIs) {
            parents.addAll( JsonHelper.getValues(entity.getAsJsonObject(), parentRelationIri) );
        }

        Set<String> parentIris = new LinkedHashSet<>();

        for (JsonElement parent : parents) {

            // extract value from reified parents
            while(parent.isJsonObject()) {
                parent = parent.getAsJsonObject().get("value");
            }

            String parentIri = parent.getAsString();

            if(parentIri.equals("http://www.w3.org/2002/07/owl#Thing")
                    || parentIri.equals("http://www.w3.org/2002/07/owl#TopObjectProperty")) {
                continue;
            }

            parentIris.add(parentIri);
        }

        return parentIris;
    }

    static String base64Encode(String str) {
        return Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));
    }
}

