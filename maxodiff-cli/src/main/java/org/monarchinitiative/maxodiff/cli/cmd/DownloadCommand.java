package org.monarchinitiative.maxodiff.cli.cmd;

import org.monarchinitiative.biodownload.BioDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Download a number of files needed for the analysis.
 * <p>
 * By default, we download by default to a subdirectory called {@code data}, which is created if necessary.
 * We download the files {@code hp.json}, {@code maxo.json}, {@code phenotype.hpoa}, and
 * @code Homo_sapiens_gene_info.gz}
 *
 * @author <a href="mailto:martha.beckwith@jax.org">Martha Beckwith</a>
 * @author <a href="mailto:daniel.gordon.danis@protonmail.com">Daniel Danis</a>
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
@CommandLine.Command(
    name = "download", 
    aliases = {"D"},
    mixinStandardHelpOptions = true,
    description = "Download files for maxodiff"
)
public class DownloadCommand implements Callable<Integer>{

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadCommand.class);
    
    @CommandLine.Option(
        names={"-d","--data"}, 
        description ="directory to download data (default: ${DEFAULT-VALUE})"
    )
    public Path datadir= Path.of("data");

    @CommandLine.Option(
        names={"-w","--overwrite"}, 
        description = "overwrite previously downloaded files (default: ${DEFAULT-VALUE})"
    )
    public boolean overwrite;

    @Override
    public Integer call() throws Exception {
        LOGGER.info("Downloading maxodiff data files to {}", datadir.toAbsolutePath());
        downloadMaxodiffData(datadir, overwrite);
        LOGGER.info("Making Phenomizer Term Pair Similarity File...");
        makePhenomizerTermPairSimilarity(datadir);

//        Path liricalDataPath = datadir.resolve("lirical");
//        LOGGER.info("Downloading LIRICAL data files to {}", liricalDataPath.toAbsolutePath());
//        downloadLiricalData(liricalDataPath, overwrite);
        
        LOGGER.info("Download is complete!");
        LOGGER.info("Bye! 😎");
        return 0;
    }

    private static void downloadLiricalData(Path destinationFolder, boolean overwrite) throws Exception {
        BioDownloader downloader = BioDownloader.builder(destinationFolder)
                .overwrite(overwrite)
                .hpoJson()
                .hpDiseaseAnnotations()
                .hgnc()
                .medgene2MIM()
                .build();
        downloader.download();
    }

    private static void downloadMaxodiffData(Path destinationFolder, boolean overwrite) throws Exception { 
        BioDownloader downloader = BioDownloader.builder(destinationFolder)
                .overwrite(overwrite)
                .hpoJson()
                .hpDiseaseAnnotations()
                .hgnc()
                .medgene2MIM()
                .maxoJson()
                .custom("maxo_diagnostic_annotations.tsv", createUrlOrExplode("https://raw.githubusercontent.com/monarch-initiative/maxo-annotations/master/annotations/maxo_diagnostic_annotations.tsv"))
                .build();
        downloader.download();
    }

    private static void makePhenomizerTermPairSimilarity(Path destinationFolder) {

        String hpFile = String.join(File.separator, destinationFolder.toString(), "hp.json");
        String hpoaFile = String.join(File.separator, destinationFolder.toString(), "phenotype.hpoa");
        String outputFile = String.join(File.separator, destinationFolder.toString(), "term-pair-similarity.csv.gz");

        String maxodiffDir = System.getProperty("user.dir");
        String maxodiffCliJar = String.join(File.separator, maxodiffDir, "maxodiff-cli", "target", "maxodiff-cli.jar");

        String[] command = new String[] {
                "java",
                "-jar",
                maxodiffCliJar,
                "precompute-resnik",
                "--hpo=" + hpFile,
                "--hpoa=" + hpoaFile,
                "--output=" + outputFile
        };

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));

            writer.newLine();
            writer.close();

            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static URL createUrlOrExplode(String url) throws Exception {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new Exception(e);
        }
    }

}

