package com.ercanbeyen.bankingapplication.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;

@TestConfiguration
public class TestContainerConfig {
    @Bean
    @ServiceConnection
    MySQLContainer<?> mySQLContainer() {
        return new MySQLContainer<>("mysql:latest")
                .withDatabaseName("test-database")
                .withUsername("username")
                .withPassword("password");
    }

    @Bean
    @ServiceConnection
    CassandraContainer<?> cassandraContainer() {
        return new CassandraContainer<>("cassandra:latest")
                .withStartupAttempts(5);
    }

    @Bean
    @ServiceConnection
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>("redis:latest")
                .withExposedPorts(6379);
    }
}
