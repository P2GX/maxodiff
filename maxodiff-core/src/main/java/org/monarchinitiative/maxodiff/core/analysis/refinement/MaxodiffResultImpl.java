package org.monarchinitiative.maxodiff.core.analysis.refinement;

import org.monarchinitiative.maxodiff.core.analysis.Frequencies;
import org.monarchinitiative.maxodiff.core.analysis.MaxoTermScore;

import java.util.List;

record MaxodiffResultImpl(MaxoTermScore maxoTermScore,
                          List<Frequencies> frequencies,
                          List<Frequencies> maxoFrequencies) implements MaxodiffResult {

}
