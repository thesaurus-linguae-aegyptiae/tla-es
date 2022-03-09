FROM gradle:7.1-jdk11 AS build

COPY --chown=gradle:gradle . /home/gradle/tla
WORKDIR /home/gradle/tla

RUN gradle bootJar --no-daemon && \
    mv build/libs/*.jar bin/run/tla-backend.jar


FROM openjdk:11.0.10-jdk-buster

RUN mkdir /app
WORKDIR /app/
COPY --from=build /home/gradle/tla/bin/run/ /app/

EXPOSE 9000
EXPOSE 9201
ENTRYPOINT ["sh", "/app/entrypoint.sh"]
