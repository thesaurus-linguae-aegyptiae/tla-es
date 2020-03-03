FROM gradle:6.2.1-jdk11 AS build

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle bootJar --no-daemon


FROM openjdk:11-jdk

EXPOSE 8090
ARG es_port
ENV ES_PORT ${es_port}

RUN mkdir /app
WORKDIR /app/
COPY --from=build /home/gradle/src/build/libs/*.jar /app/spring-boot-application.jar

ENTRYPOINT ["java", "-jar", "/app/spring-boot-application.jar"]
