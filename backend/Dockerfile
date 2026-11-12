# syntax=docker/dockerfile:1

# ============================================================
# Stage 1: BUILD - compiles and packages the app with Maven
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy only pom.xml first so dependency downloads are cached
# as long as the pom does not change
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q dependency:go-offline

# Copy the sources and package the executable jar.
# Tests are run by the CI pipeline (mvn test), not during the image build.
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q clean package -DskipTests \
    && cp target/*.jar /build/app.jar

# ============================================================
# Stage 2: RUNTIME - JRE only, no Maven, no sources, no JDK
# ============================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Run as a non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app
COPY --from=build --chown=spring:spring /build/app.jar app.jar

EXPOSE 8080

# NO credentials in this image.
# The datasource URL, username and password are injected at runtime:
#   - Docker Compose : env_file: .env  (the .env file is NOT versioned)
#   - Kubernetes     : ConfigMap + Secret
# Spring Boot maps these variables automatically:
#   SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME,
#   SPRING_DATASOURCE_PASSWORD, SPRING_JPA_HIBERNATE_DDL_AUTO ...

# Container-aware JVM memory settings; extra flags can be passed via JAVA_OPTS
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

# sh -c + exec lets JAVA_OPTS expand while keeping java as PID 1 (clean SIGTERM handling)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]