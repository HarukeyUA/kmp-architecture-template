# syntax=docker/dockerfile:1.28
# Server image: builds the distribution inside the image and runs it behind a minimal JRE.
# Target-agnostic: runs on any container host. From the repo root:
#
#   docker build -t server .
#   docker run --rm -p 8080:8080 --env-file .env server
#
# Gradle configures every project, client modules included, but builds only what :server:app needs.
# No Android SDK or Kotlin/Native host support is required for that.

FROM gradle:9.8-jdk25 AS build
WORKDIR /workspace
COPY --chown=gradle: . .
RUN ./gradlew --no-daemon :server:app:installDist

FROM eclipse-temurin:25.0.5_8-jre
WORKDIR /app
# The process owns nothing on disk: the distribution is read-only and root-owned, and the JVM
# writes only to stdout, so an unprivileged user loses nothing.
RUN useradd --system --uid 10001 --no-create-home --shell /usr/sbin/nologin server
# JVM flags (heap envelope, exit on OOM, UTC) are baked into the launcher by the
# `convention.server.app` Gradle plugin; JAVA_OPTS still appends to them if a deploy needs more.
COPY --from=build /workspace/server/app/build/install/server ./

# Config is env-driven and required values fail the boot (see .env.example); no secrets baked in.
ENV SERVER_HOST=0.0.0.0 \
    SERVER_PORT=8080 \
    TZ=UTC
EXPOSE 8080
USER server
# The start script `exec`s java, so SIGTERM from a redeploy reaches the JVM directly.
CMD ["./bin/server"]
