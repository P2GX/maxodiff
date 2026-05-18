# Overview

`maxodiff` is an approach to formalizing the quality of a differential diagnosis and the improvements to be expected by performing a diagnostic procedure. `maxodiff` can be used to predict the most useful diagnostic modality given the current status of a differential diagnosis that includes
a set of HPO terms (representing clinical manifestions of a disease), and a list of [Medical Action Ontology (MAxO)](https://www.ebi.ac.uk/ols4/ontologies/maxo) terms (representing the diagnostic procedures that have already been performed).

We implement maxodiff as a Java application that can be run as a stand-alone command line app or can be accessed from 
[Symptom annotation made simple (SAMS)](https://www.genecascade.org/sams-cgi/index.cgi), a database and phenotyping tool that is designed to support phenotyping using symptoms and clinical signs from the [Human Phenotype Ontology](https://hpo.jax.org/app/).





!!! danger "🚨 TODO"
    <span class="todo-pulse">Add link to the specific SAMS page when it is running</span>


## Feedback

The best place to leave feedback, ask questions, and report bugs is the [maxodiff Issue Tracker](https://github.com/monarch-initiative/maxodiff/issues).