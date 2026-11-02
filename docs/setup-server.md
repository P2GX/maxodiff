# Web server

The maxodiff web server provides a web page for analyzing a phenopacket and a
[JSON API](api.md) for other programs such as SAMS. You can run it on your own
machine, or in a Docker container.

## Before you start

You need:

- Java 21 or newer.
- The JAR file `maxodiff-html.jar` (see [Installation](setup.md)).
- The data files, created by the `download` command of the
  [command-line tool](app.md#download-the-data). The server expects a folder
  named `data` in the folder where you start it.
- About 8 GB of memory for the server.

## Run the server locally

```bash
java -Xmx8g -jar maxodiff-html.jar
```

Loading the data takes about 20 seconds. When the server is ready, open
<http://localhost:8080/maxodiff> in your browser.

Useful settings (add them after the JAR file name):

| Setting | Meaning | Default |
|---|---|---|
| `--server.port=9090` | Port of the server | `8080` |
| `--maxodiff.data-directory=/path/to/data` | Folder with the data files | `data` |

If Java reports a heap space error or the server stalls at
`Loading Maxodiff resources`, give Java more memory, for example `-Xmx12g`.

## Use the web page

1. Choose a phenopacket file in the **Sample Input** box. The observed and
   excluded HPO terms are displayed.
2. Choose the **number of diseases** (20 or 40) and the **number of
   repetitions** (20 to 100).
3. Choose the output: **HTML** shows the
   [results page](understanding.md), **JSON** returns the raw data.
4. Click **Run maxodiff**.

The analysis takes between 30 seconds and a few minutes, depending on the
number of repetitions and HPO terms.

If a value is outside the allowed range, the page does not run the analysis. It
explains in plain language what is wrong and what you can do.

## Threads and concurrent requests

The analysis needs a lot of CPU time. The server therefore uses **one pool of
analysis threads for the whole application**, no matter how many users are
connected:

- **Pool size:** the larger of 2 and 10% of the available processors. In a
  Docker container, the CPU limit of the container counts.
- **Concurrent analyses:** at most `maxodiff.max-concurrent-analyses` analyses
  run at the same time. Further requests wait for up to
  `maxodiff.admission-wait-seconds`. If no slot becomes free in that time, the
  server answers with **HTTP 503** (server busy), and the client should try
  again later.

| Property | Default | Meaning |
|---|---|---|
| `maxodiff.max-concurrent-analyses` | `2` | Analyses that may run at once |
| `maxodiff.admission-wait-seconds` | `60` | How long a request waits for a free slot before it gets a 503 |

Set them on the command line:

```bash
java -jar maxodiff-html.jar --maxodiff.max-concurrent-analyses=3 --maxodiff.admission-wait-seconds=120
```

Or with environment variables, which is easier in Docker or a service:

```bash
export MAXODIFF_MAX_CONCURRENT_ANALYSES=3
export MAXODIFF_ADMISSION_WAIT_SECONDS=120
java -jar maxodiff-html.jar
```

The command-line tool is separate. It has its own pool with at most 4 threads,
which you can change with `-t` (see [Command line](app.md)).

## Run the server in Docker

### Prepare the data

The server image contains the data files. Create them **before** you build the
image, from the root folder of the project:

```bash
./mvnw clean package
java -jar maxodiff-cli/target/maxodiff-cli.jar download
```

### Build the image

```bash
docker build -f maxodiff-html/Dockerfile -t maxodiff-html:latest .
```

The image is built from the sources, so you do not need Java or Maven on your
machine, only Docker. If Docker runs out of memory during the build, increase
the memory in the Docker settings (8 GB is a good start).

Behind a proxy, pass the proxy to the build:

```bash
docker build -f maxodiff-html/Dockerfile -t maxodiff-html:latest \
  --build-arg http_proxy=<proxy>:<port> --build-arg https_proxy=<proxy>:<port> \
  --build-arg MAVEN_OPTS="-Dhttp.proxyHost=<proxy> -Dhttp.proxyPort=<port> -Dhttps.proxyHost=<proxy> -Dhttps.proxyPort=<port>" .
```

### Start the container

```bash
docker run -d -p 8080:8080 -m 10g --name maxodiff-html-app maxodiff-html:latest
```

| Option | Meaning |
|---|---|
| `-d` | Run in the background. Leave it out to see the log in your terminal. |
| `-p 8080:8080` | Make port 8080 of the container available on port 8080 of your machine |
| `-m 10g` | Memory limit of the container. It must be larger than the Java memory. |
| `--name maxodiff-html-app` | A name that makes it easy to stop the container and read its log |

The server is then available at <http://localhost:8080/maxodiff>.

The Java memory is set to 8 GB by default. Change it with the environment
variable `JAVA_OPTS`, and adapt `-m` accordingly:

```bash
docker run -d -p 8080:8080 -m 6g -e JAVA_OPTS="-Xms4g -Xmx4g" \
  --name maxodiff-html-app maxodiff-html:latest
```

Docker Desktop must have enough memory: choose at least 12 GB under
**Settings > Resources**.

To use data files from a folder on your machine instead of the files in the
image, mount it:

```bash
docker run -d -p 8080:8080 -m 10g -v "$(pwd)/data:/app/data" \
  --name maxodiff-html-app maxodiff-html:latest
```

### Everyday commands

| Task | Command |
|---|---|
| Show all containers, also stopped ones | `docker ps -a` |
| Read the log (first place to look after a failed start) | `docker logs maxodiff-html-app` |
| Stop the container | `docker stop maxodiff-html-app` |
| Remove the container | `docker rm maxodiff-html-app` |

### Rebuild after a change

```bash
docker rm -f maxodiff-html-app
docker rmi maxodiff-html:latest
docker build -f maxodiff-html/Dockerfile -t maxodiff-html:latest .
docker run -d -p 8080:8080 -m 10g --name maxodiff-html-app maxodiff-html:latest
```

## Run the command-line tool in Docker

A second image runs one analysis on the command line and exits. This is
convenient for batch runs. It uses the JAR file that you built, so build the
project first:

```bash
./mvnw clean package
java -jar maxodiff-cli/target/maxodiff-cli.jar download
docker build -f maxodiff-cli/Dockerfile -t maxodiff-cli:latest .
```

Run an analysis. The phenopacket is read from a folder that you mount, and the
JSON result is written to another folder:

```bash
docker run --rm \
  -v "$(pwd)/docs/examples:/input" \
  -v "$(pwd)/results:/output" \
  -e PPKT=/input/example-phenopacket.json \
  -e OUT=/output \
  maxodiff-cli:latest
```

| Variable | Default | Meaning |
|---|---|---|
| `PPKT` | (required) | Path of the phenopacket **inside** the container |
| `OUT` | | Folder for the result **inside** the container |
| `NDISEASES` | `20` | Number of diseases |
| `NREPS` | `100` | Number of repetitions |

The container always writes JSON. The Java memory is set to 16 GB, so your
Docker installation needs at least this much memory.

## Troubleshooting

### Port already in use

```text
Web server failed to start. Port 8080 was already in use.
```

Either use another port (`--server.port=9090`), or find the process that blocks
the port:

```bash
lsof -i :8080
```

The output shows the process id (PID). Stop the process with
`kill <PID>`. Use `kill -9 <PID>` only if it does not react.

### Out of memory

If the server stalls at `Loading Maxodiff resources` and then fails, give Java
more memory (`-Xmx12g`). In Docker, raise both `JAVA_OPTS` and the container
limit `-m`.

### The server answers with 503

The server is busy. Wait a moment and try again, or increase
`maxodiff.max-concurrent-analyses` if your machine has more capacity.

### Connection refused during build or download

You may be behind a proxy. For Maven, set it before the build:

```bash
export MAVEN_OPTS="-Dhttp.proxyHost=<host> -Dhttp.proxyPort=<port> -Dhttps.proxyHost=<host> -Dhttps.proxyPort=<port>"
./mvnw clean package
```

For `download`, pass the proxy to Java:

```bash
java -Dhttps.proxyHost=<host> -Dhttps.proxyPort=<port> -jar maxodiff-cli/target/maxodiff-cli.jar download
```