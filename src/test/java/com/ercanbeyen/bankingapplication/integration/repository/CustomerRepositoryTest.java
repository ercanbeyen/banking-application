package com.ercanbeyen.bankingapplication.integration.repository;

import com.ercanbeyen.bankingapplication.constant.enums.Gender;
import com.ercanbeyen.bankingapplication.entity.Customer;
import com.ercanbeyen.bankingapplication.repository.CustomerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Customer Repository Integration Test")
class CustomerRepositoryTest {
    @Container
    @ServiceConnection
    private static final MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:latest");
    @Autowired
    private CustomerRepository customerRepository;

    @DynamicPropertySource
    static void registerMySQLProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mySQLContainer::getUsername);
        registry.add("spring.datasource.password", mySQLContainer::getPassword);

        mySQLContainer.start();
    }

    @Test
    @DisplayName("Happy path test: Save customer case")
    void givenCustomerEntity_whenSaveCustomer_thenCustomerIsPersisted() {
        Customer expected = new Customer();
        expected.setName("Test-Name1");
        expected.setSurname("Test-Surname1");
        expected.setNationalId("12345678911");
        expected.setEmail("test1@email.com");
        expected.setPhoneNumber("+905328465701");
        expected.setGender(Gender.MALE);
        expected.setBirthDate(LocalDate.of(1994, Month.JUNE, 28));

        customerRepository.save(expected);

        Optional<Customer> actual = customerRepository.findById(1);
        assertTrue(actual.isPresent());
        assertEquals(expected.getName(), actual.get().getName());
    }
}
