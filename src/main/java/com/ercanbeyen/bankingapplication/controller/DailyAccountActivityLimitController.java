package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.dto.DailyAccountActivityLimitDto;
import com.ercanbeyen.bankingapplication.service.DailyAccountActivityLimitService;
import com.ercanbeyen.bankingapplication.util.DailyAccountActivityLimitUtil;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/daily-account-activity-limits")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class DailyAccountActivityLimitController {
    private final DailyAccountActivityLimitService dailyAccountActivityLimitService;

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @PostMapping
    public ResponseEntity<DailyAccountActivityLimitDto> createDailyAccountActivityLimit(@RequestBody @Valid DailyAccountActivityLimitDto request) {
        DailyAccountActivityLimitUtil.checkRequest(request);
        return ResponseEntity.ok(dailyAccountActivityLimitService.createDailyAccountActivityLimit(request));
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @PutMapping("/{activityType}")
    public ResponseEntity<DailyAccountActivityLimitDto> updateDailyAccountActivityLimit(@PathVariable("activityType") AccountActivityType activityType, @RequestBody @Valid DailyAccountActivityLimitDto request) {
        DailyAccountActivityLimitUtil.checkRequest(request);
        return ResponseEntity.ok(dailyAccountActivityLimitService.updateDailyAccountActivityLimit(activityType, request));
    }

    @GetMapping
    public ResponseEntity<List<DailyAccountActivityLimitDto>> getDailyAccountActivityLimits() {
        return ResponseEntity.ok(dailyAccountActivityLimitService.getDailyAccountActivityLimits());
    }

    @GetMapping("/{activityType}")
    public ResponseEntity<DailyAccountActivityLimitDto> getDailyAccountActivityLimit(@PathVariable("activityType") AccountActivityType activityType) {
        return ResponseEntity.ok(dailyAccountActivityLimitService.getDailyAccountActivityLimit(activityType));
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @DeleteMapping("/{activityType}")
    public ResponseEntity<Void> deleteDailyAccountActivityLimit(@PathVariable("activityType") AccountActivityType activityType) {
        dailyAccountActivityLimitService.deleteDailyAccountActivityLimit(activityType);
        return ResponseEntity.noContent().build();
    }
}
