package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.dto.BranchDto;
import com.ercanbeyen.bankingapplication.dto.option.BranchFilteringOption;
import com.ercanbeyen.bankingapplication.service.BranchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @Override
    public ResponseEntity<BranchDto> createEntity(BranchDto request) {
        return ResponseEntity.ok(branchService.createEntity(request));
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @Override
    public ResponseEntity<BranchDto> updateEntity(Integer id, BranchDto request) {
        return ResponseEntity.ok(branchService.updateEntity(id, request));
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @Override
    public ResponseEntity<Void> deleteEntity(Integer id) {
        branchService.deleteEntity(id);
        return ResponseEntity.ok().build();
    }
}
