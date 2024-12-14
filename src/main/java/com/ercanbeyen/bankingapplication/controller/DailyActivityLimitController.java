package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.dto.DailyActivityLimitDto;
import com.ercanbeyen.bankingapplication.option.DailyActivityLimitFilteringOption;
import com.ercanbeyen.bankingapplication.service.impl.DailyActivityLimitService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/daily-activity-limits")
public class DailyActivityLimitController extends BaseController<DailyActivityLimitDto, DailyActivityLimitFilteringOption> {
    private final DailyActivityLimitService dailyActivityLimitService;

    public DailyActivityLimitController(DailyActivityLimitService dailyActivityLimitService) {
        super(dailyActivityLimitService);
        this.dailyActivityLimitService = dailyActivityLimitService;
    }
}
