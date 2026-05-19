# CI deve correr `mvn test` antes de `docker build`. Esta imagem só empacota o JAR.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -e -B dependency:go-offline
COPY src ./src
RUN mvn -q -e -B -DskipTests package

FROM eclipse-temurin:21-jre-noble
RUN apt-get update \
    && apt-get -y upgrade --no-install-recommends \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r lojapp \
    && useradd -r -g lojapp lojapp
WORKDIR /app
COPY --from=build /app/target/lojapp-api-*.jar /app/app.jar
RUN chown lojapp:lojapp /app/app.jar
USER lojapp
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
