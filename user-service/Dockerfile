FROM eclipse-temurin:22-jdk-jammy
WORKDIR /app
ARG JAR_FILE=target/user-service-1.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
