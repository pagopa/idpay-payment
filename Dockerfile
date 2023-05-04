#
# Build
#
FROM eclipse-temurin as buildtime

WORKDIR /build
COPY . .

RUN ./gradlew bootJar

#
# Docker RUNTIME
#
FROM eclipse-temurin:17-jre-alpine as runtime

VOLUME /tmp
WORKDIR /app

COPY --from=buildtime /build/target/*.jar /app/app.jar
# The agent is enabled at runtime via JAVA_TOOL_OPTIONS.
ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.4.11/applicationinsights-agent-3.4.11.jar /app/applicationinsights-agent.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]
