# ─────────────────────────────────────────
# Stage 1: Build with Maven + JDK 17
# ─────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper and pom first (layer cache for deps)
COPY pom.xml .

# Download dependencies (cached layer — only re-runs if pom.xml changes)
RUN apk add --no-cache maven && \
    mvn dependency:go-offline -B

# Copy source and build the fat JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# ─────────────────────────────────────────
# Stage 2: Lean runtime image (JRE only)
# ─────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the built JAR from builder stage
COPY --from=builder /build/target/otp-service-1.0.0.jar app.jar

# Set ownership
RUN chown appuser:appgroup app.jar

USER appuser

# Expose Spring Boot port
EXPOSE 8080

# JVM tuning for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
