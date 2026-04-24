package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.dto.TermDepositInterestRateDto;
import com.ercanbeyen.bankingapplication.dto.option.TermDepositInterestRateFilteringOption;
import com.ercanbeyen.bankingapplication.service.TermDepositInterestRateService;
import com.ercanbeyen.bankingapplication.util.TermDepositInterestRateUtil;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/term-deposit-interest-rates")
@SecurityRequirement(name = "Bearer Authentication")
public class TermDepositInterestRateController extends BaseController<TermDepositInterestRateDto, TermDepositInterestRateFilteringOption> {
    private final TermDepositInterestRateService termDepositInterestRateService;

    public TermDepositInterestRateController(TermDepositInterestRateService termDepositInterestRateService) {
        super(termDepositInterestRateService);
        this.termDepositInterestRateService = termDepositInterestRateService;
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @PostMapping
    @Override
    public ResponseEntity<TermDepositInterestRateDto> createEntity(@RequestBody @Valid TermDepositInterestRateDto request) {
        TermDepositInterestRateUtil.checkRequest(request);
        return ResponseEntity.ok(termDepositInterestRateService.createEntity(request));
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @PutMapping("/{id}")
    @Override
    public ResponseEntity<TermDepositInterestRateDto> updateEntity(@PathVariable("id") Integer id, @RequestBody @Valid TermDepositInterestRateDto request) {
        TermDepositInterestRateUtil.checkRequest(request);
        return ResponseEntity.ok(termDepositInterestRateService.updateEntity(id, request));
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @Override
    public ResponseEntity<Void> deleteEntity(Integer id) {
        termDepositInterestRateService.deleteEntity(id);
        return ResponseEntity.ok().build();
    }
}
