package org.p2gx.maxodiff.core.analysis;

import org.junit.jupiter.api.BeforeAll;
import org.p2gx.maxodiff.core.TestResources;
import org.p2gx.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.model.DifferentialDiagnosis;
import org.p2gx.maxodiff.core.model.DiseaseModelProbability;
import org.p2gx.maxodiff.core.model.MaxoHpoTermProbabilities;
import org.p2gx.maxodiff.core.model.PpktSample;

import java.util.*;


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
