FROM maven:3-eclipse-temurin-17 AS maven
COPY pom.xml .
COPY src /src
RUN MAVEN_OPTS="-Xmx3G -Xss2m" mvn clean package

FROM openjdk:17-jdk-alpine

COPY --from=maven target/obp-hola-app-*-SNAPSHOT.jar obp-hola.jar
COPY application.properties.docker application.properties
EXPOSE 8087
ENTRYPOINT ["java","-jar","/obp-hola.jar"]

