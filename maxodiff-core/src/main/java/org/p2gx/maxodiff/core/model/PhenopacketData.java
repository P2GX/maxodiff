package org.p2gx.maxodiff.core.model;

import org.p2gx.maxodiff.core.analysis.MySimpleTerm;
import org.p2gx.maxodiff.core.analysis.SimpleTerm;
import org.p2gx.maxodiff.core.io.PhenopacketImporter;
import org.p2gx.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.*;
import java.nio.file.Path;

import org.phenopackets.phenopackettools.validator.core.ValidationResults;
import org.phenopackets.phenopackettools.validator.core.ValidationWorkflowRunner;
import org.phenopackets.phenopackettools.validator.jsonschema.JsonSchemaValidationWorkflowRunner;
import org.phenopackets.schema.v2.Phenopacket;
import org.phenopackets.schema.v2.PhenopacketOrBuilder;
import org.phenopackets.schema.v2.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class PhenopacketData {
    private final static Logger LOGGER = LoggerFactory.getLogger(PhenopacketData.class);

    private final String sampleId;
    private final List<String> hpoTerms;
    private final List<String> negatedHpoTerms;
    private List<MySimpleTerm> observedHpoTerms = new ArrayList<>();
    private List<MySimpleTerm> excludedHpoTerms = new ArrayList<>();
    private final List<TermId> diseaseIds;
    /* Medical action ontology terms used in the Phenopacket. We will use this information
     in oder to not suggest a MAxO term that was previosly performed (e.g., do not suggest
     chext X ray twice!).
     */
    private final List<TermId> procedures;


    PhenopacketData(String sampleId,
                    List<String> hpoTerms,
                    List<String> negatedHpoTerms,
                    List<TermId> diseaseIds,
                    List<TermId> maxoIds
                  ) {

        this.sampleId = Objects.requireNonNull(sampleId);
        this.hpoTerms = Objects.requireNonNull(hpoTerms);
        this.negatedHpoTerms = Objects.requireNonNull(negatedHpoTerms);
        this.diseaseIds = diseaseIds;
        this.procedures = maxoIds;
    }
    /// TODO complette refactoring
    /// Masked needed only to distinguish the constructor, it
    // can be deleted later on
    PhenopacketData(String sampleId,
                    List<MySimpleTerm> hpoTerms,
                    List<MySimpleTerm> negatedHpoTerms,
                    List<TermId> diseaseIds,
                    List<TermId> maxoIds,
                    boolean masked
    ) {

        this.sampleId = Objects.requireNonNull(sampleId);
        this.observedHpoTerms = Objects.requireNonNull(hpoTerms);
        this.excludedHpoTerms = Objects.requireNonNull(negatedHpoTerms);
        this.hpoTerms = this.observedHpoTerms.stream()
                .map(MySimpleTerm::tid)
                .map(TermId::getValue)
                .toList();
        this.negatedHpoTerms = this.excludedHpoTerms.stream()
                .map(MySimpleTerm::tid)
                .map(TermId::getValue)
                .toList();
        this.diseaseIds = diseaseIds;
        this.procedures = maxoIds;
    }

    public static PhenopacketData readPhenopacketData(Path phenopacketPath)  {
        try (InputStream is = new BufferedInputStream(new FileInputStream(String.valueOf(phenopacketPath)))) {
            Phenopacket ppkt = PhenopacketImporter.readPhenopacket(is, Phenopacket.class);
            ValidationWorkflowRunner<PhenopacketOrBuilder> runner = JsonSchemaValidationWorkflowRunner.phenopacketBuilder()
                    .build();
            ValidationResults results = runner.validate(ppkt);
            if (!results.isValid()) {
                throw new RuntimeException(results.toString());
            }
            return PhenopacketData.fromPpkt(ppkt);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static PhenopacketData fromPpkt2(Phenopacket ppkt) {
        String sampleId = ppkt.getId();
        List<MySimpleTerm> observedTerms = ppkt.getPhenotypicFeaturesList()
                .stream().filter(Predicate.not(PhenotypicFeature::getExcluded))
                .map(PhenotypicFeature::getType)
                .map(oc -> MySimpleTerm.fromStrings(oc.getId(), oc.getLabel()))
                .toList();
        List<MySimpleTerm> excludedTerms = ppkt.getPhenotypicFeaturesList()
                .stream()
                .filter(PhenotypicFeature::getExcluded)
                .map(PhenotypicFeature::getType)
                .map(oc -> MySimpleTerm.fromStrings(oc.getId(), oc.getLabel()))
                .toList();
        List<TermId> diseaseIds = ppkt.getDiseasesList()
                .stream()
                .filter(Predicate.not(Disease::getExcluded))
                .map(Disease::getTerm)
                .map(OntologyClass::getId)
                .map(TermId::of)
                .toList();
        List<TermId> maxoIds = ppkt.getMedicalActionsList()
                .stream()
                .filter(MedicalAction::hasProcedure)
                .map(MedicalAction::getProcedure)
                .map(Procedure::getCode)
                .map(OntologyClass::getId)
                .map(TermId::of)
                .toList();
        return new PhenopacketData(sampleId, observedTerms, excludedTerms, diseaseIds, maxoIds, true);
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
        List<TermId> maxoIds = ppkt.getMedicalActionsList()
                .stream()
                .filter(MedicalAction::hasProcedure)
                .map(MedicalAction::getProcedure)
                .map(Procedure::getCode)
                .map(OntologyClass::getId)
                .map(TermId::of)
                .toList();
        return new PhenopacketData(sampleId, observedTerms, excludedTerms, diseaseIds, maxoIds);
    }

    public PpktSample getPpktSample(BiometadataService biometadataService) {
        List<SimpleTerm> observedSampleTerms = new ArrayList<>();
        List<SimpleTerm> excludedSampleTerms = new ArrayList<>();
        observedHpoTermIds().forEach(tid ->
                observedSampleTerms.add(new SimpleTerm(tid.getValue(), biometadataService.hpoLabel(tid).orElse("n/a"))));
        excludedHpoTermIds().forEach(tid ->
                excludedSampleTerms.add(new SimpleTerm(tid.getValue(), biometadataService.hpoLabel(tid).orElse("n/a"))));
        return new PpktSample(sampleId(), observedSampleTerms, excludedSampleTerms);
    }

    public List<SimpleTerm> getObservedHpoSimpleTerms() {
        return observedHpoTerms.
                stream()
                .map(mst -> new SimpleTerm(mst.tid().getValue(), mst.label()))
                .toList();
    }

    public List<SimpleTerm> getExcludedHpoSimpleTerms() {
        return excludedHpoTerms
                .stream()
                .map(mst -> new SimpleTerm(mst.tid().getValue(), mst.label()))
                .toList();
    }

    public PpktSample getPpktSample() {
        return new PpktSample(sampleId(), getObservedHpoSimpleTerms(), getExcludedHpoSimpleTerms());
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

    public List<TermId> maxoProcedureIds() {
        return this.procedures;
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
