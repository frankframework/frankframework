FROM eclipse-temurin:24.0.1_9-jre-alpine

COPY target/frankframework-console-webapp.war /app/frankframework-console-webapp.war

EXPOSE 8080

CMD ["java", \
	"--add-modules", "java.se", \
	"--add-exports", "java.base/jdk.internal.ref=ALL-UNNAMED", \
	"--add-opens", "java.base/java.lang=ALL-UNNAMED", \
	"--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED", \
	"--add-opens", "java.management/sun.management=ALL-UNNAMED", \
	"--add-opens", "jdk.management/com.sun.management.internal=ALL-UNNAMED", \
	"-jar", "/app/frankframework-console-webapp.war"]
