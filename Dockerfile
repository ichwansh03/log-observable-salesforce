# syntax=docker/dockerfile:1
# Build stage
FROM eclipse-temurin:25-jdk-jammy AS build
WORKDIR /app

# Copy the maven wrapper and pom.xml first to leverage Docker cache
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Download dependencies with retry and cache mount
RUN --mount=type=cache,target=/root/.m2/repository \
    for i in 1 2 3; do \
      ./mvnw dependency:go-offline -B && break || \
      echo "Attempt $i failed, retrying..." && sleep 5; \
    done

# Copy the source code and build
COPY src ./src
RUN --mount=type=cache,target=/root/.m2/repository \
    ./mvnw clean package -DskipTests -B

# Run stage
FROM eclipse-temurin:25-jre-jammy
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
