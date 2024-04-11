package org.monarchinitiative.maxodiff.core.io;

import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.service.PhenotypeService;
import org.monarchinitiative.maxodiff.core.service.PhenotypeServiceImpl;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public class MaxodiffBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(MaxodiffBuilder.class);

    private final MaxodiffDataResolver resolver;

    public MaxodiffBuilder(Path dataDirectory) {
        try {
            resolver = MaxodiffDataResolver.of(dataDirectory);
        } catch (MaxodiffDataException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public PhenotypeService phenotypeService()  {
        try {
            Ontology hpo = loadOntology(resolver.hpoJson());
            MaxoDxAnnots dxAnnots = new MaxoDxAnnots(resolver.maxoDxAnnots());
            Map<SimpleTerm, Set<SimpleTerm>> dxmap = dxAnnots.getSimpleTermSetMap();
            HpoDiseaseLoaderOptions options = HpoDiseaseLoaderOptions.defaultOptions();
            HpoDiseases diseases = loadHpoDiseases(resolver.phenotypeAnnotations(), hpo, options);
            return new PhenotypeServiceImpl(hpo, dxmap, diseases);
        } catch (MaxodiffDataException e) {
            throw new RuntimeException(e.getMessage());
        }
    }


    public static Ontology loadOntology(Path ontologyPath) throws MaxodiffDataException {
        try {
            LOGGER.debug("Loading HPO from {}", ontologyPath.toAbsolutePath());
            return OntologyLoader.loadOntology(ontologyPath.toFile());
        } catch (PhenolRuntimeException e) {
            throw new MaxodiffDataException(e);
        }
    }

    public static HpoDiseases loadHpoDiseases(Path annotationPath,
                                       Ontology hpo,
                                       HpoDiseaseLoaderOptions options) throws MaxodiffDataException {
        try {
            LOGGER.debug("Loading HPO annotations from {}", annotationPath.toAbsolutePath());
            HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(hpo, options);
            return loader.load(annotationPath);
        } catch (IOException e) {
            throw new MaxodiffDataException(e);
        }
    }

    static void loadMaxoDiagnosticAnnotations() {

    }



    /*
     private static PhenotypeService configurePhenotypeService(Path dataDirectory, HpoDiseaseLoaderOptions options) throws LiricalDataException {
        LiricalDataResolver liricalDataResolver = LiricalDataResolver.of(dataDirectory);
        Ontology hpo = LoadUtils.loadOntology(liricalDataResolver.hpoJson());
        HpoDiseases diseases = LoadUtils.loadHpoDiseases(liricalDataResolver.phenotypeAnnotations(), hpo, options);
        HpoAssociationData associationData = HpoAssociationData.builder(hpo)
                .hgncCompleteSetArchive(liricalDataResolver.hgncCompleteSet())
                .mim2GeneMedgen(liricalDataResolver.mim2geneMedgen())
                .hpoDiseases(diseases)
                .build();
        return PhenotypeService.of(hpo, diseases, associationData);
    }
     */
}
