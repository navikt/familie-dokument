FROM navikt/java:11

COPY ./target/familie-dokument.jar "app.jar"
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
