package org.monarchinitiative.maxodiff.core.model;

import org.monarchinitiative.maxodiff.core.io.PhenopacketImporter;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.*;
import java.nio.file.Path;
import org.phenopackets.schema.v2.Phenopacket;
import org.phenopackets.schema.v2.core.Disease;
import org.phenopackets.schema.v2.core.OntologyClass;
import org.phenopackets.schema.v2.core.PhenotypicFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class PhenopacketData {
    private final static Logger LOGGER = LoggerFactory.getLogger(PhenopacketData.class);

    private final String sampleId;
    private final List<String> hpoTerms;
    private final List<String> negatedHpoTerms;
    private final List<TermId> diseaseIds;


    PhenopacketData(String sampleId,
                    List<String> hpoTerms,
                    List<String> negatedHpoTerms,
                    List<TermId> diseaseIds
                  ) {

        this.sampleId = Objects.requireNonNull(sampleId);
        this.hpoTerms = Objects.requireNonNull(hpoTerms);
        this.negatedHpoTerms = Objects.requireNonNull(negatedHpoTerms);
        this.diseaseIds = diseaseIds;

    }

    public static PhenopacketData readPhenopacketData(Path phenopacketPath)  {
        try (InputStream is = new BufferedInputStream(new FileInputStream(String.valueOf(phenopacketPath)))) {
            Phenopacket ppkt = PhenopacketImporter.readPhenopacket(is, Phenopacket.class);
            return PhenopacketData.fromPpkt(ppkt);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }


    private static PhenopacketData fromPpkt(Phenopacket ppkt) {
        String sampleId = ppkt.getId();
        List<String> observedTerms = ppkt.getPhenotypicFeaturesList()
                .stream().filter(Predicate.not(PhenotypicFeature::getExcluded))
                .map(PhenotypicFeature::getType)
                .map(OntologyClass::getId)
                .toList();
        List<String> excludedTerms = ppkt.getPhenotypicFeaturesList()
                .stream().filter(PhenotypicFeature::getExcluded)
                .map(PhenotypicFeature::getType)
                .map(OntologyClass::getId)
                .toList();
        List<TermId> diseaseIds = ppkt.getDiseasesList()
                .stream()
                .filter(Predicate.not(Disease::getExcluded))
                .map(Disease::getTerm)
                .map(OntologyClass::getId)
                .map(TermId::of)
                .toList();
        return new PhenopacketData(sampleId, observedTerms, excludedTerms, diseaseIds);
    }

    public Sample getSample() {
        return Sample.of(
                sampleId(),
                observedHpoTermIds().toList(),
                excludedHpoTermIds().toList());
    }


    public String sampleId() {
        return sampleId;
    }


    public List<String> observedHpoTerms() {
        return hpoTerms;
    }

    public Stream<TermId> observedHpoTermIds() {
        return hpoTerms.stream().map(TermId::of);
    }


    public List<String> excludedHpoTerms() {
        return negatedHpoTerms;
    }

    public Stream<TermId> excludedHpoTermIds() {
        return negatedHpoTerms.stream().map(TermId::of);
    }




    public List<TermId> diseaseIds() {
        return diseaseIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhenopacketData that = (PhenopacketData) o;
        return Objects.equals(sampleId, that.sampleId)
                && Objects.equals(hpoTerms, that.hpoTerms)
                && Objects.equals(negatedHpoTerms, that.negatedHpoTerms)
                && Objects.equals(diseaseIds, that.diseaseIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sampleId, hpoTerms, negatedHpoTerms, diseaseIds);
    }

    @Override
    public String toString() {
        return "PhenopacketData{" +
                ", sampleId='" + sampleId + '\'' +
                ", hpoTerms=" + hpoTerms +
                ", negatedHpoTerms=" + negatedHpoTerms +
                ", diseaseIds=" + diseaseIds +
                '}';
    }
}
