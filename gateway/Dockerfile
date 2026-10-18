FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY target/*.jar ./gateway.jar

EXPOSE 9090
ENTRYPOINT ["java", "-jar", "gateway.jar"]