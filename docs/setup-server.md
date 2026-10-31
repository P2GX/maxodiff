# Setting up the maxodiff server

## Threads and concurrent requests

The analysis is CPU-intensive. The server therefore uses **one pool of analysis threads for the whole
application**, however many users are connected:

- **Pool size:** `max(2, round(0.10 × available processors))`. Inside a Docker container or Kubernetes pod the
  CPU limit counts as the number of processors.
- **Concurrent analyses:** at most `maxodiff.max-concurrent-analyses` analyses run at the same time.
  Further requests wait up to `maxodiff.admission-wait-seconds`. If no slot becomes free in that time, the server
  answers with **HTTP 503** ("The server is busy…"), and the client should try again later.

| Property | Default | Meaning |
|---|---|---|
| `maxodiff.max-concurrent-analyses` | `2` | Analyses allowed to run at once. The queue for waiting tasks is sized from this value. |
| `maxodiff.admission-wait-seconds` | `60` | How long a request waits for a free slot before getting a 503. |

To override a value on the command line:

```bash
java -jar maxodiff-app.jar --maxodiff.max-concurrent-analyses=3 --maxodiff.admission-wait-seconds=120
```

In a Docker container or a systemd service it is easier to set environment variables. Spring Boot translates
uppercase, underscore-separated names into the dotted property names:

```bash
export MAXODIFF_MAX_CONCURRENT_ANALYSES=3
export MAXODIFF_ADMISSION_WAIT_SECONDS=120
java -jar maxodiff-app.jar
```

The pool size itself is not a property. It follows the 10% rule above.

The command-line tool is separate: it creates its own pool of `min(4, available processors)` threads, which you
can change with `-t` / `--nThreads`.


## Docker

To create the docker container

- Start Docker
- You many need to increase memory in the settings (in the Docker container). Choose 8gb to start if you have issues
- Run the following command from the root project directory

```shell
docker build --no-cache -f maxodiff-html/Dockerfile -t maxodiff-html:latest .
```

- To build using a proxy server, run the following command:

```shell
docker build --no-cache -f maxodiff-html/Dockerfile -t maxodiff-html:latest --build-arg http_proxy=<proxy-server-address>:<proxy-port-number> --build-arg https_proxy=<proxy-server-address>:<proxy-port-number> --build-arg MAVEN_OPTS="-Dhttp.proxyHost=<proxy-server-address> -Dhttp.proxyPort=<proxy-port-number> -Dhttps.proxyHost=<proxy-server-address> -Dhttps.proxyPort=<proxy-port-number>" .
```

Assuming the build runs without error, we can now start the Docker process.

- Because your JVM heap alone requires 8GB, the total memory consumed by the container will be higher
- Make sure the global Docker virtual machine has enough resources allocated to it. Go to Settings > Resources > Virtual manual / Advanced and ensure the memory slider is set to at least 12GB.

```shell
docker run -m 10g -p 8080:8080 --name maxodiff-html-app maxodiff-html:latest
```

Explanations:

- `-d`: detached mode (Runs the container in the background)
- `-p 8080:8080`: (Port mapping): Maps port 8080 on your host machine to port 8080 inside the Docker container.
- `--name maxodiff-html-app`: Gives the running container a name so it's easy to stop or check logs later.

You should now see `maxodiff-html-app` in the list of Containers in the Docker GUI.


- The docker process should now be visible at http://localhost:8080/maxodiff.



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

- Make changes and rebuild the app
- Remove the old container and image trace, and rebuild the new docker image
- Finally, restart
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

### Testing with Postman


#### maxodiff (standard algorithm)

- To test the container, we can use [postman](https://www.postman.com/).        
- Use Postman Desktop app (Note: do not use the Postman Agent app, it is not suited for this).
- Create a new request, set it to POST at http://localhost:8080/api/analyze. 
- Set the BODY to JSON and paste the JSON file for a phenopacket.
- Click on Send.
- The calculations will take 10 seconds, and then JSON code will be returned.


#### maxodiff single-disease

- Enter POST.
- Use the API http://localhost:8080/api/modality?targetDiseaseId=OMIM:157700. (substitute any OMIM id).
- As body, paste in a complete phenopacket.
- Click on SEND.
- This will return JSON like this:

```json
[
    {
        "targetDisease": {
            "tid": "OMIM:157700"
        },
        "maxoTerm": {
            "tid": "MAXO:0035050"
        },
        "nMaxoPhenotypes": 1,
        "totalIC": 9.061724347647399,
        "maxoScore": 8.499235179380303,
        "rankedOmimTerm": {
            "tid": "OMIM:157700",
            "averageRank": 0.0
        }
    },
    {
        "targetDisease": {
            "tid": "OMIM:157700"
        },
        "maxoTerm": {
            "tid": "MAXO:0000706"
        },
        "nMaxoPhenotypes": 2,
        "totalIC": 7.263296516746243,
        "maxoScore": 16.172687354838573,
        "rankedOmimTerm": {
            "tid": "OMIM:157700",
            "averageRank": 0.0
        }
    },
(,,,)
]
```

