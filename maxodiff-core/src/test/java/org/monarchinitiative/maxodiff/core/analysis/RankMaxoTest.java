package org.monarchinitiative.maxodiff.core.analysis;

import org.junit.jupiter.api.BeforeAll;
import org.monarchinitiative.maxodiff.core.SimpleTermOld;
import org.monarchinitiative.maxodiff.core.TestResources;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.*;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;


public class RankMaxoTest {

    private final static HpoDiseases hpoDiseases = TestResources.hpoDiseases();
    private final static List<DifferentialDiagnosis> initialDiagnoses = TestResources.getExampleDiagnoses().stream().toList();
    private final static Map<SimpleTermOld, Set<SimpleTermOld>> hpoToMaxoTermMap = TestResources.hpoToMaxo();

    private final static Map<TermId, Set<TermId>> maxoToHpoTermIdMap = MaxoHpoTermIdMaps.getMaxoToHpoTermIdMap(hpoToMaxoTermMap);
    private final static MaxoHpoTermProbabilities maxoHpoTermProbabilities =
            new MaxoHpoTermProbabilities(hpoDiseases,
                                         hpoToMaxoTermMap,
                                         initialDiagnoses,
                                         DiseaseModelProbability.ranked(initialDiagnoses));

    private static DifferentialDiagnosisEngine ENGINE;
    private static final Ontology ontology = TestResources.hpo();

    @BeforeAll
    public static void setUpBeforeClass() {
        ENGINE = new DifferentialDiagnosisEngine() {
            @Override
            public List<DifferentialDiagnosis> run(PpktSample sample) {
                return initialDiagnoses;
            }

            @Override
            public List<DifferentialDiagnosis> run(PpktSample sample, Collection<TermId> targetDiseases) {
                return initialDiagnoses;
            }
        };
    }


}
