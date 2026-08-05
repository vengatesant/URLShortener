# Combined single-app build for free-tier hosting (Render, Fly, Koyeb, etc.): the frontend is
# built and embedded as Spring Boot static resources, so one process serves both the API and
# the SPA from the same origin. Local dev keeps using docker-compose.yml + the per-service
# Dockerfiles instead — this one is for a single deployable artifact.

FROM node:20-alpine AS frontend-build
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ .
RUN npm run build

FROM maven:3.9-eclipse-temurin-21 AS backend-build
WORKDIR /app
COPY backend/pom.xml .
RUN mvn -B dependency:go-offline
COPY backend/src ./src
COPY --from=frontend-build /app/dist ./src/main/resources/static
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=backend-build /app/target/url-shortener-backend.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
