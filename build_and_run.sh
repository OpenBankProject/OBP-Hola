#!/bin/bash
set -e

echo "Building OBP-Hola..."
mvn clean package -DskipTests

echo "Running OBP-Hola..."
java -jar target/obp-hola-app-0.0.29-SNAPSHOT.jar "$@"
