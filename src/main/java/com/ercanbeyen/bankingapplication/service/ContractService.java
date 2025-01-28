package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.dto.ContractDto;

public interface ContractService {
    ContractDto createContract(ContractDto request);
    ContractDto updateContract(String id, ContractDto request);
    ContractDto getContract(String id);
    String deleteContract(String id);
}
