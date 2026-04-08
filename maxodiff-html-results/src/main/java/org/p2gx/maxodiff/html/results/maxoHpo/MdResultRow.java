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
                                                         List<HpoFrequency> hpoFrequenciesMica,
                                                         HTMLFrequencyMap htmlFrequencyMap,
                                                         double maxMica) {
        List<MdResultRow> rows = new ArrayList<>();

        for (RankedOmimTerm omimTerm : result.rankedOmimTermList()) {
            String omimId = omimTerm.omimTerm().termId();
            String omimLabel = omimTerm.omimTerm().termLabel();
            int initialRank = omimTerm.initialRank();
            float averageRank = omimTerm.averageRank();
            List<HpoTableCell> cells = new ArrayList<>();
            for (CountedHpoTerm hpoTerm : result.hpoTermIds()) {
                String hpoId = hpoTerm.hpoTerm().termId();
                int ct = hpoTerm.count();
                Optional<HpoFrequency> hpoFrequencyOpt = hpoFrequenciesMica.stream()
                    .filter(hpoFrequency ->
                        (hpoFrequency.diseaseId().getValue().equals(omimId) && hpoFrequency.hpoId().getValue().equals(hpoId)))
                    .findFirst();
                float mica = hpoFrequencyOpt.map(HpoFrequency::mica).orElseGet(() -> htmlFrequencyMap.micaForDisease(TermId.of(hpoId), TermId.of(omimId)));
                double opacity = mica / maxMica;
                cells.add(new HpoTableCell(ct, opacity, mica, maxMica));
            }
            rows.add(new MdResultRow(omimId, omimLabel, initialRank, averageRank, nDiseases, cells));

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
