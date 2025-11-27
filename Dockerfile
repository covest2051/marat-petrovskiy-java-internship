FROM eclipse-temurin:22-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN ./mvnw -q clean package -DskipTests || mvn -q clean package -DskipTests

FROM eclipse-temurin:22-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/authentication-service-*.jar app.jar
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
