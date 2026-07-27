# Background


## Contents:
- [What is a differential diagnosis?](#what-is-a-differential-diagnosis)
- [How does maxodiff work?](#how-does-maxodiff-work)
- [GA4GH Phenopackets](#ga4gh-phenopackets)
- [Feedback](#feedback)


## What is a differential diagnosis?

The process of differential diagnosis aims to identify the etiology of a condition by an evaluation of patient history, physical examination findings, and in many cases laboratory data or imaging tests. The result of this process can be conceptualized as a list of candidate diagnoses, and the goal of the process is to find the correct diagnosis in order to plan clinical management optimally.

The [Human Phenotype Ontology](https://hpo.jax.org/){:target="_blank"} (HPO) provides a semantically unified framework of knowledge on diseases, genes, and phenotypes that is used to support phenotype-driven variant prioritization in diagnostic sequencing. The HPO is wiedely used to support
differential diagnosis in medical genetics and other fields. The [Medical Action Ontology](https://github.com/monarch-initiative/maxo){:target="_blank"} (MAxO) was developed to organize medical procedures, therapies and interventions in a structured way. MAxO contains terms describing activities and measures undertaken as a part of clinical management that collectively we refer to as medical actions. In addition to pharmaceutical treatment, medical actions include surgical procedures, ablations, treatment with biologics, behavioral and cognitive interventions, deep brain stimulation and many others. The MAxO project additionally provides annotations of diagnostic modalities (represented as MAxO terms) for specific clinical findings (represented as HPO terms).

!!! quote "Example"
    For instance, one such annotation specifies that the HPO term [Corneal crystals](https://hpo.jax.org/browse/term/HP:0000531){:target="_blank"} is_observable_through slit-lamp examination (MAXO:0000973). 

Corneal crystals are tiny crystalline deposits within the cornea that appear as shiny, refractile spots on slit-lamp exam. Several diseases can manifest corneal crystals, including [Cystinosis](https://hpo.jax.org/browse/disease/OMIM:219900) and [Bietti crystalline dystrophy](https://hpo.jax.org/browse/disease/OMIM:210370){:target="_blank"} and others, and the finding of Corneal crystals would tend to make these diseases rank more highly in a differential, and correspondingly reduce the rank of other diseases no characterized by Cornal crystals.


## GA4GH Phenopackets

The input required for maxodiff is a [Global Alliance for Genomics and Health](https://www.ga4gh.org/){:target="_blank"}  (GA4GH) phenopacket.
The GA4GH [Phenopacket Schema](https://phenopacket-schema.readthedocs.io/en/latest/){:target="_blank"} is a standard for sharing disease and phenotype information characterizing an individual person or biosample that addresses the challenge of documenting case-level phenotypic information (see [Jacobsen at el., 2022](https://pubmed.ncbi.nlm.nih.gov/35705716/){:target="_blank"}, for more information).  Each phenopacket describes on individual and can include information about phenotypic descriptions, numerical measurements, genetic information, diagnoses, and treatments. To run the Web version of maxodiff, only a phenopacket is required. A little more setup is required for the command-line version (see tutorial).

To test the software, users may obtain phenopackets from [Phenopacket Store](https://github.com/monarch-initiative/phenopacket-store){:target="_blank"} (See also [Danis et al, 2024](https://pubmed.ncbi.nlm.nih.gov/39394689/){:target="_blank"}). Currently, over 8000 phenopackets are available.

We have developed software libraries that may be helpful in converting existing data to phenopacket format.

- [SAMS](https://www.genecascade.org/sams-cgi/index.cgi): Symptom annotation made simple - SAMS is a database and phenotyping tool for precision medicine that includes numerous functionalities including the creation of phenopackets. 
- [phenopacket-tools](https://github.com/phenopackets/phenopacket-tools){:target="_blank"}: (See also [Danis et al., 2024](https://pubmed.ncbi.nlm.nih.gov/37196000/){:target="_blank"}): Java library and command-line application
- [phenopackets ](https://pypi.org/project/phenopackets/){:target="_blank"}: Python library
- [phenopacket-schema](https://mvnrepository.com/artifact/org.phenopackets/phenopacket-schema){:target="_blank"}: Java library





