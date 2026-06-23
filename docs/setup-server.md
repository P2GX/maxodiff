# Setting up the maxodiff server


## Threads

The `application.properties` file at `maxodiff/maxodiff-html/src/main/resources/application.properties` contains the line

```bash
maxodiff.threads=#{T(java.lang.Runtime).getRuntime().availableProcessors() - 1}
```
This defaults to the number of available processors minus 1, but can be overridden. It is picked up the Spring autowiring system in the class `MaxodiffController` as follows:

```java
public MaxodiffController(
        UserSessionData sessionData,
        MdContext context,
        DiffDiagRefiner diffDiagRefiner,
        @Value("${maxodiff.threads:4}") int nthreads) {
                    // ...
        }
```

To override this via the command line, enter
```bash
java -jar maxodiff-app.jar --maxodiff.threads=8
```

To deploy the Spring Boot application inside a Docker container or via a systemd service, it is better to set an environment variable. Spring Boot automatically translates uppercase, underscore-separated environment variables into dot-notation properties.
```bash
export MAXODIFF_THREADS=12
java -jar maxodiff-app.jar
```
