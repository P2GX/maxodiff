package org.monarchinitiative.maxodiff.core.analysis;

import java.util.List;

record MaxodiffResultImpl(MaxoTermScore maxoTermScore, List<Frequencies> frequencies) implements MaxodiffResult {

}
