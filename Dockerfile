# ── Widget: the embeddable recommendations script (widget/, React + TypeScript) ──
FROM node:26.13.0-alpine AS widget
WORKDIR /build/widget
# Dependencies first: reused until the lockfile changes; the cache mount keeps npm's download cache
COPY widget/package.json widget/package-lock.json widget/.npmrc ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund
COPY widget/ ./
# Writes ../src/main/resources/static/widget/ (store-widget.js, demo.html); tests run in CI, not here
RUN npm run build

# ── Build: the project's own Maven wrapper, so the image is built with the same Maven as CI ──
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Dependencies first: this layer is reused until pom.xml changes; the cache mount keeps ~/.m2 between builds
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q -ntp dependency:go-offline
COPY src/ src/
# The built widget goes into the jar's static files: served at /widget/store-widget.js
COPY --from=widget /build/src/main/resources/static/widget/ src/main/resources/static/widget/
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
