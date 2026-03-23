package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.dto.ChargeDto;
import com.ercanbeyen.bankingapplication.service.ChargeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/charges")
@RequiredArgsConstructor
public class ChargeController {
    private final ChargeService chargeService;

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @PostMapping
    public ResponseEntity<ChargeDto> createCharge(@RequestBody @Valid ChargeDto request) {
        return ResponseEntity.ok(chargeService.createCharge(request));
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @PutMapping("/{activityType}")
    public ResponseEntity<ChargeDto> updateCharge(@PathVariable("activityType") AccountActivityType activityType, @RequestBody @Valid ChargeDto request) {
        return ResponseEntity.ok(chargeService.updateCharge(activityType, request));
    }

    @GetMapping
    public ResponseEntity<List<ChargeDto>> getCharges() {
        return ResponseEntity.ok(chargeService.getCharges());
    }

    @GetMapping("/{activityType}")
    public ResponseEntity<ChargeDto> getCharge(@PathVariable("activityType") AccountActivityType activityType) {
        return ResponseEntity.ok(chargeService.getCharge(activityType));
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @DeleteMapping("/{activityType}")
    public ResponseEntity<Void> deleteCharge(@PathVariable("activityType") AccountActivityType activityType) {
        chargeService.deleteCharge(activityType);
        return ResponseEntity.noContent().build();
    }
}
