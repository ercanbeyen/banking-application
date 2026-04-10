package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.dto.DeductionDto;
import com.ercanbeyen.bankingapplication.service.DeductionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/deductions")
@RequiredArgsConstructor
public class DeductionController {
    private final DeductionService deductionService;

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @PostMapping
    public ResponseEntity<DeductionDto> createDeduction(@RequestBody @Valid DeductionDto request) {
        return ResponseEntity.ok(deductionService.createDeduction(request));
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @PutMapping("/{activityType}")
    public ResponseEntity<DeductionDto> updateDeduction(@PathVariable("activityType") AccountActivityType activityType, @RequestBody @Valid DeductionDto request) {
        return ResponseEntity.ok(deductionService.updateDeduction(activityType, request));
    }

    @GetMapping
    public ResponseEntity<List<DeductionDto>> getDeductions() {
        return ResponseEntity.ok(deductionService.getDeductions());
    }

    @GetMapping("/{activityType}")
    public ResponseEntity<DeductionDto> getDeduction(@PathVariable("activityType") AccountActivityType activityType) {
        return ResponseEntity.ok(deductionService.getDeduction(activityType));
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @DeleteMapping("/{activityType}")
    public ResponseEntity<Void> deleteDeduction(@PathVariable("activityType") AccountActivityType activityType) {
        deductionService.deleteDeduction(activityType);
        return ResponseEntity.noContent().build();
    }
}
