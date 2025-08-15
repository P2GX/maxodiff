package org.monarchinitiative.maxodiff.phenomizer;

import java.time.LocalDate;

/**
 * Metadata of the IC MICA dictionary.
 *
 * @param hpoVersion  the HPO version used to compute the IC values (e.g. <code>2025-03-03</code>)
 * @param hpoaVersion the version of HPO annotation database used to compute the IC values (e.g. <code>2025-03-03</code>)
 * @param created     a date of creation of the IC MICA dictionary
 */
public record IcMicaDictMetadata(String hpoVersion, String hpoaVersion, LocalDate created) {
}
