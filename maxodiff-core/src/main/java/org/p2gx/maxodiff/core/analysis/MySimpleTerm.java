package org.p2gx.maxodiff.core.analysis;

import org.monarchinitiative.phenol.ontology.data.TermId;

public record MySimpleTerm(TermId tid, String label) {




    public static MySimpleTerm fromStrings(String id, String label) {
        TermId tid = TermId.of(id);
        return new MySimpleTerm(tid, label);
    }
}
