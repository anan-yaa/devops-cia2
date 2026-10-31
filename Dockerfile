# The pipeline builds the JAR with Maven first (mvn package), then this image just runs it.
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Run as a non-root user
RUN addgroup -S app && adduser -S app -G app
COPY target/app.jar app.jar
RUN chown app:app app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
