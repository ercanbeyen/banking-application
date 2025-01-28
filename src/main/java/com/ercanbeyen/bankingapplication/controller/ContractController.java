package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.dto.ContractDto;
import com.ercanbeyen.bankingapplication.dto.response.MessageResponse;
import com.ercanbeyen.bankingapplication.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ContractController {
    private final ContractService contractService;

    @PostMapping
    public ResponseEntity<ContractDto> createContract(@RequestBody ContractDto request) {
        return ResponseEntity.ok(contractService.createContract(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContractDto> updateContract(@PathVariable("id") String id, @RequestBody ContractDto request) {
        return ResponseEntity.ok(contractService.updateContract(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContractDto> getContract(@PathVariable("id") String id) {
        return ResponseEntity.ok(contractService.getContract(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse<String>> deleteContract(@PathVariable("id") String id) {
        MessageResponse<String> response = new MessageResponse<>(contractService.deleteContract(id));
        return ResponseEntity.ok(response);
    }
}
