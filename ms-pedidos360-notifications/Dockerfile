FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /build
COPY ms-pedidos360-notifications/pom.xml .
COPY ms-pedidos360-notifications/src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl \
    && addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build --chown=app:app /build/target/*.jar app.jar
USER app:app
EXPOSE 8083
HEALTHCHECK --interval=10s --timeout=5s --retries=12 CMD curl -s -o /dev/null http://localhost:8083/api/notifications/history || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
