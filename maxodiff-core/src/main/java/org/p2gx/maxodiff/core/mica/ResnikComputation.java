package org.p2gx.maxodiff.core.mica;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.similarity.TermPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResnikComputation {
    private final static Logger LOGGER = LoggerFactory.getLogger(ResnikComputation.class);

    public static Map<TermPair, Double> precompute(MinimalOntology hpo, HpoDiseases diseases, boolean assumeAnnotated) {
        Map<TermId, Double> termToIc = ResnikComputation.calculateTermToIc(hpo, diseases, assumeAnnotated);
        LOGGER.trace("Assigning MICA information content to term pairs");
        Map<TermPair, Double> termPairResnikSimilarityMap = assignMicaToTermPairs(hpo, termToIc);
        LocalDate date = LocalDate.now();
        String hpoVersion = hpo.version().orElse("N/A");
        String hpoaVersion = diseases.version().orElse("N/A");
        LOGGER.trace("Calculated Resnik scores for HPO version {}, HPOA version {} on {}", 
            hpoVersion, hpoaVersion, date);
        return termPairResnikSimilarityMap;
    }

    public static void precomputeAndOutput(
            MinimalOntology hpo,
            HpoDiseases diseases,
            boolean assumeAnnotated,
            Path output) throws IOException {
        Map<TermPair, Double> resnikMap = precompute(hpo, diseases, assumeAnnotated);
        writeSerializedMap(resnikMap, output);
    }

    public static void writeSerializedMap(Map<TermPair, Double> map, Path output) throws IOException {
        int size = map.size();
        String[] termsA = new String[size];
        String[] termsB = new String[size];
        double[] icMicas = new double[size];

        int i = 0;
        int progressStep = Math.max(1, map.size() / 100);
        int effectiveSize = map.size() / 100;
      
        ProgressBar progress = new ProgressBar("Serializing", effectiveSize);
        for (Map.Entry<TermPair, Double> e : map.entrySet()) {
            termsA[i] = e.getKey().getTidA().getValue();
            termsB[i] = e.getKey().getTidB().getValue();
            icMicas[i] = e.getValue();

            if (i % progressStep == 0 || i == size - 1) {
                progress.step();
            }
            i++;
        }

        ResnikFlatData flatData = new ResnikFlatData(termsA, termsB, icMicas);

        // Write the entire structural block in one shot
        try (OutputStream fos = Files.newOutputStream(output);
                OutputStream gzos = new GZIPOutputStream(fos); // Keeps file small on disk
                ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(gzos))) {
            oos.writeObject(flatData);
        }
    }

    private static Map<TermId, Double> calculateTermToIc(MinimalOntology hpo, HpoDiseases diseases,
            boolean assumeAnnotated) {
        MicaCalculator micaCalculator = new MicaCalculator(hpo, assumeAnnotated);
        return micaCalculator.calculateMica(diseases).termToIc();
    }

    private static Map<TermPair, Double> assignMicaToTermPairs(MinimalOntology hpo, Map<TermId, Double> termToIc) {
        return HpoResnikSimilarityPrecomputer.precomputeSimilaritiesForTermPairs(hpo, termToIc);
    }


    public static Map<TermPair, Double> loadSerializedResnik(Path input) throws IOException, ClassNotFoundException {
        ResnikFlatData flatData;

        System.err.println("Loading serialized binary block from disk...");
        long startTime = System.currentTimeMillis();
        try (InputStream fis = Files.newInputStream(input);
                ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new GZIPInputStream(fis)))) {
            flatData = (ResnikFlatData) ois.readObject();
        }

        long ioEndTime = System.currentTimeMillis();
        System.err.printf("Binary data block loaded in %d ms. Reconstructing map...%n", (ioEndTime - startTime));

        // 2. Unpack the parallel arrays back into the Map structure
        int size = flatData.icMicas().length;

        // Optimize Map allocation by specifying the target capacity upfront to avoid
        // rehashing resizing passes
        Map<TermPair, Double> resnikMap = new HashMap<>((int) (size / 0.75f) + 1);

        String[] termsA = flatData.termsA();
        String[] termsB = flatData.termsB();
        double[] micas = flatData.icMicas();

        int progressStep = Math.max(1, size / 100);
        ProgressBar progress = new ProgressBar("Serializing", progressStep);
        for (int i = 0; i < size; i++) {
            TermId tidA = TermId.of(termsA[i]);
            TermId tidB = TermId.of(termsB[i]);
            TermPair tp = TermPair.symmetric(tidA, tidB);
            resnikMap.put(tp, micas[i]);
              if (i % progressStep == 0 || i == size - 1) {
                progress.step();
            }
            i++;
           
        }
        System.err.println();

        long totalEndTime = System.currentTimeMillis();
        System.err.printf("Map fully built! Total load time: %d ms.%n", (totalEndTime - startTime));

        return resnikMap;
    }

}
