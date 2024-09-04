package org.monarchinitiative.maxodiff.core.analysis;

import java.util.List;

record MaxodiffResultImpl(MaxoTermScore maxoTermScore,
                          List<Frequencies> frequencies,
                          List<Frequencies> maxoFrequencies) implements MaxodiffResult {

}
