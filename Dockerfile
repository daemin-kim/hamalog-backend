FROM openjdk:21-jdk
WORKDIR /app
COPY *.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
