#!/bin/sh

${JAVA_HOME}/bin/java -Djava.util.logging.config.file=./logging.properties -jar target/koskino-1.0.0-SNAPSHOT.jar --port 40001
