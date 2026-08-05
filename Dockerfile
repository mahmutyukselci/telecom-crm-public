FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

ARG SERVICE_NAME

# Copy the target directory into a temporary location
COPY ${SERVICE_NAME}/target/ /tmp/target/

# Find the main JAR file (excluding .original, -sources, and -javadoc)
# regardless of version, and copy it as app.jar
RUN find /tmp/target -name "*.jar" \
    ! -name "*.original" \
    ! -name "*-sources.jar" \
    ! -name "*-javadoc.jar" \
    -exec cp {} /app/app.jar \;

ENTRYPOINT ["java", "-jar", "/app/app.jar"]