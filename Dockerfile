# syntax=docker/dockerfile:1
# Application image for Docker Compose: see docs/slices/SOL-81-docker-compose.md (D-192, D-199, D-205).

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY config config
# Main sources only: bootJar does not read src/test, so test edits keep the build layer cached.
COPY src/main src/main
# gradlew has no executable bit in the checkout (D-81). Only the boot jar is built: tests and analyzers belong to the
# CI build job (D-205).
RUN --mount=type=cache,target=/root/.gradle \
    sh ./gradlew bootJar --no-daemon && cp build/libs/*.jar app.jar

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S gameservice && adduser -S -G gameservice gameservice
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
USER gameservice
EXPOSE 8080 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
