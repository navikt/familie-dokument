FROM navikt/java:17

COPY ./target/familie-dokument.jar "app.jar"
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Djava.awt.headless=true"
