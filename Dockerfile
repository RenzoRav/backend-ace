FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle lombok.config ./
COPY src/ src/

RUN sed -i 's/\r$//' gradlew \
    && sh ./gradlew --no-daemon bootJar \
    && cp build/libs/*.jar /app.jar

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN groupadd --system app \
    && useradd --system --gid app --home-dir /app --shell /usr/sbin/nologin app

COPY --from=build --chown=app:app /app.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
