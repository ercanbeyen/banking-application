package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.dto.DeductionDto;
import com.ercanbeyen.bankingapplication.service.DeductionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/deductions")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class DeductionController {
    private final DeductionService deductionService;

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @PostMapping
    public ResponseEntity<DeductionDto> createDeduction(@RequestBody @Valid DeductionDto request) {
        return ResponseEntity.ok(deductionService.createDeduction(request));
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @PutMapping("/{account-activity-type}")
    public ResponseEntity<DeductionDto> updateDeduction(@PathVariable("account-activity-type") AccountActivityType accountActivityType, @RequestBody @Valid DeductionDto request) {
        return ResponseEntity.ok(deductionService.updateDeduction(accountActivityType, request));
    }

    @GetMapping
    public ResponseEntity<List<DeductionDto>> getDeductions() {
        return ResponseEntity.ok(deductionService.getDeductions());
    }

    @GetMapping("/{account-activity-type}")
    public ResponseEntity<DeductionDto> getDeduction(@PathVariable("account-activity-type") AccountActivityType accountActivityType) {
        return ResponseEntity.ok(deductionService.getDeduction(accountActivityType));
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @DeleteMapping("/{account-activity-type}")
    public ResponseEntity<Void> deleteDeduction(@PathVariable("account-activity-type") AccountActivityType accountActivityType) {
        deductionService.deleteDeduction(accountActivityType);
        return ResponseEntity.noContent().build();
    }
}
