package org.monarchinitiative.maxodiff.html.results.maxoHpo;

import org.monarchinitiative.maxodiff.core.analysis.*;
import org.monarchinitiative.maxodiff.core.analysis.refinement.MaxodiffResult;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

public class MdResultRow {
    private final String omimId;
    private final String omimLabel;
    private final int initialRank;
    private final int rankChange;
    private final int nDiseases;
    private final List<HpoTableCell> cells;

    public MdResultRow(String omimId, String omimLabel, int initialRank, int rankChange, int nDiseases, List<HpoTableCell> cells) {
        this.omimId = omimId;
        this.omimLabel = omimLabel;
        this.initialRank = initialRank;
        this.rankChange = rankChange;
        this.nDiseases = nDiseases;
        this.cells = cells;
    }


    public static List<MdResultRow> createMaxoResultRows(RankedMaxoResult result,
                                                         int nDiseases,
                                                         List<HpoFrequency> hpoFrequenciesMica,
                                                         HTMLFrequencyMap htmlFrequencyMap) {
        List<MdResultRow> rows = new ArrayList<>();

        for (RankedOmimTerm omimTerm : result.rankedOmimTermList()) {
            String omimId = omimTerm.omimTerm().termId();
            String omimLabel = omimTerm.omimTerm().termLabel();
            int initialRank = omimTerm.initialRank();
            int averageRank = omimTerm.averageRank();
            List<HpoTableCell> cells = new ArrayList<>();
            for (CountedHpoTerm hpoTerm : result.hpoTermIds()) {
                String hpoId = hpoTerm.hpoTerm().termId();
                int ct = hpoTerm.count();
                float mica = 0f;
                Optional<HpoFrequency> hpoFrequencyOpt = hpoFrequenciesMica.stream()
                    .filter(hpoFrequency ->
                        (hpoFrequency.omimId().equals(omimId) && hpoFrequency.hpoId().equals(hpoId)))
                    .findFirst();
                if (hpoFrequencyOpt.isPresent()) {
                    mica = hpoFrequencyOpt.get().mica();
                } else {
                    ct = 0;
                    mica = htmlFrequencyMap.micaForDisease(TermId.of(hpoId), TermId.of(omimId));
                }
                float maxMica = 8.343077871169383f;
                double opacity = mica / maxMica;
                cells.add(new HpoTableCell(ct, opacity, mica));
            }
            rows.add(new MdResultRow(omimId, omimLabel, initialRank, averageRank, nDiseases, cells));

        }
        return rows;
    }

    public String getStyle() {
        double opacity = (double) rankChange / nDiseases;
        return (rankChange < 0) ? "rgba(0, 128, 0, " + (-1.0 * opacity) + ")" :
                "rgba(255, 0, 0, " + opacity + ")";
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

    public int getRankChange() {
        return rankChange;
    }

    public int getnDiseases() {
        return nDiseases;
    }

    public List<HpoTableCell> getCells() {
        return cells;
    }
}
