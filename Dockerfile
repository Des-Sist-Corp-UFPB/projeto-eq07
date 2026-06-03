# =============================================================================
# Monolith Dockerfile — Multi-stage build (Maven/Spring Boot)
# =============================================================================
# This Dockerfile compiles the Maven project and runs the single output JAR:
#   1. Build Stage (maven): Compiles the Java/Spring Boot application.
#   2. Runtime Stage (jre): Minimal JRE 21 execution environment as a non-root user.
# =============================================================================

# ---- Stage 1: Build Stage ---------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy pom.xml first to leverage Docker layer caching for Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# Copy the backend source code (including classpath resources under src/main/resources/public)
COPY src ./src

# Compile the Maven package (skipping tests since CI runs/skips them)
RUN mvn clean package -DskipTests -B -q

# ---- Stage 2: Runtime Stage -------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime

# Create a non-root group and user for security compliance
RUN groupadd --gid 1001 appgroup && \
    useradd --uid 1001 --gid appgroup --shell /bin/false appuser

WORKDIR /app

# Copy only the compiled monolith JAR from the builder stage
COPY --from=builder /build/target/*.jar app.jar

# Set ownership of the application file to the non-root user
RUN chown appuser:appgroup app.jar

# Switch to the non-root user
USER appuser

# Expose the port the Spring Boot application runs on (container port 8080)
EXPOSE 8080

# Health check to verify the monolith is active (checking '/ping')
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/ping || exit 1

# Production JVM optimizations
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]
