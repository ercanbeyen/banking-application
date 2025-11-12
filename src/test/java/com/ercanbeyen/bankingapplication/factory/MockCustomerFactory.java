package com.ercanbeyen.bankingapplication.factory;

import com.ercanbeyen.bankingapplication.constant.enums.Gender;
import com.ercanbeyen.bankingapplication.dto.CustomerDto;
import com.ercanbeyen.bankingapplication.entity.Agreement;
import com.ercanbeyen.bankingapplication.entity.CashFlowCalendar;
import com.ercanbeyen.bankingapplication.entity.Customer;
import com.ercanbeyen.bankingapplication.entity.CustomerAgreement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MockCustomerFactory {
    private MockCustomerFactory() {}

    public static List<Customer> generateMockCustomers() {
        List<CashFlowCalendar> cashFlowCalendars = MockCashFlowCalendarFactory.generateMockCashFlowCalendars();
        Agreement agreement = MockAgreementFactory.getMockAgreement();

        CustomerAgreement customerAgreement = new CustomerAgreement();
        customerAgreement.setAgreement(agreement);
        customerAgreement.setApprovedAt(LocalDateTime.now());

        List<Customer> customers = new ArrayList<>();
        List<CustomerDto> requests = generateMockCustomerDtos();

        addCustomerToList(requests.getFirst(), cashFlowCalendars.getFirst(), Set.of(customerAgreement), customers);
        addCustomerToList(requests.get(1), cashFlowCalendars.get(1), Set.of(customerAgreement), customers);
        addCustomerToList(requests.getLast(), cashFlowCalendars.getLast(), Set.of(customerAgreement), customers);

        return customers;
    }

    public static List<CustomerDto> generateMockCustomerDtos() {
        int id = 1;

        CustomerDto customerDto1 = new CustomerDto();
        customerDto1.setId(id);
        customerDto1.setName("Test-Name1");
        customerDto1.setSurname("Test-Surname1");
        customerDto1.setNationalId("12345678911");
        customerDto1.setEmail("test1@email.com");
        customerDto1.setPhoneNumber("+905328465701");
        customerDto1.setGender(Gender.MALE);
        customerDto1.setBirthDate(LocalDate.of(1980, 8, 15));
        customerDto1.setAddresses(new ArrayList<>());

        id++;

        CustomerDto customerDto2 = new CustomerDto();
        customerDto2.setId(id);
        customerDto2.setName("Test-Name2");
        customerDto2.setSurname("Test-Surname2");
        customerDto2.setNationalId("12345678912");
        customerDto2.setEmail("test2@email.com");
        customerDto2.setPhoneNumber("+905328465702");
        customerDto2.setGender(Gender.FEMALE);
        customerDto2.setBirthDate(LocalDate.of(1985, 4, 6));
        customerDto2.setAddresses(new ArrayList<>());

        id++;

        CustomerDto customerDto3 = new CustomerDto();
        customerDto3.setId(id);
        customerDto3.setName("Test-Name3");
        customerDto3.setSurname("Test-Surname3");
        customerDto3.setNationalId("12345678913");
        customerDto3.setEmail("test3@email.com");
        customerDto3.setPhoneNumber("+905328465703");
        customerDto3.setGender(Gender.FEMALE);
        customerDto3.setBirthDate(LocalDate.of(1993, 2, 20));
        customerDto3.setAddresses(new ArrayList<>());

        return List.of(customerDto1, customerDto2, customerDto3);
    }

    private static void addCustomerToList(CustomerDto request, CashFlowCalendar cashFlowCalendar, Set<CustomerAgreement> agreements, List<Customer> customers) {
        Customer customer = new Customer();
        customer.setId(request.getId());
        customer.setName(request.getName());
        customer.setSurname(request.getSurname());
        customer.setNationalId(request.getNationalId());
        customer.setEmail(request.getEmail());
        customer.setPhoneNumber(request.getPhoneNumber());
        customer.setGender(request.getGender());
        customer.setBirthDate(request.getBirthDate());
        customer.setAddresses(request.getAddresses());
        customer.setCashFlowCalendar(cashFlowCalendar);
        customer.setAgreements(agreements);

        customers.add(customer);
    }
}
