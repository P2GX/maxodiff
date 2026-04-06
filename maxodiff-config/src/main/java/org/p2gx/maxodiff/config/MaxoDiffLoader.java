package org.p2gx.maxodiff.config;

import org.p2gx.maxodiff.config.impl.MaxoDiffFileLoader;
import org.p2gx.maxodiff.core.phenomizer.IcMicaData;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;

import java.nio.file.Path;

public interface MaxoDiffLoader {

    MinimalOntology hpo();
    HpoDiseases hpoDiseases() throws MaxodiffDataException;
    IcMicaData icMicaData() throws MaxodiffDataException;
    static MaxoDiffLoader fileLoader(Path maxoPAth) throws MaxodiffDataException {
        return new MaxoDiffFileLoader(maxoPAth);
    }
}
