FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY target/*.jar ./notification.jar

EXPOSE 8086
ENTRYPOINT ["java", "-jar", "notification.jar"]