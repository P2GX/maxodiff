package org.monarchinitiative.maxodiff.core.analysis;

import java.util.List;

public interface MaxodiffResult {

    MaxoTermScore maxoTermScore();
    List<Frequencies> frequencies();
}
