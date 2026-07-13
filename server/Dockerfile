# ---- Build stage ----
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
# The executable bit on gradlew can get lost when the build context comes
# from a Windows host checkout — set it explicitly rather than relying on
# whatever the host filesystem preserved.
RUN chmod +x gradlew && ./gradlew --version

COPY src src
RUN ./gradlew installDist --no-daemon

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# eclipse-temurin's JRE image doesn't reliably ship an up-to-date CA bundle,
# which can make outbound HTTPS calls (Open-Meteo) fail a TLS handshake
# immediately — install/refresh it explicitly rather than rely on the base
# image having it.
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates \
    && update-ca-certificates \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/build/install/sunset-chasers-server /app

# No database, only a 30-min in-memory weather cache — a modest heap fits
# comfortably inside Render's free-tier 512MB RAM limit.
ENV JAVA_OPTS="-Xmx400m -XX:MaxMetaspaceSize=128m"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -cp '/app/lib/*' com.sunsetchasers.ApplicationKt"]
