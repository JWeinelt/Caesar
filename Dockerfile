# === Stage 1: Build ===
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Pom & Sources reinkopieren
COPY pom.xml .
COPY src ./src

# Build starten
RUN mvn -B package

# === Stage 2: Runtime ===
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Nur das Jar aus Stage 1 kopieren
COPY --from=build /build/target/*.jar /app/caesar.jar

EXPOSE 48000 48001 48002 48003 48004 48005

CMD ["java", "-jar", "/app/caesar.jar"]
