#!/bin/bash

echo "Entrypoint::Containers are starting..."

sleep 5

echo "Entrypoint::Spring Boot application is starting..."

exec java -jar /app/banking-application.jar
