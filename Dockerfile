# syntax=docker/dockerfile:1
# Multi-Stage-Build (Spring Boot 4 / Java 25). Layered-Extraction + JarLauncher
# (schnellerer Start, besseres Layer-Caching). Cross-Build:
#   docker buildx build --platform linux/amd64 ...
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle .
COPY settings.gradle .
RUN ./gradlew dependencies --no-daemon
COPY src/ src/
RUN ./gradlew clean bootJar --no-daemon
RUN java -Djarmode=tools -jar build/libs/*.jar extract --launcher --destination build/extracted

# runtime
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
# uid 1001 passend zur k8s-securityContext (runAsUser: 1001).
RUN addgroup -S spring && adduser -S -u 1001 -G spring spring
COPY --from=builder --chown=spring:spring /app/build/extracted/ ./
# Leere .env fuer springboot4-dotenv (Werte kommen zur Laufzeit aus Env-Vars) +
# Verzeichnis fuer den Pseudonym-Store (data/mappings.enc, im Deployment PVC).
RUN touch .env && mkdir -p /app/data && chown spring:spring .env /app/data
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "org.springframework.boot.loader.launch.JarLauncher"]
