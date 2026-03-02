# Tutorial

This tutorial explains how to set up and run the command-line interface (CLI) version of maxodiff. Instructions for the online version of maxodiff can be found here 
!!! danger "🚨 TODO"
    <span class="todo-pulse">Fill in the correct link to the online version of maxodiff!</span>


## Requirements

maxodiff requires a Java environment with version 21 or newer. We recommend installing the full JDK, since separate runtime environments are no longer shipped by most vendors. Major operating systems have package managers that are often the best option. For instance, on Apple, one can install Java with homebrew.

```bash
brew install openjdk@21
``` 

If desired, one can also obtain installation files from many sources including [Oracle](https://www.oracle.com/br/java/technologies/downloads/){:target="_blank"} and [azul](https://www.azul.com/downloads/?package=jdk#zulu){:target="_blank"}.

## Requirements to build from source

maxodiff was developed using the [Apache maven](https://maven.apache.org/){:target="_blank"} build system. It uses a mvnw build script so that users do not necessarily need to install the maven software to build it. Perform the following steps.

1. Clone  the repository from GitHub (or download a Zip-archive if desired)
```bash
git clone https://github.com/P2GX/maxodiff.git
``` 
2. Enter into the directory and build the executable with maven.
This can be done using the script
```bash
cd maxodiff
./mvnw package
``` 
Or using an installed version of maven
```bash
cd maxodiff
mvn package
``` 
3. Check for success.
```bash
java -jar maxodiff-cli/target/maxodiff-cli.jar
``` 
If the executable was successfully created, you should see the following in your shell
```bash
Usage: maxodiff [-hV] [COMMAND]
maxo terms for differential diagnosis
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
Commands:
  download, D        Download files for maxodiff
  analyze, a         maxodiff analysis
  batch, b           batch maxodiff analysis
  benchmark, B       benchmark maxodiff analysis
  precompute-resnik  Precompute Resnik term pair similarity table
``` 

These steps generally should work if Java is correctly installed on your system. If you encounter issues, first try the following command and check that Java is correctly installed and has at least version 21.
```bash
java -version
``` 
For instance, you should see something like this

```bash
java -version
openjdk version "23.0.2" 2025-01-21
OpenJDK Runtime Environment Zulu23.32+11-CA (build 23.0.2+7)
OpenJDK 64-Bit Server VM Zulu23.32+11-CA (build 23.0.2+7, mixed mode, sharing)
``` 

## Setting up maxodiff-cli

The maven build process creates the executable file in the maxodiff-cli/target folder. If desired, you can move the executable to any convenient location. The following examples work if the executable is in the current working directory.

First we need to download input files.
```bash
java -jar maxodiff-cli.jar download
``` 

This step should take 10-20 seconds. It will show status messages along the way and finish with the message "Download is complete!".
The HPO files are updated about 6 times a year. This command can be used to update the downloaded files if the ``-w`` (or ``--overwrite``) flag is passed.
This command automatically creates the similarity files (Resnik) for the Phenomizer analysis.

The command can be run separately if needed.

```bash
java -jar maxodiff-cli.jar precompute-resnik
```

This command, which will take about 2-3 minutes to run, creates the file
``term-pair-similarity.csv.gz`` in the ``data`` subfolder (This subfolder was created by the download command and is
the place where the other data files are kept).

!!! note "Note"
    The ``precompute-resnik`` command needs to be run once every time the data files are updated.


## Running maxodiff

The CLI version of maxodiff can be run with default values using the analyze command, specifying a path to a phenopacket of interest:

```bash
java -jar maxodiff-cli/target/maxodiff-cli.jar analyze -p <path to phenopacket file> 
```

To have the program output a JSON file with the results instead of the HTML file, use the above command but add a ``-j/--json``.

```bash
java -jar maxodiff-cli/target/maxodiff-cli.jar analyze -p <path to phenopacket file> -j 
```


# Server
We can start a local server as follows. First download the files as mentioned above.
Then enter

```bash
java -jar maxodiff-html/target/maxodiff-html.jar
```

If a Java heap space error occurs, more memory can be allocated by adding VM options:

```shell
java -Xmx8g -jar maxodiff-html/target/maxodiff-html.jar
```

A server will start at ``http://localhost:8080/maxodiff``

We see a data entry page with Clinician and Researcher view (Note: We will combine these later on so that the results page has both).
The styling is still preliminary, but it works.