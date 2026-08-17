FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25-dev@sha256:34ede3d42ed192f5c13c9c3ad5917afa4c9998136876d35ce83225c6cfdebfb0
COPY ./target/familie-dokument.jar "app.jar"

ENV TZ="Europe/Oslo"
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75 -Djava.awt.headless=true"

ENTRYPOINT [ "java", "-Djdk.tls.client.protocols=TLSv1.2", "-jar", "app.jar" ]