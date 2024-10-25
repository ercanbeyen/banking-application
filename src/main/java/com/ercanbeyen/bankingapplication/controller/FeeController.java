package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.dto.FeeDto;
import com.ercanbeyen.bankingapplication.option.FeeFilteringOptions;
import com.ercanbeyen.bankingapplication.service.impl.FeeService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fees")
public class FeeController extends BaseController<FeeDto, FeeFilteringOptions> {
    private final FeeService feeService;

    public FeeController(FeeService feeService) {
        super(feeService);
        this.feeService = feeService;
    }
}
