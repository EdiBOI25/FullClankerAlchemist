FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /src
COPY mvnw .
COPY .mvn .mvn
COPY alchemist-service alchemist-service
RUN --mount=type=cache,target=/root/.m2,sharing=locked ./mvnw -q -f alchemist-service/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN adduser -S -u 1001 app && mkdir /data && chown app /data
USER app
WORKDIR /app
ENV SPRING_DATASOURCE_URL=jdbc:sqlite:/data/alchemist.db?foreign_keys=true \
    AI_SERVICE_URL=http://ai-service:2501
COPY --from=build /src/alchemist-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
