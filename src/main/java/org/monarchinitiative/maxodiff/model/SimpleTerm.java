package org.monarchinitiative.maxodiff.model;

import org.monarchinitiative.phenol.ontology.data.TermId;

public record SimpleTerm(TermId tid, String label) {

    public  String display () {
        return String.format("%s [%s]", label(), tid().getValue());
    }
}
