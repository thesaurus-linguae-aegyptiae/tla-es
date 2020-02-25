FROM openjdk:11-jdk

ARG es_port
ENV ES_PORT ${es_port}

COPY . /app
WORKDIR /app/

EXPOSE 8090

ENTRYPOINT ["./gradlew", "bootRun"]

