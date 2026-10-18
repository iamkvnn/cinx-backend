FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY target/*.jar ./user.jar

EXPOSE 8089
ENTRYPOINT ["java", "-jar", "user.jar"]