package org.monarchinitiative.maxodiff.html.results;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.monarchinitiative.maxodiff.core.analysis.HpoFrequency;
import org.monarchinitiative.maxodiff.core.analysis.MaxoTermScore;
import org.monarchinitiative.maxodiff.core.analysis.RankMaxoScore;
import org.monarchinitiative.maxodiff.core.analysis.refinement.MaxodiffResult;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
class MaxoHtmlResultTest {

    @Mock
    private BiometadataService biometadataService;

    private TemplateEngine templateEngine;
    @Mock
    private MaxodiffResult mockMaxoDiffResult;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        setupTemplateEngine();
        setupMockBiometadataService();
        mockMaxoDiffResult = mockMaxoDiffResult();
    }

    private void setupTemplateEngine() {
        templateEngine = new SpringTemplateEngine();
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        templateEngine.setTemplateResolver(resolver);
    }

    private void setupMockBiometadataService() {
        when(biometadataService.maxoLabel("MAXO:0000001"))
                .thenReturn(Optional.of("Neonatal Marfan syndrome treatment"));
        when(biometadataService.hpoLabel(TermId.of("HP:0000592")))
                .thenReturn(Optional.of("Blue sclerae"));
        when(biometadataService.hpoLabel(TermId.of("HP:0001083")))
                .thenReturn(Optional.of("Ectopia lentis"));
        when(biometadataService.diseaseLabel(TermId.of("OMIM:154700")))
                .thenReturn(Optional.of("Marfan syndrome"));
        when(biometadataService.diseaseLabel(TermId.of("OMIM:609192")))
                .thenReturn(Optional.of("Loeys-Dietz syndrome 1"));
        when(biometadataService.maxoLabel("MAXO:0000973"))
                .thenReturn(Optional.of("slit-lamp examination"));
    }

    private List<DifferentialDiagnosis> mockDifferentialDiagnosisList() {
        DifferentialDiagnosis mockDD1 = mockDifferentialDiagnosis("DISEASE:123456", 0.85, 2.5);
        DifferentialDiagnosis mockDD2 = mockDifferentialDiagnosis("DISEASE:65654", 0.25, 1.5);
        return new ArrayList<>(Arrays.asList(mockDD1, mockDD2));
    }

    private DifferentialDiagnosis mockDifferentialDiagnosis(String diseaseId, double score, double lr) {
        return DifferentialDiagnosis.of(TermId.of(diseaseId), score, lr);
    }

    private MaxoTermScore mockMaxoTermScore() {
        TermId termId = TermId.of("MAXO:0000973");
        return new MaxoTermScore("MAXO:0000973",5, Set.of(), Set.of(), 10, Set.of(),
                0.75, 0.85, 0.10, termId, mockDifferentialDiagnosisList(), List.of(),
                new double[]{0.1, 0.2, 0.3, 0.4, 0.5},new double[]{0.15, 0.25, 0.35, 0.45, 0.55});
    }

    private RankMaxoScore mockRankMaxoScore() {
        RankMaxoScore mockRankScore = mock(RankMaxoScore.class);

        // Mock TermIds
        TermId mockMaxoId = TermId.of("MAXO:0000973");

        TermId mockOmimTerm = TermId.of("OMIM:123456");

        TermId mockHpoTerm = TermId.of("HP:0001234");

        TermId mockDiseaseTerm = TermId.of("DISEASE:567890");

        /*RankMaxoScore(TermId maxoId, Set<TermId> initialOmimTermIds, Set<TermId> maxoOmimTermIds,
                            Set<TermId> discoverableObservedHpoTermIds,
                            Set<TermId> discoverableObservedDescendantHpoTermIds,
                            Double maxoScore,
                            List<DifferentialDiagnosis> maxoDiagnoses,
                            Map<TermId, Map<TermId, Integer>> hpoTermIdRepCtsMap,
                            Map<TermId, Integer> maxoDiseaseAvgRankChangeMap,
                            int minRankChange,
                            int maxRankChange*/
        double maxoScore = 0.85d;
        int minRankChange = -10;
        int maxRankChange = 15;
        Map<TermId, Map<TermId, Integer>> hpoTermIdRepCtsMap = Map.of(mockHpoTerm, Map.of(mockDiseaseTerm, 3));
        Map<TermId, Integer> maxoDiseaseAvgRankChangeMap = Map.of(mockDiseaseTerm, 5);
        return new RankMaxoScore(mockMaxoId, Set.of(mockOmimTerm), Set.of(mockOmimTerm),
                Set.of(mockHpoTerm), Set.of(mockHpoTerm), maxoScore,
                mockDifferentialDiagnosisList(),hpoTermIdRepCtsMap,
                maxoDiseaseAvgRankChangeMap,
                minRankChange, maxRankChange
                );
    }


    private MaxodiffResult mockMaxoDiffResult() {
        MaxodiffResult mockMaxoDiffResult = mock(MaxodiffResult.class);
        when(mockMaxoDiffResult.maxoTermScore()).thenReturn(mockMaxoTermScore());
        when(mockMaxoDiffResult.rankMaxoScore()).thenReturn(mockRankMaxoScore());
        when(mockMaxoDiffResult.frequencies()).thenReturn(List.of());
        when(mockMaxoDiffResult.maxoFrequencies()).thenReturn(List.of());
        return mockMaxoDiffResult;
    }


