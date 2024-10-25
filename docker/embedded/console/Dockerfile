FROM eclipse-temurin:23.0.1_11-jre-alpine

COPY target/frankframework-console-webapp.war /app/frankframework-console-webapp.war

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/frankframework-console-webapp.war"]
