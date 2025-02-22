
import Multimap from "multimap"
import Entity from "../../model/Entity"
import assert from "assert";


/* Unflattens a list of entities into:
 *	- A list of root entities
 * 	- A mapping of URI to list of child entities
 * 
 * Used by EntityTree and EntityGraph
 */
export default function extractEntityHierarchy(
	entities:Entity[]
):({
	rootEntities:Entity[],
	uriToChildNodes:Multimap<string, Entity>
}) {
	let uriToNode: Map<string, Entity> = new Map()
	let uriToChildNodes: Multimap<string, Entity> = new Multimap()
	let uriToParentNodes: Multimap<string, Entity> = new Multimap()

	for (let entity of entities) {
		uriToNode.set(entity.getIri(), entity)
	}

	for (let entity of entities) {

		let parents = entity.getParents()
			.map(parent => parent.value)
			// not interested in bnode subclassofs like restrictions etc
			.filter(parent => typeof parent === 'string')
			.map(parentUri => uriToNode.get(parentUri))
			.filter(parent => parent !== undefined)

		for (let parent of parents) {
			assert(parent)
			uriToChildNodes.set(parent.getIri(), entity)
			uriToParentNodes.set(entity.getIri(), parent)
		}
	}

	let rootEntities = entities.filter((node) => {
		return (uriToParentNodes.get(node.getIri()) || []).length === 0
	})

	return { rootEntities, uriToChildNodes }
}


