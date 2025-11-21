# Multi-stage build for Student Placement System
# Backend: Pure Java HttpServer on port 8080
# Frontend: Static files served from port 5500

# Stage 1: Build backend JAR
FROM maven:3.9-eclipse-temurin-17 AS backend-builder

WORKDIR /build

# Copy backend pom.xml
COPY backend/pom.xml ./pom.xml

# Download dependencies (cache layer)
RUN mvn dependency:resolve -q || true

# Copy backend source
COPY backend/src ./src/

# Build JAR with dependencies
RUN mvn clean package -DskipTests -q

# Stage 2: Runtime environment
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install Python and pip for frontend server
RUN apk add --no-cache python3 py3-pip

# Copy built JAR from builder stage
COPY --from=backend-builder /build/target/placement-backend-1.0.0-jar-with-dependencies.jar ./app.jar

# Copy frontend files
COPY ui/ ./ui/

# Expose ports
EXPOSE 8080 5500

# Create startup script
RUN echo '#!/bin/sh\n\
# Start backend on port 8080\n\
java -jar /app/app.jar &\n\
\n\
# Wait for backend to start\n\
sleep 2\n\
\n\
# Start frontend HTTP server on port 5500\n\
cd /app/ui\n\
python3 -m http.server 5500 --directory /app/ui\n\
' > /app/start.sh && chmod +x /app/start.sh

# Health check for backend
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/api/health || exit 1

# Run startup script
CMD ["/app/start.sh"]
