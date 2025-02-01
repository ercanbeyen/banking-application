package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.dto.ContractDto;
import com.ercanbeyen.bankingapplication.entity.Customer;

public interface ContractService {
    ContractDto createContract(ContractDto request);
    ContractDto updateContract(String id, ContractDto request);
    ContractDto getContract(String id);
    void addCustomerToContract(String subject, Customer customer);
    String deleteContract(String id);
}
