FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /src
COPY mvnw .
COPY .mvn .mvn
COPY ai-service ai-service
RUN --mount=type=cache,target=/root/.m2,sharing=locked ./mvnw -q -f ai-service/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN adduser -S -u 1001 app
USER app
WORKDIR /app
COPY --from=build /src/ai-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
