#!/bin/bash

echo "Start::Docker services are initializing..."

docker-compose up -d --build

echo "Start::Containers are being starting, waiting..."
sleep 10

echo "Start::Spring Boot application logs:"
docker-compose logs -f app