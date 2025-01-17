package org.monarchinitiative.maxodiff.core.analysis;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.lirical.configuration.LiricalBuilder;
import org.monarchinitiative.lirical.configuration.impl.BundledBackgroundVariantFrequencyServiceFactory;
import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.AnalysisOptions;
import org.monarchinitiative.lirical.core.analysis.LiricalAnalysisRunner;
import org.monarchinitiative.lirical.core.exception.LiricalException;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.monarchinitiative.lirical.core.service.BackgroundVariantFrequencyServiceFactory;
import org.monarchinitiative.lirical.core.service.PhenotypeService;
import org.monarchinitiative.lirical.io.background.CustomBackgroundVariantFrequencyServiceFactory;
import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.TestResources;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.lirical.*;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;


public class RankMaxoTest {

    private final static HpoDiseases hpoDiseases = TestResources.hpoDiseases();
    private final static List<DifferentialDiagnosis> initialDiagnoses = TestResources.getExampleDiagnoses().stream().toList();
    private final static Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap = TestResources.hpoToMaxo();

    private final static Map<TermId, Set<TermId>> maxoToHpoTermIdMap = MaxoHpoTermIdMaps.getMaxoToHpoTermIdMap(hpoToMaxoTermMap);
    private final static MaxoHpoTermProbabilities maxoHpoTermProbabilities =
            new MaxoHpoTermProbabilities(hpoDiseases,
                                         hpoToMaxoTermMap,
                                         initialDiagnoses,
                                         DiseaseModelProbability.ranked(initialDiagnoses));


    public static LiricalConfiguration configureLirical() throws LiricalException {
        String homeDir = System.getProperty("user.home");
        Path liricalDataDir = Path.of(String.join(File.separator, homeDir, ".maxodiff/data/lirical"));
        Path exomiserDb = null; //Path.of("/Users/beckwm/Exomiser/2109_hg38/2109_hg38");
        String genomeBuild = "hg38";
        TranscriptDatabase transcriptDb = TranscriptDatabase.valueOf("REFSEQ");
        float pathogenicity = 0.8f;
        double defaultVarBkgFreq = 0.1;
        boolean strict = false;
        boolean globalAnalysisMode = false;

        return LiricalConfiguration.of(
                liricalDataDir,
                exomiserDb,
                genomeBuild,
                transcriptDb,
                pathogenicity,
                defaultVarBkgFreq,
                strict,
                globalAnalysisMode
        );
    }

    public static LiricalDifferentialDiagnosisEngine getLiricalEngine(LiricalConfiguration liricalConfiguration,
                                                                      Set<TermId> diseaseIds) throws LiricalException {

        Lirical lirical = liricalConfiguration.lirical();
        PhenotypeService phenotypeService = lirical.phenotypeService();
        BundledBackgroundVariantFrequencyServiceFactory bundledBackgroundVariantFrequencyServiceFactory =
                BundledBackgroundVariantFrequencyServiceFactory.getInstance();

        MaxodiffLiricalAnalysisRunner maxodiffLiricalAnalysisRunner =
                MaxodiffLiricalAnalysisRunnerImpl.of(phenotypeService, bundledBackgroundVariantFrequencyServiceFactory, 1);

        LiricalDifferentialDiagnosisEngineConfigurer liricalDifferentialDiagnosisEngineConfigurer =
                LiricalDifferentialDiagnosisEngineConfigurer.of(maxodiffLiricalAnalysisRunner);

        AnalysisOptions options = liricalConfiguration.prepareAnalysisOptions(diseaseIds);

        final LiricalDifferentialDiagnosisEngine engine = liricalDifferentialDiagnosisEngineConfigurer.configure(options, diseaseIds);

        return engine;
    }


    /**
     *
     * @return Sample phenopacket with one included HPO term Id and one disease Id.
     */
    public static Sample getPPkt1() {
        List<TermId> presentTerms = List.of(
                TermId.of("HP:0008619")
        );
        List<TermId> excludedTerms = List.of();
        List<TermId> diseaseIds = List.of(TermId.of("OMIM:615837"));

        return Sample.of("sample1", presentTerms, excludedTerms);//, diseaseIds);
    }

    /**
     *
     * @return Sample phenopacket with two included HPO term Ids and one disease Id.
     */
    public static Sample getPPkt2() {
        List<TermId> presentTerms = List.of(
                TermId.of("HP:0008619"),
                TermId.of("HP:0001751")
        );
        List<TermId> excludedTerms = List.of();
        List<TermId> diseaseIds = List.of(TermId.of("OMIM:615837"));

        return Sample.of("sample1", presentTerms, excludedTerms);//, diseaseIds);
    }

    /**
     * We expect this to cause an error, because OMIM:123456 is not aa actual identifier
     * @return Sample phenopacket with one included HPO term Id and one dummy disease Id.
     */
    public static Sample getPPktEmptyDisease() {
        List<TermId> presentTerms = List.of(
                TermId.of("HP:0008619")
        );
        List<TermId> excludedTerms = List.of();
        List<TermId> diseaseIds = List.of(TermId.of("OMIM:123456"));

        return Sample.of("sample2", presentTerms, excludedTerms);//, diseaseIds);
    }

    /**
     * This tests ranking MAxO terms
     */
    @Test
    public void testRankMaxoTerms() throws LiricalException {
        Set<TermId> diseaseIds = initialDiagnoses.stream()
                .map(DifferentialDiagnosis::diseaseId).collect(Collectors.toSet());
        Sample s1 = TestResources.getExampleSample();
        LiricalConfiguration liricalConfiguration = configureLirical();
        LiricalDifferentialDiagnosisEngine engine = getLiricalEngine(liricalConfiguration, diseaseIds);
        RankMaxo rankMaxo = new RankMaxo(maxoToHpoTermIdMap, maxoHpoTermProbabilities, engine);
        Map<TermId, Double> maxoTermRanks = rankMaxo.rankMaxoTerms(s1, 2);
        System.out.println(maxoTermRanks);
    }

}
