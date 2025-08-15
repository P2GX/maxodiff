package org.monarchinitiative.maxodiff.lirical;

import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.lirical.io.analysis.PhenopacketImporter;
import org.monarchinitiative.lirical.io.analysis.PhenopacketImporters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class PhenopacketFileParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhenopacketFileParser.class);

    private PhenopacketFileParser() {}

    public static PhenopacketData readPhenopacketData(Path phenopacketPath) throws Exception {
        PhenopacketData data = null;
        try (InputStream is = new BufferedInputStream(Files.newInputStream(phenopacketPath))) {
            PhenopacketImporter v2 = PhenopacketImporters.v2();
            data = v2.read(is);
            LOGGER.debug("Success!");
        } catch (Exception e) {
            LOGGER.debug("Unable to parse as v2 phenopacket, trying v1.");
        }

        if (data == null) {
            try (InputStream is = new BufferedInputStream(Files.newInputStream(phenopacketPath))) {
                PhenopacketImporter v1 = PhenopacketImporters.v1();
                data = v1.read(is);
                LOGGER.debug("Success!");
            } catch (IOException e) {
                LOGGER.debug("Unable to parser as v1 phenopacket.");
                throw new Exception("Unable to parse phenopacket from " + phenopacketPath.toAbsolutePath());
            }
        }

        // Check we have exactly one disease ID.
        if (data.diseaseIds().isEmpty())
            throw new Exception("Missing disease ID which is required for the benchmark!");
        else if (data.diseaseIds().size() > 1)
            throw new Exception("Saw >1 disease IDs {}, but we need exactly one for the benchmark!");
        return data;
    }
}
