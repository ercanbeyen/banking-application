package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.dto.FeeDto;
import com.ercanbeyen.bankingapplication.option.FeeFilteringOption;
import com.ercanbeyen.bankingapplication.service.impl.FeeService;
import com.ercanbeyen.bankingapplication.util.FeeUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fees")
public class FeeController extends BaseController<FeeDto, FeeFilteringOption> {
    private final FeeService feeService;

    public FeeController(FeeService feeService) {
        super(feeService);
        this.feeService = feeService;
    }

    @PostMapping
    @Override
    public ResponseEntity<FeeDto> createEntity(@RequestBody @Valid FeeDto request) {
        FeeUtil.checkRequest(request);
        return ResponseEntity.ok(feeService.createEntity(request));
    }

    @PutMapping("/{id}")
    @Override
    public ResponseEntity<FeeDto> updateEntity(@PathVariable("id") Integer id, @RequestBody @Valid FeeDto request) {
        FeeUtil.checkRequest(request);
        return ResponseEntity.ok(feeService.updateEntity(id, request));
    }
}
