FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY learning/target/learning-1.0.jar ./learning.jar

EXPOSE 8084
ENTRYPOINT ["java", "-jar", "learning.jar"]
