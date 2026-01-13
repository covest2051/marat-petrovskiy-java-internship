FROM eclipse-temurin:22-jdk-jammy

RUN apt-get update && apt-get install -y \
    curl \
    dnsutils \
    iputils-ping \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
ARG JAR_FILE=target/authentication-service-1.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
