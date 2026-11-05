# syntax=docker/dockerfile:1

# ── Build: the project's own Maven wrapper, so the image is built with the same Maven as CI ──
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Dependencies first: this layer is reused until pom.xml changes; the cache mount keeps ~/.m2 between builds
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q -ntp dependency:go-offline
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q -ntp package -DskipTests \
    && cp target/*.jar app.jar \
    # Split the jar into layers (dependencies change rarely, the app often) for smaller image updates
    && java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# ── Run: JRE only, non-root ──
FROM eclipse-temurin:17-jre-jammy
RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --no-create-home app \
    && mkdir -p /app/uploads \
    && chown app:app /app/uploads
WORKDIR /app
COPY --from=build /build/extracted/dependencies/ ./
COPY --from=build /build/extracted/spring-boot-loader/ ./
COPY --from=build /build/extracted/snapshot-dependencies/ ./
COPY --from=build /build/extracted/application/ ./
USER app

# Size the heap from the container's memory limit (not the host's) and restart cleanly instead of limping on
# after an OutOfMemoryError; override with JAVA_TOOL_OPTIONS if needed
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
# 8080: the shop; 8081: management (health, Prometheus metrics) in the prod profile
EXPOSE 8080 8081
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
