FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25-dev@sha256:c446c8efb1b6c256725a9e8427663538578697ab53dda5a5ce85315fe3b873e7
COPY ./target/familie-dokument.jar "app.jar"

ENV TZ="Europe/Oslo"
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75 -Djava.awt.headless=true"

ENTRYPOINT [ "java", "-Djdk.tls.client.protocols=TLSv1.2", "-jar", "app.jar" ]