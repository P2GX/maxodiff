package org.monarchinitiative.maxodiff.analysis;

import org.checkerframework.checker.units.qual.A;
import org.monarchinitiative.maxodiff.model.SimpleTerm;
import org.monarchinitiative.maxodiff.service.MaxoDiffService;
import org.monarchinitiative.maxodiff.service.MaxoDiffServiceImpl;
import org.monarchinitiative.maxodiff.service.PhenotypeService;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

public class MaxodiffAnalyzer {

    /** Disease objects representing the disease ids passed to the constructor. */
    private final Map<TermId, HpoDisease> diseaseMap;

    private final Ontology hpo;

    private final Map<SimpleTerm, Set<SimpleTerm>> maxoDxAnnots;

    private final Map<TermId, Set<SimpleTerm>> diseaseToMaxoMap;
    private final Map<TermId, Set<SimpleTerm>> diseaseToHpoMap;
    /**
     * For the set of diseases being investigated, a given Maxo term may correspond to multiple
     * HPO terms. e.g. Brain CT could correspond to multiple CNS anomalies. Here, the key is the
     * MAXO term and the value is the set of HPO terms that correspond to it in the current dataset.
     */
    private final Map<SimpleTerm, Set<SimpleTerm>> maxoToHpoMap;

    private final List<SimpleTerm> allMaxoAnnots;
    private final List<SimpleTerm> allHpoAnnots;

    public MaxodiffAnalyzer(PhenotypeService phenotypeService, List<TermId> diseaseTermIds) {
        diseaseMap = new HashMap<>();
        HpoDiseases diseases = phenotypeService.diseases();
        hpo = phenotypeService.hpo();
        for (var diseaseId : diseaseTermIds) {
            Optional<HpoDisease> opt = diseases.diseaseById(diseaseId);
            if (opt.isPresent()) {
                diseaseMap.put(diseaseId, opt.get());
            } else {
                System.err.printf("[ERROR] Could not find \"%s\".\n", diseaseId.getValue());
            }
        }
        maxoDxAnnots = phenotypeService.maxoDxAnnots();
        diseaseToMaxoMap = new HashMap<>();
        diseaseToHpoMap = new HashMap<>();
        maxoToHpoMap = new HashMap<>();
        this.allHpoAnnots = new ArrayList<>();
        this.allMaxoAnnots = new ArrayList<>();
        init();
    }





    private void init() {
        Set<SimpleTerm> allMxoAnnotSet = new HashSet<>();
        Set<SimpleTerm> allHpoAnnotSet = new HashSet<>();

        // first collect common HPO terms
        for (var entry : this.diseaseMap.entrySet()) {
            TermId diseaseId = entry.getKey();
            HpoDisease disease = entry.getValue();
            for (var hpoId : disease.annotationTermIdList()) {
                this.diseaseToHpoMap.putIfAbsent(diseaseId, new HashSet<>());
                Optional<Term> opt = hpo.termForTermId(hpoId);
                if (opt.isPresent()) {
                    SimpleTerm hpost = new SimpleTerm(hpoId, opt.get().getName());
                    this.diseaseToHpoMap.get(diseaseId).add(hpost);
                    allHpoAnnotSet.add(hpost);
                } else {
                    System.err.printf("[ERROR] Could not find HPO term for %s.\n", hpoId.getValue());
                }
            }
        }
        // Now map these terms to MAXO ids
        // also record which HPOs correspond to the MAXO terms used in our dataset
        for (var entry : this.diseaseToHpoMap.entrySet()) {
            TermId diseaseId = entry.getKey();
            Set<SimpleTerm> hpoSet = entry.getValue();
            this.diseaseToMaxoMap.putIfAbsent(diseaseId, new HashSet<>());
            for (var hpost: hpoSet) {
                Set<SimpleTerm> maxoSet = this.maxoDxAnnots.getOrDefault(hpost, Set.of());
                for (var stmaxo : maxoSet) {
                    this.diseaseToMaxoMap.get(diseaseId).add(stmaxo);
                    allMxoAnnotSet.add(stmaxo);
                    maxoToHpoMap.putIfAbsent(stmaxo, new HashSet<>());
                    maxoToHpoMap.get(stmaxo).add(hpost);
                }
            }
        }


        this.allHpoAnnots.addAll(allHpoAnnotSet);
        this.allMaxoAnnots.addAll(allMxoAnnotSet);

        dumpStats();

    }

    private void dumpStats() {
        for (var entry : this.diseaseToHpoMap.entrySet()) {
            TermId diseaseId = entry.getKey();
            int n = entry.getValue().size();
            System.out.printf("%s: %d HPO annotations.\n", diseaseId.getValue(), n);
        }
        for (var entry : this.diseaseToMaxoMap.entrySet()) {
            TermId diseaseId = entry.getKey();
            int n = entry.getValue().size();
            System.out.printf("%s: %d MAxO annotations.\n", diseaseId.getValue(), n);
        }
        System.out.printf("Number of unique HPO terms used: %d\n", allHpoAnnots.size());
        System.out.printf("Number of unique MAxO terms used: %d\n", allMaxoAnnots.size());
    }

    public MaxoDiffService maxoDiffService() {
        return new MaxoDiffServiceImpl(diseaseMap, diseaseToMaxoMap, diseaseToHpoMap, allMaxoAnnots,
                allHpoAnnots, this.maxoDxAnnots, this.maxoToHpoMap);
    }


}
