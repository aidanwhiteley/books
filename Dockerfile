
FROM openjdk:8-jdk-alpine
LABEL maintainer="github@aidanwhiteley.com"
VOLUME /tmp
COPY target/*.jar app.jar
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar"]