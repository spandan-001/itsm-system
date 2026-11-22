FROM node:24-bookworm-slim AS frontend
WORKDIR /build
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
ARG VITE_SUPABASE_URL
ARG VITE_SUPABASE_PUBLISHABLE_KEY
RUN test -n "$VITE_SUPABASE_URL" && test -n "$VITE_SUPABASE_PUBLISHABLE_KEY" && npm run build

FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /build
COPY pom.xml ./
COPY src/ src/
COPY --from=frontend /build/dist/ src/main/resources/static/
RUN mvn -B -DskipTests package && cp target/*.jar /app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=backend /app.jar app.jar
ENV SPRING_PROFILES_ACTIVE=supabase
USER 10001
ENTRYPOINT ["java","-jar","app.jar"]
