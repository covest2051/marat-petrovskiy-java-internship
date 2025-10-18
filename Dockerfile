FROM eclipse-temurin:22-jdk-alpine
WORKDIR /app
COPY target/java-internship-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]
