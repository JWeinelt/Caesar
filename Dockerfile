FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY target/caesar.jar /app/caesar.jar

EXPOSE 48000 48001 48002 48003 48004 48005

CMD ["java", "-jar", "/app/caesar.jar"]
