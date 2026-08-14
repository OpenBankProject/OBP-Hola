FROM maven:3-eclipse-temurin-17 AS maven
COPY pom.xml .
COPY src /src
RUN MAVEN_OPTS="-Xmx3G -Xss2m" mvn clean package

FROM eclipse-temurin:17-jre-alpine

COPY --from=maven target/obp-hola-app-*-SNAPSHOT.jar obp-hola.jar
COPY application.properties.docker application.properties
ENTRYPOINT ["java","-jar","/obp-hola.jar"]

