FROM navikt/java:11

ENV JAVA_OPTS="-Dhttps.protocols=TLSv1,TLSv1.1,TLSv1.2"

COPY ./target/familie-dokument.jar "app.jar"
