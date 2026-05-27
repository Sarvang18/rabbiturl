# ─── Stage 1: Build ───────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper and pom first (layer caching — dependencies only
# re-download when pom.xml changes, not on every code change)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Now copy source and build
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ─── Stage 2: Runtime ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: do not run as root
RUN addgroup -S rabbiturl && adduser -S rabbiturl -G rabbiturl
WORKDIR /app

# Copy only the fat JAR from the build stage
COPY --from=builder /app/target/*.jar app.jar

# Set ownership
RUN chown rabbiturl:rabbiturl app.jar
USER rabbiturl

# Expose app port
EXPOSE 8080

# JVM tuning for containers: respect cgroup memory limits,
# use G1GC which performs well in containerised environments
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
