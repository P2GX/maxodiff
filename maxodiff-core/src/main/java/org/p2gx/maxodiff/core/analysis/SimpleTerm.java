package org.p2gx.maxodiff.core.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.p2gx.maxodiff.core.io.PrefixZeroCleaner;

public record SimpleTerm(@JsonDeserialize(using = PrefixZeroCleaner.class) String termId,
                         @JsonIgnore String termLabel) {
}
