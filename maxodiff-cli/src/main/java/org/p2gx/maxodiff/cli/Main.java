package org.p2gx.maxodiff.cli;

import org.p2gx.maxodiff.cli.cmd.BenchmarkingCommand;
import org.p2gx.maxodiff.cli.cmd.BestDxModalityCommand;
import org.p2gx.maxodiff.cli.cmd.DDxCommand;
import org.p2gx.maxodiff.cli.cmd.DownloadCommand;
import org.p2gx.maxodiff.cli.cmd.ManifestVersionProvider;
import org.p2gx.maxodiff.cli.cmd.PrecomputeResnikMapCommand;
import picocli.CommandLine;
import java.util.concurrent.Callable;

@CommandLine.Command(
    name = "maxodiff", 
    mixinStandardHelpOptions = true, 
    versionProvider = ManifestVersionProvider.class,
    description = "maxo terms for differential diagnosis",
    footer = {
            "", // Empty string creates a newline space
            "For detailed documentation, visit https://github.com/p2gx/maxodiff",
            "Enter a subcommand name with -h for help"
        })
public class Main implements Callable<Integer> {

    public static void main(String[] args){
        if (args.length == 0) {
            // if the user doesn't pass any command or option, add -h to show help
            args = new String[]{"-h"};
        }
        CommandLine cline = new CommandLine(new Main())
                .addSubcommand("download", new DownloadCommand())
                .addSubcommand("analyze", new DDxCommand())
                .addSubcommand("modality", new BestDxModalityCommand())
                .addSubcommand("benchmarking", new BenchmarkingCommand())
                .addSubcommand("precompute-resnik", new PrecomputeResnikMapCommand())
                ;
        cline.setToggleBooleanFlags(false);
        int exitCode = cline.execute(args);
        System.exit(exitCode);
    }


    @Override
    public Integer call() {
        // work done in subcommands
        return 0;
    }
}

