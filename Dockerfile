FROM eclipse-temurin:25
VOLUME /tmp
WORKDIR /app
COPY target/banking-application-0.0.1-SNAPSHOT.jar banking-application.jar
COPY target/classes/photo photo
ENTRYPOINT ["java", "-jar", "banking-application.jar"]