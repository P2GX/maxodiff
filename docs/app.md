# Command line

The command-line tool runs maxodiff on one phenopacket and writes the results
to a file. Use it for batch analyses, scripting, or if you do not want to run a
server.

!!! note "Before you start"
You need Java 21 or newer, the maxodiff JAR file, and the downloaded data
files. See [Installation](setup.md).

All examples assume that you run them from the folder that contains the JAR
file and the `data` folder.

## Commands

Run the JAR without arguments to see the list of commands:

```bash
java -jar maxodiff-cli.jar
```

| Command | Short | Purpose |
|---|---|---|
| `download` | `D` | Download the HPO and MAxO data files and prepare them for analysis |
| `analyze` | `A` | Analyze one phenopacket (the main command) |
| `modality` | `M` | Find the best diagnostic procedures for one named target disease |
| `precompute-resnik` | | Recompute the term similarity file from the downloaded data |
| `benchmarking` | `BX` | Evaluate maxodiff on many phenopackets (for developers) |

Add `-h` to any command for its full list of options, for example
`java -jar maxodiff-cli.jar analyze -h`. Use `-V` to print the version.

## Download the data

```bash
java -jar maxodiff-cli.jar download
```

This downloads `hp.json`, `phenotype.hpoa`, `maxo.json`, and the MAxO
diagnostic annotations into a `data` folder, and then computes the term
similarity file `data/term-pair-similarity.ser` (the file is about 100 MB and
takes a few minutes to compute).

| Option | Meaning |
|---|---|
| `-d`, `--data` | Folder for the data files (default: `data`) |
| `-w`, `--overwrite` | Replace files that were downloaded earlier |

HPO and MAxO are updated several times a year. To update your data, run the
command again with `-w`.

## Analyze one phenopacket

```bash
java -jar maxodiff-cli.jar analyze -p patient.json
```

This runs the analysis with default settings and writes an HTML file in the
current folder. A progress bar shows how far the analysis is.

### Options

| Option | Default | Meaning |
|---|---|---|
| `-p`, `--phenopacket` | (required) | Path to the phenopacket JSON file |
| `-n`, `--nDiseases` | `20` | Number of top-ranked diseases that are analyzed |
| `-nr`, `--nRepetitions` | `80` | Number of simulations per procedure. More repetitions give more stable results and take longer. |
| `-t`, `--nThreads` | smaller of 4 and the number of CPU cores | Number of threads |
| `-j`, `--json` | off | Write JSON instead of HTML |
| `-m`, `--maxoData` | `data` | Folder with the downloaded data files |
| `-f`, `--outFilename` | | Name of the output file (the suffix `.html` or `.json` is added if missing) |
| `-O`, `--outputDirectory` | current folder | Folder for the output file (only used together with `-f`) |
| `-v` | | More log output; repeat for more detail (`-vv`) |

### Examples

Use 40 diseases and 100 repetitions:

```bash
java -jar maxodiff-cli.jar analyze -p patient.json -n 40 -nr 100
```

Write JSON:

```bash
java -jar maxodiff-cli.jar analyze -p patient.json -j
```

Choose the name and the folder of the output file:

```bash
java -jar maxodiff-cli.jar analyze -p patient.json -O results -f patient1
```

This writes `results/patient1.html`.

### Output

Without `-f`, the file is written to the current folder and named after the
sample id of the phenopacket:

```text
<sample id>_<nDiseases>_<nRepetitions>_maxodiff_results.html
```

The HTML file is self-contained. You can open it in a browser without internet
access and send it to colleagues. [Reading the results](understanding.md)
explains the page. The JSON file contains all procedures with a score greater
than zero, with the full details.

### Running time

The running time grows with the number of repetitions and diseases and falls
with more threads. With the defaults, an analysis takes about a minute on a
laptop. Do not set the number of threads higher than the number of CPU cores of
your machine.

## Best procedures for a target disease

If you already suspect one disease, `modality` ranks procedures with respect to
that disease:

```bash
java -jar maxodiff-cli.jar modality -p patient.json --targetDisease OMIM:157700 -j
```

| Option | Meaning |
|---|---|
| `-p`, `--phenopacket` | Phenopacket file (required) |
| `--targetDisease` | Identifier of the target disease, for example `OMIM:157700` (required). The prefix must be `OMIM`, `ORPHA`, or `DECIPHER`. |
| `-j`, `--json` | Write the results to a JSON file named `<sample id>_maxodiff_modalities.json` |
| `-O`, `--outputDirectory` | Folder for the JSON file |
| `-t`, `--nThreads` | Number of threads |

## Troubleshooting

**`Could not find phenopacket file`**
: The path after `-p` is wrong. Check it and the current folder.

**The program stalls for a long time at `Loading Maxodiff resources`, then fails**
: Java has not enough memory. Allow more with `-Xmx`, for example
`java -Xmx8g -jar maxodiff-cli.jar analyze -p patient.json`.

**`Connection refused` or a connection timeout during `download`**
: You may be behind a proxy. Pass the proxy to Java:
`java -Dhttps.proxyHost=<host> -Dhttps.proxyPort=<port> -jar maxodiff-cli.jar download`.

**`HPO file does not exist`**
: You have not downloaded the data yet, or you run the command from a different
folder. Run `download` first or point to the files with `--hpo` and `--hpoa`.