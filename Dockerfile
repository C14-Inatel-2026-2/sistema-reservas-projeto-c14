FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /build

COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
RUN chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp dependency:go-offline
COPY backend/src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp package -DskipTests

FROM eclipse-temurin:21-jre-jammy
RUN groupadd --system app && useradd --system --gid app --home-dir /app app
WORKDIR /app
COPY --from=build /build/target/sistema-reservas-*.jar app.jar
COPY scripts/docker-entrypoint.sh docker-entrypoint.sh
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60.0"
USER app
EXPOSE 8080
ENTRYPOINT ["sh", "/app/docker-entrypoint.sh"]
