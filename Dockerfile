FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25-dev@sha256:84ab569435e122e838637287e754a53cd2173abf1a51600aa45a6ff7df498d79
COPY ./target/familie-dokument.jar "app.jar"

ENV TZ="Europe/Oslo"
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75 -Djava.awt.headless=true"

ENTRYPOINT [ "java", "-Djdk.tls.client.protocols=TLSv1.2", "-jar", "app.jar" ]