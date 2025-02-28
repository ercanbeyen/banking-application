package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.dto.AgreementDto;
import com.ercanbeyen.bankingapplication.dto.response.MessageResponse;
import com.ercanbeyen.bankingapplication.service.AgreementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/agreements")
@RequiredArgsConstructor
public class AgreementController {
    private final AgreementService agreementService;

    @PostMapping
    public ResponseEntity<AgreementDto> createAgreement(@RequestParam("subject") String subject, @RequestParam("file") MultipartFile request) {
        return ResponseEntity.ok(agreementService.createAgreement(subject, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgreementDto> updateAgreement(@PathVariable("id") String id, @RequestParam("subject") String subject, @RequestParam("file") MultipartFile request) {
        return ResponseEntity.ok(agreementService.updateAgreement(id, subject, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgreementDto> getAgreement(@PathVariable("id") String id) {
        return ResponseEntity.ok(agreementService.getAgreement(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse<String>> deleteAgreement(@PathVariable("id") String id) {
        MessageResponse<String> response = new MessageResponse<>(agreementService.deleteAgreement(id));
        return ResponseEntity.ok(response);
    }
}
