# Setting up maxodiff

## Prerequisites

maxodiff is a Java application. If you want to build maxodiff from source, 
then the build process described below requires
[Git](https://git-scm.com/book/en/v2) and [Java Development Kit (JDK)](https://openjdk.org/) 17 or better.


## Installing maxodiff

### Building from sources

To build maxodiff from sources, go the GitHub page of [maxodiff](https://github.com/monarch-initiative/maxodiff),
and clone or download the project, then build the executable JAR files from sources with Maven:

```
  git clone https://github.com/monarch-initiative/maxodiff.git
  cd maxodiff
  ./mvnw clean package
```

We use the [Maven Wrapper](https://maven.apache.org/wrapper/) for building the sources, so installation
of Maven prior to build is *not* required.

### Download resources

We can download the resource files needed by maxodiff by running the `download` command of the CLI JAR:

```shell
java -jar maxodiff-cli/target/maxodiff-cli.jar download
```

The command will download the data files into `data` folder that will be created in the current working directory.
See the command's documentation for more options.

### Start Maxodiff web app

The web app can be started as:

```shell
java -jar maxodiff-html/target/maxodiff-html.jar
```

By default, the app will serve requests at `http://localhost:8080`.
