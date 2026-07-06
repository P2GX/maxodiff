package org.p2gx.maxodiff.cli.cmd;


import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.MaxodiffAnalysisRunner;
import org.p2gx.maxodiff.core.analysis.HpoFrequency;
import org.p2gx.maxodiff.core.analysis.RankedMaxoResultSingleDisease;
import org.p2gx.maxodiff.core.analysis.refinement.DiffDiagRefiner;
import org.p2gx.maxodiff.core.diffdg.DDxEngine;
import org.p2gx.maxodiff.core.io.JsonWriter;
import org.p2gx.maxodiff.core.io.MdContext;
import org.p2gx.maxodiff.core.io.impl.MdContextBuilder;
import org.p2gx.maxodiff.core.model.PhenopacketData;
import org.p2gx.maxodiff.core.phenomizer.PhenomizerDDxEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;

@CommandLine.Command(
        name = "modality",
        aliases = {"M"},
        mixinStandardHelpOptions = true,
        description = "find best diagnostic modality")
public class BestDxModalityCommand extends BaseCommand {
     private static final Logger LOGGER = LoggerFactory.getLogger(DDxCommand.class);


     private int nRepetitions = 80;
     private int nDiseases = 20;

    @CommandLine.Option(
        names = {"-p", "--phenopacket"},
        required = true,
        description = "Path to phenopacket JSON file.")
    private Path phenopacketPath;

    @CommandLine.Option(names = {"-t", "--nThreads"},
        description = "Number of threads to use for analysis.")
    protected int nThreads = Runtime.getRuntime().availableProcessors() - 1;

    @CommandLine.Option(names = {"--targetDisease"},required=true,
        description="OMIM Identifier of the target disease"
    )
    private String targetDiseaseId;

     @CommandLine.Option(names = {"-j", "--json"},
        description = "output results to JSON file")
    private boolean outputJson = false;

    @CommandLine.Option(names = {"-O", "--outputDirectory"},
            description = "Where to write the results files (default: ${DEFAULT-VALUE}).")
    protected Path outputDir = Path.of(".");

    @Override
    protected Integer execute() throws Exception {
        if (!Files.exists(phenopacketPath)) {
            System.err.println("Could not find phenopacket file: " + phenopacketPath);
            return 1;
        }
        try {
             PhenopacketData phenopacketData = PhenopacketData.readPhenopacketData(phenopacketPath);
            getBestDxModalities(phenopacketData);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return 1;
        }
        return 0;
    }



    private void getBestDxModalities(PhenopacketData pdata) throws Exception {
        MdContext context = MdContextBuilder.buildContext(
                this.maxoDataPath,
                this.nRepetitions,
                this.nDiseases,
                true);  
        List<HpoFrequency> allHpoFrequencies = context.createHpoFrequencies();
        DiffDiagRefiner maxoDiffRefiner = context.createRefiner();
        DDxEngine engine = new PhenomizerDDxEngine(context);
        PhenopacketData phenopacketData = PhenopacketData.readPhenopacketData(phenopacketPath);
        MaxodiffAnalysisRunner runner = new MaxodiffAnalysisRunner(
                context,
                nThreads,
                engine,
                maxoDiffRefiner,
                allHpoFrequencies);
        TermId targetTermId = TermId.of(targetDiseaseId);
        List<RankedMaxoResultSingleDisease> resultsList = runner.analyzeSampleSingleDisease(phenopacketData,
                                                                                            targetTermId);
       
        if(this.outputJson) {
            String jsonFilename = String.join("_", phenopacketData.sampleId(), "maxodiff_modalities.json");
            Path jsonPath = Path.of(String.join(File.separator, outputDir.toString(), jsonFilename));
            int zeroIdx = resultsList.stream()
                .filter(result -> result.maxoScore() == 0.)
                .findFirst().map(resultsList::indexOf).orElse(resultsList.size());
          int nDisplayed = Math.min(resultsList.size(), zeroIdx);

        JsonWriter.writeModalitiesToJsonFile(jsonPath, resultsList.subList(0, nDisplayed));
         LOGGER.debug("Wrote JSON file to {}.", jsonPath);

        } else {
            for (var result: resultsList.subList(0,10)) {
                System.out.println(result.maxoScore() + "TODO TOSTRING FOR RESULTS");
            }
        }

       
    }
    
}
