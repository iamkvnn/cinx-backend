FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY target/*.jar ./eureka.jar

EXPOSE 8761
ENTRYPOINT ["java", "-jar", "eureka.jar"]