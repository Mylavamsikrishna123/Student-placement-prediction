#!/bin/bash
# Railway startup script for Student Placement System Backend

# Set environment variables (Railway provides these)
export DB_URL=${DB_URL:-"jdbc:mysql://localhost:3306/JAVAPROJECT"}
export DB_USER=${DB_USER:-"root"}
export DB_PASS=${DB_PASS:-"root"}

# Run the pre-built JAR
java -jar backend/target/placement-backend-1.0.0-jar-with-dependencies.jar
