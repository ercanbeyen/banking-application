package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.dto.DailyActivityLimitDto;
import com.ercanbeyen.bankingapplication.service.DailyActivityLimitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/daily-activity-limits")
@RequiredArgsConstructor
public class DailyActivityLimitController {
    private final DailyActivityLimitService dailyActivityLimitService;

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @PostMapping
    public ResponseEntity<DailyActivityLimitDto> createDailyActivityLimit(@RequestBody @Valid DailyActivityLimitDto request) {
        return ResponseEntity.ok(dailyActivityLimitService.createDailyActivityLimit(request));
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @PutMapping("/{activityType}")
    public ResponseEntity<DailyActivityLimitDto> updateDailyActivityLimit(@PathVariable("activityType") AccountActivityType activityType, @RequestBody @Valid DailyActivityLimitDto request) {
        return ResponseEntity.ok(dailyActivityLimitService.updateDailyActivityLimit(activityType, request));
    }

    @GetMapping
    public ResponseEntity<List<DailyActivityLimitDto>> getDailyActivityLimits() {
        return ResponseEntity.ok(dailyActivityLimitService.getDailyActivityLimits());
    }

    @GetMapping("/{activityType}")
    public ResponseEntity<DailyActivityLimitDto> getDailyActivityLimit(@PathVariable("activityType") AccountActivityType activityType) {
        return ResponseEntity.ok(dailyActivityLimitService.getDailyActivityLimit(activityType));
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @DeleteMapping("/{activityType}")
    public ResponseEntity<Void> deleteDailyActivityLimit(@PathVariable("activityType") AccountActivityType activityType) {
        dailyActivityLimitService.deleteDailyActivityLimit(activityType);
        return ResponseEntity.noContent().build();
    }
}
