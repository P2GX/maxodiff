package org.p2gx.maxodiff.cli.cmd;


import picocli.CommandLine;

/** This extracts the version from the POM file and injects it into picocli */
public class ManifestVersionProvider implements CommandLine.IVersionProvider {
    @Override
    public String[] getVersion() {
        String version = ManifestVersionProvider.class.getPackage().getImplementationVersion();
        
        // Fallback to your default value if running in an IDE or if manifest is missing
        if (version == null) {
            version = "n/a";
        }
        
        return new String[] { "maxodiff v" + version };
    }
}