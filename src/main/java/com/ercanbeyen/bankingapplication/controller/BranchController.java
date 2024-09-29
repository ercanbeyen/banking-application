package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.dto.BranchDto;
import com.ercanbeyen.bankingapplication.option.BranchFilteringOptions;
import com.ercanbeyen.bankingapplication.service.impl.BranchService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/branches")
public class BranchController extends BaseController<BranchDto, BranchFilteringOptions> {
    private final BranchService branchService;


    public BranchController(BranchService branchService) {
        super(branchService);
        this.branchService = branchService;
    }
}
