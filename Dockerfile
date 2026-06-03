# ---- Stage 1: Build Stage ---------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build

# 1. Copia o pom.xml e o código fonte de uma vez só
COPY pom.xml .
COPY src ./src

# 2. Compila direto (ele vai baixar as dependências direto aqui, muito mais rápido)
RUN mvn clean package -DskipTests -B -q

# ---- Stage 2: Runtime Stage -------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime

# Create a non-root group and user for security compliance
RUN groupadd --gid 1001 appgroup && \
    useradd --uid 1001 --gid appgroup --shell /bin/false appuser

# Instala o wget como root para o HEALTHCHECK funcionar na imagem enxuta
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy only the compiled monolith JAR from the builder stage
COPY --from=builder /build/target/*.jar app.jar

# Set ownership of the application file to the non-root user
RUN chown appuser:appgroup app.jar

# Switch to the non-root user
USER appuser

# Expose the port the Spring Boot application runs on INTERNALLY
EXPOSE 8080

# Health check to verify the monolith is active (checking internal port 8080)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/ping || exit 1

# Production JVM optimizations
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]