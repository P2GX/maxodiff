package org.p2gx.maxodiff.core.model;

import org.monarchinitiative.phenol.ontology.data.TermId;

public record TermPair(TermId tidA, TermId tidB) {

  public static TermPair symmetric(TermId a, TermId b) {
    return a.getId().compareTo(b.getId()) > 0 ? new TermPair(a, b) : new TermPair(b, a);
  }

  public static TermPair asymmetric(TermId a, TermId b) {
    return new TermPair(a, b);
  }
}