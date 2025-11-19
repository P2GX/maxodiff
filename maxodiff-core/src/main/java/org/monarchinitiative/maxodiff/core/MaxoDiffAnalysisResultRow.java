package org.monarchinitiative.maxodiff.core;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

public record MaxoDiffAnalysisResultRow(
        String phenopacket_id,
        String disease_id,
        String maxo_id,
        String maxo_label,
        int n_diseases,
        java.util.Set<org.monarchinitiative.phenol.ontology.data.TermId> disease_ids,
        int n_repitions,
        double score
) {

    public static List<String> headerFields() {
        return List.of("phenopacket", "disease_id", "maxo_id", "maxo_label",
                "n_diseases", "disease_ids", "n_repetitions", "score");
    }

    public List<String> getFields() {
        return List.of(phenopacket_id(),
                disease_id(),
                maxo_id(),
                maxo_label(),
                String.valueOf(n_diseases()),
                String.join(";", disease_ids().stream().map(TermId::getValue).toList()),
                String.valueOf(n_repitions),
                String.valueOf(score)
        );
    }
}



