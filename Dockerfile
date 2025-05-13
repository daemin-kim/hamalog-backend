FROM openjdk:21-jdk
WORKDIR /app
COPY Hamalog-0.0.1-SNAPSHOT.jar Hamalog-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java", "-jar", "Hamalog-0.0.1-SNAPSHOT.jar"]
