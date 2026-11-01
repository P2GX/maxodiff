# Background


## Contents:
- [What is a differential diagnosis?](#what-is-a-differential-diagnosis)
- [How does maxodiff work?](#how-does-maxodiff-work)
- [GA4GH Phenopackets](#ga4gh-phenopackets)
- [Feedback](#feedback)


## What is a differential diagnosis?

The process of differential diagnosis aims to identify the cause of a condition
by evaluating the patient's history, physical examination findings, and in many cases
laboratory data or imaging. The result  of this process can be conceptualized 
as a ranked list of  candidate diagnoses. The goal is to find the correct diagnosis 
so that clinical management can be planned optimally.

Every further test or examination can change this list: a new finding makes some
diagnoses more likely and others less likely. An assumption of our method, and one
that is also taught to medical students, is that a test whose outcome does not
change clinical management is useless. Computationally, we model this by prioritizing
tests that are expected to change the list the most.

## HPO and MAxO

The [Human Phenotype Ontology](https://hpo.jax.org/) (HPO) provides a
semantically unified framework of knowledge on diseases, genes, and phenotypes.
It is widely used to support phenotype-driven differential diagnosis and
variant prioritization in medical genetics and other fields.

The [Medical Action Ontology](https://github.com/monarch-initiative/maxo) (MAxO)
organizes medical procedures, therapies, and interventions, collectively called
*medical actions*. These include pharmaceutical treatment, surgery, behavioral
and cognitive interventions, and diagnostic procedures. The MAxO project also
provides annotations that link diagnostic procedures (MAxO terms) to the
clinical findings (HPO terms) that they can reveal.

> **Example.** One such annotation states that the HPO term
> [Corneal crystals](https://hpo.jax.org/browse/term/HP:0000531) is observable
> through slit-lamp examination (MAXO:0000973).

Corneal crystals are tiny crystalline deposits in the cornea that appear as
shiny, refractile spots on slit-lamp examination. Several diseases can manifest
with them, including
[Cystinosis](https://hpo.jax.org/browse/disease/OMIM:219900) and
[Bietti crystalline dystrophy](https://hpo.jax.org/browse/disease/OMIM:210370).
Finding corneal crystals would make these diseases rank higher in the
differential and lower the rank of diseases that are not characterized by
corneal crystals.

## How does maxodiff work?

For a given patient, maxodiff proceeds as follows:

1. **Initial ranking.** The patient's observed and excluded HPO terms are used
   to rank diseases with the Phenomizer algorithm. The top-ranked diseases are
   the differential diagnosis that maxodiff will try to improve.
2. **Candidate procedures.** For every MAxO procedure, maxodiff collects the HPO
   terms that the procedure can reveal and that are not already part of the
   patient's description.
3. **Simulating the result of a procedure.** We do not know what a procedure
   will find in this patient, so maxodiff simulates it. It draws a plausible set
   of new HPO terms from the revealable terms. Terms that occur in highly ranked
   candidate diseases are drawn more often than terms that occur only in lower
   ranked ones, because the top candidates are more likely to be the correct
   diagnosis.
4. **Re-ranking.** The drawn terms are added to the patient's description and
   the diseases are ranked again.
5. **Repetition.** Steps 3 and 4 are repeated many times (the number of
   repetitions is a parameter), each time starting from the original patient.
   For each disease, maxodiff records its average rank across repetitions.
6. **Comparison.** The average rank of each disease is compared with its initial
   rank. Procedures that move the diseases of the differential diagnosis
   apart, so that they can be told from each other, are ranked highest.

Because the procedure's result is simulated by random sampling, repeated runs
give slightly different numbers. Using more repetitions makes the results more
stable at the cost of a longer running time.

See [Reading the results](app.md) for how these numbers are displayed.

## GA4GH Phenopackets

The input for maxodiff is a
[Global Alliance for Genomics and Health](https://www.ga4gh.org/){:target="_blank"} (GA4GH)
phenopacket. The
[Phenopacket Schema](https://phenopacket-schema.readthedocs.io/en/latest/) is a
standard for sharing disease and phenotype information about an individual
person or biosample (see
[Jacobsen et al., 2022](https://pubmed.ncbi.nlm.nih.gov/35705716/){:target="_blank"}). Each
phenopacket describes one individual and can include phenotypic descriptions,
measurements, genetic information, diagnoses, and treatments.

maxodiff uses the observed and excluded HPO terms of the phenopacket. To run the
web version, only a phenopacket is required. The command-line version needs a
little more [setup](setup.md).

To try the software, you can download phenopackets from
[Phenopacket Store](https://github.com/monarch-initiative/phenopacket-store){:target="_blank"}
(see also [Danis et al., 2025](https://pubmed.ncbi.nlm.nih.gov/39394689/){:target="_blank"}),
which currently provides over 10,000 phenopackets.

The following tools can help to convert existing data to phenopacket format:

- [SAMS](https://www.genecascade.org/sams-cgi/index.cgi){:target="_blank"} (Symptom annotation
  made simple): a database and phenotyping tool for precision medicine that can
  create phenopackets.
- [phenopacket-tools](https://github.com/phenopackets/phenopacket-tools){:target="_blank"}: Java
  library and command-line application (see also
  [Danis et al., 2023](https://pubmed.ncbi.nlm.nih.gov/37196000/){:target="_blank"}).
- [phenopackets](https://pypi.org/project/phenopackets/){:target="_blank"}: Python library.
- [phenopacket-schema](https://mvnrepository.com/artifact/org.phenopackets/phenopacket-schema){:target="_blank"}:
  Java library.