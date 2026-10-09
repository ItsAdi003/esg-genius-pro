# Build stage
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src src
RUN ./mvnw package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --home-dir /app app

COPY --from=build /app/target/esgenius-backend-*.jar app.jar

RUN mkdir -p /app/uploads && chown -R app:app /app

USER app

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
