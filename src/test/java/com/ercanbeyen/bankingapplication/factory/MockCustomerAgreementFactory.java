package com.ercanbeyen.bankingapplication.factory;

import com.ercanbeyen.bankingapplication.dto.CustomerAgreementDto;
import com.ercanbeyen.bankingapplication.entity.Agreement;
import com.ercanbeyen.bankingapplication.entity.Customer;
import com.ercanbeyen.bankingapplication.entity.CustomerAgreement;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MockCustomerAgreementFactory {
    public static List<CustomerAgreement> generateMockCustomerAgreements() {
        Agreement agreement = MockAgreementFactory.getMockAgreement();
        Customer customer = MockCustomerFactory.generateMockCustomers().getFirst();

        CustomerAgreement customerAgreement = new CustomerAgreement();
        customerAgreement.setId(UUID.randomUUID().toString());
        customerAgreement.setAgreement(agreement);
        customerAgreement.setCustomer(customer);
        customerAgreement.setApprovedAt(LocalDateTime.now());

        return List.of(customerAgreement);
    }

    public static List<CustomerAgreementDto> generateMockCustomerAgreementDtos() {
        List<CustomerAgreement> customerAgreements = generateMockCustomerAgreements();
        List<CustomerAgreementDto> customerAgreementDtos = new ArrayList<>();

        customerAgreements.forEach(customerAgreement -> {
            CustomerAgreementDto customerAgreementDto = new CustomerAgreementDto(
                    UUID.randomUUID().toString(), customerAgreement.getCustomer().getNationalId(), customerAgreement.getAgreement().getTitle(), LocalDateTime.now());
            customerAgreementDtos.add(customerAgreementDto);
        });

        return customerAgreementDtos;
    }
}
