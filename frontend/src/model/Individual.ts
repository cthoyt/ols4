import { asArray } from "../app/util";
import Entity from "./Entity";

export default class Individual extends Entity {
  getParents() {
    return [];
  }
  getEquivalents() {
    return [];
  }

  getDifferentFrom() {
    return asArray(
      this.properties["http://www.w3.org/2002/07/owl#differentFrom"]
    );
  }

  getSameAs() {
    return asArray(this.properties["http://www.w3.org/2002/07/owl#sameAs"]);
  }

  getIndividualTypes(): string[] {
    const rdfTypes: any = this.getRdfTypes();

    if (!rdfTypes || !Array.isArray(rdfTypes)) {
      return [];
    }

    return rdfTypes.filter(
      (t: any) =>
        typeof t === "string" &&
        t !== "http://www.w3.org/2002/07/owl#NamedIndividual" &&
        !t.startsWith("http://www.w3.org/2000/01/rdf-schema#")
    );
  }
}
