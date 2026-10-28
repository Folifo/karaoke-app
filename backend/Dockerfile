# Construit depuis la racine du dépôt (le contrat docs/openapi.yaml est embarqué dans l'application) :
#   docker build -f backend/Dockerfile .
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY backend/.mvn backend/.mvn
COPY backend/mvnw backend/pom.xml backend/
RUN cd backend && ./mvnw -B -q dependency:go-offline
COPY docs docs
COPY backend/src backend/src
RUN cd backend && ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 app
WORKDIR /app
COPY --from=build /workspace/backend/target/karaoke-backend-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
