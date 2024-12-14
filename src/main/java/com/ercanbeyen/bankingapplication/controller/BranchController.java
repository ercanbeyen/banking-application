package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.dto.BranchDto;
import com.ercanbeyen.bankingapplication.option.BranchFilteringOption;
import com.ercanbeyen.bankingapplication.service.impl.BranchService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/branches")
public class BranchController extends BaseController<BranchDto, BranchFilteringOption> {
    private final BranchService branchService;


    public BranchController(BranchService branchService) {
        super(branchService);
        this.branchService = branchService;
    }
}
