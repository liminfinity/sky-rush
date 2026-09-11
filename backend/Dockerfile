FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew
COPY config ./config
COPY src ./src
# Reuse the wrapper and dependency cache when source edits invalidate this layer.
RUN --mount=type=cache,id=skyrush-gradle,target=/root/.gradle,sharing=locked \
    ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S skyrush && adduser -S skyrush -G skyrush
COPY --from=build /app/build/libs/skyrush.jar app.jar
USER skyrush
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
