#
# Build
#
FROM eclipse-temurin:17-jdk-alpine as buildtime

WORKDIR /build
COPY . .

RUN ./gradlew bootJar

#
# Docker RUNTIME
#
FROM eclipse-temurin:17-jre-alpine as runtime

WORKDIR /app

COPY --from=buildtime /build/build/libs/*.jar /app/app.jar
# The agent is enabled at runtime via JAVA_TOOL_OPTIONS.
ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.4.11/applicationinsights-agent-3.4.11.jar /app/applicationinsights-agent.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]
