# Multi-stage build - compile backend
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy backend source and pom.xml
COPY backend/pom.xml ./backend/
COPY backend/src ./backend/src/

# Build the JAR
WORKDIR /app/backend
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder /app/backend/target/placement-backend-1.0.0-jar-with-dependencies.jar ./

# Expose port 8080
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/ || exit 1

# Run the application
CMD ["java", "-jar", "placement-backend-1.0.0-jar-with-dependencies.jar"]
