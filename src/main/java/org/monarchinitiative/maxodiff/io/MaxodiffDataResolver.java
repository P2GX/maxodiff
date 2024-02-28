package org.monarchinitiative.maxodiff.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class MaxodiffDataResolver {


    private static final Logger LOGGER = LoggerFactory.getLogger(MaxodiffDataResolver.class);

    private final Path dataDirectory;

    public static MaxodiffDataResolver of(Path dataDirectory) throws MaxodiffDataException {
        return new MaxodiffDataResolver(dataDirectory);
    }

    public MaxodiffDataResolver(Path dataDirectory) throws MaxodiffDataException {
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "Data directory must not be null!");
        checkV1Resources();
    }

    private void checkV1Resources() throws MaxodiffDataException {
        boolean error = false;
        List<Path> requiredFiles = List.of(hpoJson(), hgncCompleteSet(), mim2geneMedgen(), phenotypeAnnotations());
        for (Path file : requiredFiles) {
            if (!Files.isRegularFile(file)) {
                LOGGER.error("Missing required file `{}` in `{}`.", file.toFile().getName(), dataDirectory.toAbsolutePath());
                error = true;
            }
        }
        if (error) {
            throw new MaxodiffDataException("Missing one or more resource files in Lirical data directory!");
        }
    }

    public Path dataDirectory() {
        return dataDirectory;
    }

    public Path hpoJson() {
        return dataDirectory.resolve("hp.json");
    }

    public Path maxoJson() {
        return dataDirectory.resolve("maxo.json");
    }

    public Path maxoDxAnnots() { return dataDirectory.resolve("maxo_diagnostic_annotations.tsv"); }

    public Path hgncCompleteSet() {
        return dataDirectory.resolve("hgnc_complete_set.txt");
    }

    public Path mim2geneMedgen() {
        return dataDirectory.resolve("mim2gene_medgen");
    }

    public Path phenotypeAnnotations() {
        return dataDirectory.resolve("phenotype.hpoa");
    }


}
