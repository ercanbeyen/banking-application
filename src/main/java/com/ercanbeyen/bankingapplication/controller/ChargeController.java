package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.dto.ChargeDto;
import com.ercanbeyen.bankingapplication.option.ChargeFilteringOptions;
import com.ercanbeyen.bankingapplication.service.impl.ChargeService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/charges")
public class ChargeController extends BaseController<ChargeDto, ChargeFilteringOptions> {
    private final ChargeService chargeService;

    public ChargeController(ChargeService chargeService) {
        super(chargeService);
        this.chargeService = chargeService;
    }
}
