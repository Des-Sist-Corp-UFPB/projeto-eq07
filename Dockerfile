# =============================================================================
# Production Dockerfile — Multi-stage build
# =============================================================================
# This Dockerfile uses a multi-stage build process to optimize image size and security:
#   1. Estágio de Build (builder): Compiles the code using Maven and JRE/JDK 21.
#   2. Estágio de Execução (runtime): Minimal JRE 21 image with a non-root user.
# =============================================================================

# ---- Stage 1: Build ---------------------------------------------------------
# We use JDK 21 to match the project's Java version (pom.xml).
# For Java 17, change this tag to 'maven:3.9.9-eclipse-temurin-17'
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy pom.xml first to leverage Docker layer caching for Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# Copy the source code and compile the application (skipping tests as they run in CI)
COPY src ./src
RUN mvn clean package -DskipTests -B -q

# ---- Stage 2: Runtime -------------------------------------------------------
# Minimal JRE runtime environment for production
# For Java 17, change this tag to 'eclipse-temurin:17-jre-jammy'
FROM eclipse-temurin:21-jre-jammy AS runtime

# Create a non-root group and user for security compliance
RUN groupadd --gid 1001 appgroup && \
    useradd --uid 1001 --gid appgroup --shell /bin/false appuser

WORKDIR /app

# Copy only the compiled JAR from the builder stage
COPY --from=builder /build/target/*.jar app.jar

# Set ownership of the application file to the non-root user
RUN chown appuser:appgroup app.jar

# Switch to the non-root user
USER appuser

# Expose the standard port the application runs on
EXPOSE 8080

# Health check using the Spring Boot Actuator endpoint (matches standard practice)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Production JVM optimizations:
#   -XX:+UseContainerSupport: respects Docker memory limits
#   -XX:MaxRAMPercentage=75: allocates up to 75% of container RAM to the JVM heap
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Dspring.profiles.active=prod", \
    "-jar", "app.jar"]
