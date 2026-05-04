FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21-dev@sha256:e76412a95c897028d18fa20c73065fd4975f0fe6c7cdc07df09af43597ce9805

COPY ./target/familie-dokument.jar "app.jar"

ENV TZ="Europe/Oslo"
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75 -Djava.awt.headless=true"

ENTRYPOINT [ "java", "-Djdk.tls.client.protocols=TLSv1.2", "-jar", "app.jar" ]