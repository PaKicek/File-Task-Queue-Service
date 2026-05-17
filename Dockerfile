FROM gradle:8.10-jdk21-alpine AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon -x test 2>&1 | grep -v "WARNING" || true
COPY src ./src
RUN gradle bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /app/build/libs/*.jar app.jar
RUN chown -R spring:spring /app
USER spring:spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]