package org.p2gx.maxodiff.core.analysis;

import java.util.List;

public record MdMetadata(
        String ppktId,
        int nDiseases,
        int nRepetitions,
        List<MySimpleTerm> observedHpoTerms,
        List<MySimpleTerm> excludedHpoTerms,
        List<RankedMaxoResult> resultList

) {
}