/*
    private MaxoHtmlResult createTestMaxoHtmlResult() {
        // Create mock MaxodiffResult with realistic test data
        MaxodiffResult mockResult = createMockMaxodiffResult();
        List<HpoFrequency> mockHpoFrequencies = createMockHpoFrequencies();

        return new MaxoHtmlResult(mockResult, mockHpoFrequencies, 1, biometadataService);
    }
*/
    private void createMockMaxodiffResult() {
        // Create realistic test data based on your document
        Set<TermId> hpoIds = Set.of(
                TermId.of("HP:0000592"), // Blue sclerae
                TermId.of("HP:0001083")  // Ectopia lentis
        );

        Set<TermId> omimIds = Set.of(
                TermId.of("OMIM:154700"), // Marfan syndrome
                TermId.of("OMIM:609192"), // Loeys-Dietz syndrome 1
                TermId.of("OMIM:610168")  // Neonatal Marfan syndrome
        );

        Map<TermId, Integer> diseaseRankChanges = Map.of(
                TermId.of("OMIM:610168"), -11, // Neonatal Marfan syndrome (improvement)
                TermId.of("OMIM:154700"), -8,   // Marfan syndrome (improvement)
                TermId.of("OMIM:609192"), 0     // Loeys-Dietz syndrome 1 (no change)
        );

        // Create mock HPO association data
        Map<TermId, Map<TermId, Integer>> hpoAssociations = new HashMap<>();
        hpoAssociations.put(TermId.of("OMIM:610168"), Map.of(
                TermId.of("HP:0000592"), 1, // Neonatal Marfan has blue sclerae
                TermId.of("HP:0001083"), 1  // and ectopia lentis
        ));
        hpoAssociations.put(TermId.of("OMIM:154700"), Map.of(
                TermId.of("HP:0000592"), 0, // Marfan syndrome no blue sclerae
                TermId.of("HP:0001083"), 1  // but has ectopia lentis
        ));
        hpoAssociations.put(TermId.of("OMIM:609192"), Map.of(
                TermId.of("HP:0000592"), 1, // Loeys-Dietz has blue sclerae
                TermId.of("HP:0001083"), 1  // and ectopia lentis
        ));

    }

    private List<HpoFrequency> createMockHpoFrequencies() {
        return List.of(
                new HpoFrequency("HP:0000592", "Blue sclerae", 6, 1.0f),
                new HpoFrequency("HP:0000592", "Blue sclerae", 7, 0.1f),
                new HpoFrequency("HP:0001083", "Ectopia lentis", 8, 0.3f),
                new HpoFrequency("HP:0001083", "Ectopia lentis", 1, 0.8f)
        );
    }


    @Test
    public void testTemplate() {
        // Set up Thymeleaf template engine
        FileTemplateResolver templateResolver = new FileTemplateResolver();
        templateResolver.setPrefix("src/main/resources/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCacheable(false);

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        int mockIndex = 42;
       MaxoHtmlResult maxoData = new MaxoHtmlResult(
                mockMaxoDiffResult,
                List.of(),
                mockIndex,
                biometadataService
        );

        // Create Thymeleaf context and add your data
        Context context = new Context();
        context.setVariable("maxoData", maxoData);

        // Process the template
        String result = templateEngine.process("maxoResultBox", context);
        // Write the result to an HTML file for browser viewing
        try {
            File outputFile = new File("target/test-output.html");
            outputFile.getParentFile().mkdirs(); // Create directories if they don't exist

            try (FileWriter writer = new FileWriter(outputFile)) {
                writer.write(result);
            }

            System.out.println("HTML output written to: " + outputFile.getAbsolutePath());
            System.out.println("Open this file in your browser to view the results!");
        } catch (IOException e) {
            System.err.println("Failed to write HTML file: " + e.getMessage());
        }
        // Assert the results
        System.out.println("Rendered template:");
        System.out.println(result);


    }


}

