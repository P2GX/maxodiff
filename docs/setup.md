# Setting up maxodiff

## Prerequisites

maxodiff is a Java application. If you want to build maxodiff from source, 
then the build process described below requires
[Git](https://git-scm.com/book/en/v2) and [Java Development Kit (JDK)](https://openjdk.org/).


## Installing maxodiff

### Building from sources

To build maxodiff from sources, go the GitHub page of [maxodiff](https://github.com/monarch-initiative/maxodiff),
and clone or download the project, then build the executable from source with Maven:

```
  git clone https://github.com/monarch-initiative/maxodiff.git
  cd maxodiff
  ./mvnw clean package
```

We use the [Maven Wrapper](https://maven.apache.org/wrapper/) for building the sources, so installation
of Maven prior to build is *not* required.

The executable jar is located at the *maxodiff-html/target* subdirectory.

After running the executable jar, the website will be available at http://localhost:8080/.

