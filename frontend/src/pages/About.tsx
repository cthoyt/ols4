import { Fragment } from "react";
import Header from "../components/Header";

export default function About() {
  document.title = "Ontology Lookup Service (OLS)";
  return (
    <Fragment>
      <Header section="about" />
      <main className="container mx-auto">About OLS</main>
    </Fragment>
  );
}
