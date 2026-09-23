FROM eclipse-temurin:25

VOLUME /tmp

WORKDIR /app

COPY target/banking-application-0.0.1-SNAPSHOT.jar banking-application.jar
COPY target/classes/photo photo

COPY ./start.sh start.sh
COPY ./entrypoint.sh entrypoint.sh

RUN chmod +x start.sh && chmod +x entrypoint.sh

ENTRYPOINT ["./entrypoint.sh"]