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


## Docker

To create the docker container

- Start Docker
- You many need to increase memory in the settings (in the Docker contained). Choose 8gb to start if you have issues
- Run the following command from the root project directory

```shell
docker build --no-cache -f maxodiff-html/Dockerfile -t maxodiff-html:latest .
```

Assuming this runs without error, we can now start the Docker process

```shell
docker run -dp 8080:8080 --name maxodiff-html-app maxodiff-html:latest
```

Explanations:

- `-d`: detached mode (Runs the container in the background)
- `-p 8080:8080`: (Port mapping): Maps port 8080 on your host machine to port 8080 inside the Docker container.
- `--name maxodiff-html-app`: Gives the running container a name so it's easy to stop or check logs later.

You should now see `maxodiff-html-app` in the list of Containers in the Docker GUI.



### Tips

To see all processes (including exited)
```bash
docker ps -a
CONTAINER ID   IMAGE                  COMMAND                  CREATED             STATUS                      PORTS     NAMES
7c5a93bf2bac   maxodiff-html:latest   "java -jar maxodiff-…"   About an hour ago   Exited (1) 14 minutes ago             maxodiff-html-app
```

- Read the Java/Spring Boot error logs:
```bash
docker logs maxodiff-html-app
```

To remove a running container from docker
```bash
docker rm maxodiff-html-app
```

- To run in foreground -- without detach -- for debugging
```bash
docker run -it -p 8080:8080 --name maxodiff-html-app maxodiff-html:latest
```

### WIPE and Rebuild Docker

Make changes, rebuild the app, Remove the old container and image trace, and rebuld the new docker image, finally restart
```bash
mvn clean package
docker rm maxodiff-html-app
docker rmi maxodiff-html:latest
docker build --no-cache -f maxodiff-html/Dockerfile -t maxodiff-html:latest .
docker run -it -p 8080:8080 --name maxodiff-html-app maxodiff-html:latest
```


Here is a script to start the server with mappings to our local directories
```bash
#!/bin/bash
docker rm -f maxodiff-html-app 2>/dev/null
docker run -it -p 8080:8080 \
  --name maxodiff-html-app \
  -m 8g \
  -e JAVA_TOOL_OPTIONS="-Xms4g -Xmx8g" \
  -v "$(pwd)/data:/app/data" \
  -v "$HOME/maxodiff:/root/maxodiff" \
  maxodiff-html:latest
```

### Testing

- To test the container, we can use [postman](https://www.postman.com/).        
- Use Postman Desktop app (Note: do not use the Postman Agent app, it is not suited for this)
- Create a new request, set it to POST at http://localhost:8080/api/analyze, 
- Set the BODY to JSON and paste the JSON file for a phenopacket
- Click on Send
- The calculations will take 10 seconds, and then JSON code will be returned.


