# Overview

`maxodiff` intends to support the decision that is commonly encountered in rare-disease diagnostics:
Which diagnostic test should be performed next to provide the greatest amount of information that
could improve the accuracy of the differential diagnosis?

Given a patient's phenotypic features (as [HPO](https://hpo.jax.org/) terms) and a
differential diagnosis, maxodiff estimates, for each candidate diagnostic
procedure from the [Medical Action Ontology (MAxO)](https://www.ebi.ac.uk/ols4/ontologies/maxo),
how much the ranking of the differential diagnosis is expected to change if the
procedure is performed. Procedures that are likely to reveal new, discriminating
findings are ranked highest.

Maxodiff is implemented as a Java application that can be run as a stand-alone command line app,
run as an HTML server, or can be accessed from 
[Symptom annotation made simple (SAMS)](https://www.genecascade.org/sams-cgi/index.cgi), a database and phenotyping tool.




## Inputs and Outputs

| Input                                                                                                      | Output                                                                                    |
|------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------|
| A [GA4GH phenopacket](background.md#ga4gh-phenopackets) with the patient's observed and excluded HPO terms | A list of MAxO procedures, ordered by their expected effect on the differential diagnosis |
| The number of top-ranked diseases to consider and the number of simulation repetitions                     | For each procedure, which diseases are expected to move up or down, and by how much       |

## Ways to run maxodiff

| | Web server | Command line |
|---|---|---|
| Best for | Trying it out on a single patient; integration in other tools such as [SAMS](https://www.genecascade.org/sams-cgi/index.cgi) | Batch analyses, scripting, offline use |
| Output | Interactive page, or JSON | Self-contained HTML file |
| Documentation | [Server setup](setup-server.md), [Web server output](json.md) | [Setup](setup.md), [Command line app](app.md) |

## Where to go next

- New to the topic? Read the [Background](background.md): differential diagnosis,
  HPO, MAxO, and how the simulation works.
- Want to run it yourself? Start with [Setup](setup.md).
- Want to understand a results page? See [Reading the results](app.md).
- Contributing to the code? See the [developer notes](developers.md).

## Feedback

The best place to leave feedback, ask questions, and report bugs is the
[maxodiff issue tracker](https://github.com/monarch-initiative/maxodiff/issues).

