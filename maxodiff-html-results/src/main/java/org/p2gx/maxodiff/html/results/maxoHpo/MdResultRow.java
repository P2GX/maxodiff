package org.p2gx.maxodiff.html.results.maxoHpo;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.analysis.*;

import java.util.*;

public class MdResultRow {
    private final String omimId;
    private final String omimLabel;
    private final int initialRank;
    private final float averageRank;
    private final int nDiseases;
    private final List<HpoTableCell> cells;

    public MdResultRow(String omimId, String omimLabel, int initialRank, float averageRank, int nDiseases, List<HpoTableCell> cells) {
        this.omimId = omimId;
        this.omimLabel = omimLabel;
        this.initialRank = initialRank;
        this.averageRank = averageRank;
        this.nDiseases = nDiseases;
        this.cells = cells;
    }


    public static List<MdResultRow> createMaxoResultRows(RankedMaxoResult result,
                                                         int nDiseases,
                                                         HTMLFrequencyMap htmlFrequencyMap,
                                                         double maxMica) {
        List<MdResultRow> rows = new ArrayList<>();

        for (RankedOmimTerm diseaseTerm : result.rankedOmimTermList()) {
            TermId diseaseId = diseaseTerm.omimTerm().tid();
            String diseaseLabel = diseaseTerm.omimTerm().label();
            int initialRank = diseaseTerm.initialRank();
            float averageRank = diseaseTerm.averageRank();
            List<HpoTableCell> cells = new ArrayList<>();
            for (CountedHpoTerm hpoTerm : result.hpoTermIds()) {
                TermId hpoId = hpoTerm.hpoTerm().tid();
                int ct = hpoTerm.count();
                Optional<HpoFrequency> hpoFrequencyOpt = result.frequencies().stream()
                    .filter(hpoFrequency ->
                        (hpoFrequency.diseaseId().equals(diseaseId) && hpoFrequency.hpoId().equals(hpoId)))
                    .findFirst();
                float mica = hpoFrequencyOpt.map(HpoFrequency::mica).orElseGet(() -> htmlFrequencyMap.micaForDisease(hpoId, diseaseId));
                double opacity = mica / maxMica;
                cells.add(new HpoTableCell(ct, opacity, mica, maxMica));
            }
            rows.add(new MdResultRow(diseaseId.getValue(), diseaseLabel, initialRank, averageRank, nDiseases, cells));

        }
        return rows;
    }

    public String getOmimId() {
        return omimId;
    }

    public String getOmimIdDigits() {
        String[] fields =  omimId.split(":");
        return fields.length == 2 ? fields[1] : omimId;
    }

    public String getOmimLabel() {
        return omimLabel;
    }

    public int getInitialRank() {
        return initialRank;
    }

    public float getAverageRank() {
        return averageRank;
    }

    public int getnDiseases() {
        return nDiseases;
    }

    public List<HpoTableCell> getCells() {
        return cells;
    }
}
