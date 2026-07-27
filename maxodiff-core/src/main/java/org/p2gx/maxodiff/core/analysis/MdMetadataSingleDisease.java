package org.p2gx.maxodiff.core.analysis;

import java.util.List;

public record MdMetadataSingleDisease(
        String ppktId,
        int nDiseases,
        int nRepetitions,
        List<SimpleTerm> observedHpoTerms,
        List<SimpleTerm> excludedHpoTerms,
        List<RankedMaxoResultSingleDisease> resultList

) {
}
