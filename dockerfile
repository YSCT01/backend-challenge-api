# JAVA 17 as image
FROM openjdk:17-jdk-slim

# Work directory
WORKDIR /app

#Copy JAR to container
COPY target/weathermicroservice-0.0.1-SNAPSHOT.jar app.jar

# PORT 8080
EXPOSE 8080

# Execute command
ENTRYPOINT ["java", "-jar", "app.jar"]