# Stage 1: Build the application with Maven & OpenJDK 17
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and resolve dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and build jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Lightweight runtime container
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy executable jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Dynamic port binding for cloud platforms (defaults to 8080)
ENV PORT=8080
EXPOSE 8080

# JVM options configured for Render Free (512 MB memory limit)
# -XX:+UseSerialGC: minimizes GC memory overhead on low-memory containers
# -Xss512k: halves default per-thread stack footprint
# -XX:MaxRAMPercentage=75.0: dynamically limits heap to ~384MB on a 512MB host
# Allows override via JAVA_OPTS environment variable if needed
ENV JAVA_OPTS="-XX:+UseSerialGC -Xss512k -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Launch application using configured JVM flags
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

