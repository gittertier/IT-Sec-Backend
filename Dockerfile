# syntax=docker/dockerfile:1
#
# Multi-Stage-Build fuer das Spring-Boot-4-Backend (Java 25).
# Build via Gradle-Wrapper (laedt das passende Gradle selbst), Runtime auf einem
# schlanken Temurin-JRE.
#
# WICHTIG: Immer fuer die Server-Architektur bauen:
#   docker buildx build --platform linux/amd64 ...

# ---------- Build ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Erst nur die Gradle-Metadaten kopieren -> Layer-Caching der Dependencies.
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# Dann den Quellcode und das Boot-Jar bauen (Tests werden im Image-Build
# uebersprungen; die laufen in CI / lokal).
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# ---------- Runtime ----------
# Falls der Tag "25-jre" mal nicht aufloest: auf "25-jdk" wechseln.
FROM eclipse-temurin:25-jre AS run
WORKDIR /app

# Unprivilegierten User anlegen.
RUN useradd -r -u 1001 -m -d /home/spring spring

# Das ausfuehrbare Boot-Jar (bootJar erzeugt nur dieses eine Jar in build/libs).
COPY --from=build --chown=spring:spring /app/build/libs/*.jar app.jar

# Die App liest per springboot4-dotenv eine .env im Arbeitsverzeichnis. Im
# Container gibt es keine (siehe .dockerignore) -> leere Datei anlegen, damit
# die Lib nicht stolpert. Alle Werte kommen zur Laufzeit aus den k8s-Env-Vars.
RUN touch .env && chown spring:spring .env

# Verzeichnis fuer den verschluesselten Pseudonym-Mapping-Store (pseudo.mapping.file
# = data/mappings.enc). Im Deployment wird hier ein emptyDir gemountet.
RUN mkdir -p /app/data && chown spring:spring /app/data

USER spring
EXPOSE 8080
# MaxRAMPercentage, damit die JVM das k8s-Memory-Limit respektiert.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
