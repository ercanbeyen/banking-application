package com.ercanbeyen.bankingapplication.factory;

import com.ercanbeyen.bankingapplication.dto.CustomerAgreementDto;
import com.ercanbeyen.bankingapplication.entity.Agreement;
import com.ercanbeyen.bankingapplication.entity.Customer;
import com.ercanbeyen.bankingapplication.entity.CustomerAgreement;
import com.ercanbeyen.bankingapplication.util.TimeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MockCustomerAgreementFactory {
    public static List<CustomerAgreementDto> generateMockCustomerAgreementDtos() {
        List<CustomerAgreement> customerAgreements = generateMockCustomerAgreements();
        List<CustomerAgreementDto> customerAgreementDtos = new ArrayList<>();

        customerAgreements.forEach(customerAgreement -> {
            CustomerAgreementDto customerAgreementDto = new CustomerAgreementDto(
                    UUID.randomUUID().toString(), customerAgreement.getCustomer().getNationalId(), customerAgreement.getAgreement().getTitle(), TimeUtil.getTurkeyDateTime());
            customerAgreementDtos.add(customerAgreementDto);
        });

        return customerAgreementDtos;
    }

    private static List<CustomerAgreement> generateMockCustomerAgreements() {
        Agreement agreement = MockAgreementFactory.getMockAgreement();
        Customer customer = MockCustomerFactory.generateMockCustomers().getFirst();

        CustomerAgreement customerAgreement = new CustomerAgreement();
        customerAgreement.setId(UUID.randomUUID().toString());
        customerAgreement.setAgreement(agreement);
        customerAgreement.setCustomer(customer);
        customerAgreement.setApprovedAt(TimeUtil.getTurkeyDateTime());

        return List.of(customerAgreement);
    }
}
