FROM gradle:6.4-jdk11 AS build

COPY --chown=gradle:gradle . /home/gradle/tla
WORKDIR /home/gradle/tla

ARG es_port
ARG es_host
ARG sample_url
ENV ES_PORT ${es_port}
ENV ES_HOST ${es_host}
ENV SAMPLE_URL ${sample_url}

RUN curl "http:/${ES_HOST}:${ES_PORT}" && gradle test populate bootJar --no-daemon


FROM openjdk:11-jre-slim

RUN mkdir /app
WORKDIR /app/
COPY --from=build /home/gradle/tla/build/libs/*.jar /app/tla-web-backend.jar

ARG es_port
ARG es_host
ENV ES_PORT ${es_port}
ENV ES_HOST ${es_host}
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app/tla-web-backend.jar"]
