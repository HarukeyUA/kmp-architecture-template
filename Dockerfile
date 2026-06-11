# Server image — package the runnable distribution into a small JRE base (ADR-0010: installDist → JRE
# image). Target-agnostic: runs on any container host (Railway, Fly, Cloud Run, k8s, …).
#
# Build the distribution first, then the image (the monorepo's Android/iOS modules need the Android
# SDK to configure, so the JVM artifact is built on the host/CI rather than inside this image):
#
#   ./gradlew :server:app:installDist
#   docker build -t kmp-server .
#   docker run --rm -p 8080:8080 --env-file .env kmp-server
#
# `installDist` writes a self-contained dir (bin/ launcher + lib/ jars) — no Gradle in the image.
FROM eclipse-temurin:21-jre

WORKDIR /app
COPY server/app/build/install/server/ /app/

# Config is env-driven (DATABASE_*, S3_*, APP_ENV=production); see .env.example. No secrets baked in.
# TZ is belt-and-braces only (all timestamp columns are TIMESTAMPTZ; the bin/server launcher also
# pins -Duser.timezone=UTC via applicationDefaultJvmArgs).
ENV SERVER_HOST=0.0.0.0 \
    SERVER_PORT=8080 \
    TZ=UTC
EXPOSE 8080

# /metrics (Prometheus) and /health are served by the app for scrape/liveness probes.
ENTRYPOINT ["/app/bin/server"]
