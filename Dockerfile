FROM eclipse-temurin:21-jre AS development
WORKDIR /app
COPY target/bsplay-backend-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre AS production
WORKDIR /app
RUN useradd --system --uid 10001 bsplay
COPY --from=build /app/target/bsplay-backend-*.jar app.jar
USER bsplay
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
