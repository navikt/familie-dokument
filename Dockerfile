FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25-dev@sha256:b3752229abf50c79d80cc933e9c09d38151bfd24467bea6b94bc1702e5b1e6ef
COPY ./target/familie-dokument.jar "app.jar"

ENV TZ="Europe/Oslo"
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75 -Djava.awt.headless=true"

ENTRYPOINT [ "java", "-Djdk.tls.client.protocols=TLSv1.2", "-jar", "app.jar" ]