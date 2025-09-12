FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN mvn -B package

FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY --from=build /build/target/*.jar /app/caesar.jar

EXPOSE 48000 48001 48002 48003 48004 48005

CMD ["java", "-jar", "/app/caesar.jar"]
