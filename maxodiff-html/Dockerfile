FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .



# Copy ALL module poms so Maven can validate the reactor graph
COPY maxodiff-cli/pom.xml maxodiff-cli/
COPY maxodiff-html/pom.xml maxodiff-html/
COPY maxodiff-html-results/pom.xml maxodiff-html-results/
COPY maxodiff-core/pom.xml maxodiff-core/
COPY maxodiff-phenomizer/pom.xml maxodiff-phenomizer/

# Copy sources (including CLI just to satisfy the project structure)
COPY maxodiff-cli/src maxodiff-cli/src
COPY maxodiff-html/src maxodiff-html/src
COPY maxodiff-html-results/src maxodiff-html-results/src
COPY maxodiff-core/src maxodiff-core/src
COPY maxodiff-phenomizer/src maxodiff-phenomizer/src

FROM maven
ARG MAVEN_OPTS
RUN ["mvn", "package", "-pl", "maxodiff-html", "-am", "-DskipTests", "-B", "$MAVEN_OPTS"]
#RUN ./mvnw package -pl maxodiff-html -am -DskipTests -B

# Stage 2: Server runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/maxodiff-html/target/*.jar maxodiff-html.jar
COPY data/ /app/data/
COPY data /app/data


ENV JAVA_OPTS="-Xms8g -Xmx8g"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar maxodiff-html.jar"]