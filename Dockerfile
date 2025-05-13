FROM openjdk:21-jdk
WORKDIR /app
COPY build/libs/*.jar app.jarß
ENTRYPOINT ["java", "-jar", "/app/Hamalog.jar"]
