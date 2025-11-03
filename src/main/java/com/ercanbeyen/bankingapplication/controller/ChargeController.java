package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.dto.ChargeDto;
import com.ercanbeyen.bankingapplication.service.ChargeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/charges")
@RequiredArgsConstructor
public class ChargeController {
    private final ChargeService chargeService;

    @PostMapping
    public ResponseEntity<ChargeDto> createCharge(@RequestBody @Valid ChargeDto request) {
        return ResponseEntity.ok(chargeService.createCharge(request));
    }

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

    @DeleteMapping("/{activityType}")
    public ResponseEntity<Void> deleteCharge(@PathVariable("activityType") AccountActivityType activityType) {
        chargeService.deleteCharge(activityType);
        return ResponseEntity.noContent().build();
    }
}
