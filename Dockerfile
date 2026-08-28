# ──────────────────────────────────────────────────────────────
# Root Dockerfile for Spring Boot Backend Deployment
# ──────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY backend/pom.xml .
RUN mvn dependency:go-offline -B

COPY backend/src ./src
RUN mvn clean package -DskipTests -B

# ──────────────────────────────────────────────────────────────
# Stage 2: Lightweight Java 21 Runtime Environment
# ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

ENV PORT=8080
ENV JAVA_TOOL_OPTIONS="-Xms128m -Xmx384m -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=64m -XX:+UseSerialGC"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
