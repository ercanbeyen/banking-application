package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.AgreementDto;
import com.ercanbeyen.bankingapplication.dto.response.MessageResponse;
import com.ercanbeyen.bankingapplication.service.AgreementService;
import com.ercanbeyen.bankingapplication.util.FileUtil;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/agreements")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class AgreementController {
    private final AgreementService agreementService;

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AgreementDto> createAgreement(@RequestParam("title") String title, @RequestParam("subject") String subject, @RequestParam("file") MultipartFile request) {
        FileUtil.checkFile(request);
        return ResponseEntity.ok(agreementService.createAgreement(title, subject, request));
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AgreementDto> updateAgreement(@PathVariable("id") String id, @RequestParam("title") String title, @RequestParam("subject") String subject, @RequestParam("file") MultipartFile request) {
        FileUtil.checkFile(request);
        return ResponseEntity.ok(agreementService.updateAgreement(id, title, subject, request));
    }

    @PreAuthorize("hasAuthority('READ_DATA')")
    @GetMapping
    public ResponseEntity<List<AgreementDto>> getAgreements() {
        return ResponseEntity.ok(agreementService.getAgreements());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgreementDto> getAgreement(@PathVariable("id") String id) {
        return ResponseEntity.ok(agreementService.getAgreement(id));
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse<String>> deleteAgreement(@PathVariable("id") String id) {
        agreementService.deleteAgreement(id);
        MessageResponse<String> response = new MessageResponse<>(String.format(ResponseMessage.DELETE_SUCCESS, Entity.AGREEMENT.getValue()));
        return ResponseEntity.ok(response);
    }
}
