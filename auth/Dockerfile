FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY target/*.jar ./auth.jar

EXPOSE 8888
ENTRYPOINT ["java", "-jar", "auth.jar"]