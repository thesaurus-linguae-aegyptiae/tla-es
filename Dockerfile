FROM gradle:6.3-jdk11 AS build

COPY --chown=gradle:gradle . /home/gradle/tla
WORKDIR /home/gradle/tla

RUN gradle bootJar --no-daemon && \
    mv build/libs/*.jar bin/run/tla-backend.jar


FROM openjdk:16-jdk-alpine3.12

RUN mkdir /app
WORKDIR /app/
COPY --from=build /home/gradle/tla/bin/run/ /app/

EXPOSE 8090
ENTRYPOINT ["sh", "/app/entrypoint.sh"]
