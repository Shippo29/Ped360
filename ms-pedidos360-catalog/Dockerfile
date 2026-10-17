FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /build
COPY ms-pedidos360-catalog/pom.xml .
COPY ms-pedidos360-catalog/src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl \
    && addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build --chown=app:app /build/target/*.jar app.jar
USER app:app
EXPOSE 8081
HEALTHCHECK --interval=10s --timeout=5s --retries=12 CMD curl -s -o /dev/null http://localhost:8081/ || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
