package org.monarchinitiative.maxodiff.core.analysis.refinement;

import org.monarchinitiative.maxodiff.core.analysis.Frequencies;
import org.monarchinitiative.maxodiff.core.analysis.RankMaxoScore;

import java.util.List;

record MaxodiffResultImpl(RankMaxoScore rankMaxoScore,
                          List<Frequencies> frequencies) implements MaxodiffResult {

}
