package org.monarchinitiative.maxodiff.config;

import org.monarchinitiative.maxodiff.config.impl.MaxoDiffFileLoader;
import org.monarchinitiative.maxodiff.phenomizer.IcMicaData;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;

import java.nio.file.Path;

public interface MaxoDiffLoader {

    MinimalOntology hpo();
    HpoDiseases hpoDiseases() throws MaxodiffDataException;
    IcMicaData icMicaData() throws MaxodiffDataException;
    public static MaxoDiffLoader fileLoader(Path maxoPAth) throws MaxodiffDataException {
        return new MaxoDiffFileLoader(maxoPAth);
    }
}
