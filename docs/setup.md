# Setting up maxodiff

MaxoDiff can be run from the command-line or as a Web application.

## Prerequisites

maxodiff is a Java application. If you want to build maxodiff from source,
then the build process described below requires
[Git](https://git-scm.com/book/en/v2) and [Java Development Kit (JDK)](https://openjdk.org/) 21 or newer.


## Installing maxodiff

### Building from sources

To build maxodiff from sources, go the GitHub page of [maxodiff](https://github.com/P2GX/maxodiff),
and clone or download the project, then build the executable JAR files from sources with Maven:

```
  git clone https://github.com/P2GX/maxodiff.git
  cd maxodiff
  ./mvnw clean package
```

We use the [Maven Wrapper](https://maven.apache.org/wrapper/) for building the sources, so installation
of Maven prior to build is *not* required.

### Download resources

We can download the resource files needed by maxodiff by running the `download` command
of the CLI JAR:
```shell
java -jar maxodiff-cli/target/maxodiff-cli.jar download
```

The command will download the data files into `data` folder that will be created in the current working directory.
See the command's documentation for more options.

### MICA dictionary
To run MaxoDiff with Phenomizer, we must first generate a file that contains the
most informative common ancestors for pairs of HPO terms. See the class ``IcMicaDictLoader``.

MaxoDiff uses the Phenomizer algorithm to calculate the differential diagnosis; see
the original publication for details:
[Clinical diagnostics in human genetics with semantic similarity searches in ontologies](https://pubmed.ncbi.nlm.nih.gov/19800049/)

To run this algorithm, we require
the information content of the most informative common ancestor (MICA) for
every pair of terms. It is more efficient to precalculate this using the
following command. By default, the command will look for the input files ``hp.json``
and ``phenotype.hpoa`` in the ``data``folder, which is created by the download command.
```shell
java -jar maxodiff-cli/target/maxodiff-cli.jar precalculate-resnik
```
On a modern commodity laptop, this command will require a few minutes to complete.
By default, the output file will be created here. Currently, it occupies 117 Mb.
```shell
data/term-pair-similarity.csv.gz
```


### Start Maxodiff web app

The web app can be started as:

```shell
java -jar maxodiff-html/target/maxodiff-html.jar
```

By default, the app will serve requests at `http://localhost:8080`.
