# Build stage
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml
COPY backend/pom.xml ./pom.xml

# Download dependencies (cache layer)
RUN mvn dependency:resolve

# Copy source code
COPY backend/src ./src/

# Build the JAR
RUN mvn clean package -DskipTests -q

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder /app/target/placement-backend-1.0.0-jar-with-dependencies.jar app.jar

# Expose port 8080
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/ || exit 1

# Run the application
CMD ["java", "-jar", "app.jar"]
