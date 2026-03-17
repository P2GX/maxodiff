# Use a base image with Java installed
FROM azul/zulu-openjdk:21

# Set the working directory inside the container
WORKDIR maxodiff-cli/target/

# Copy maxodiff-cli JAR file into the container
COPY maxodiff-cli/target/maxodiff-cli.jar maxodiff-cli.jar

# Expose the port the web service listens on (e.g., 8080)
EXPOSE 8080

# Command to run the application when the container starts
CMD ["java", "-Xmx8g", "-jar", "maxodiff-cli.jar", "analyze", "-p", "$PPKT", "-j"]
