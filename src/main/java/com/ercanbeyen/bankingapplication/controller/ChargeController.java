package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.dto.ChargeDto;
import com.ercanbeyen.bankingapplication.option.ChargeFilteringOption;
import com.ercanbeyen.bankingapplication.service.impl.ChargeService;
import com.ercanbeyen.bankingapplication.util.ChargeUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/charges")
public class ChargeController extends BaseController<ChargeDto, ChargeFilteringOption> {
    private final ChargeService chargeService;

    public ChargeController(ChargeService chargeService) {
        super(chargeService);
        this.chargeService = chargeService;
    }

    @PostMapping
    @Override
    public ResponseEntity<ChargeDto> createEntity(@RequestBody @Valid ChargeDto request) {
        ChargeUtil.checkRequest(request);
        return ResponseEntity.ok(chargeService.createEntity(request));
    }

    @PutMapping("/{id}")
    @Override
    public ResponseEntity<ChargeDto> updateEntity(@PathVariable("id") Integer id, @RequestBody @Valid ChargeDto request) {
        ChargeUtil.checkRequest(request);
        return ResponseEntity.ok(chargeService.updateEntity(id, request));
    }
}
