package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.dto.BranchDto;
import com.ercanbeyen.bankingapplication.dto.option.BranchFilteringOption;
import com.ercanbeyen.bankingapplication.embeddable.Address;
import com.ercanbeyen.bankingapplication.service.BranchService;
import com.ercanbeyen.bankingapplication.service.TimeZoneService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/branches")
@SecurityRequirement(name = "Bearer Authentication")
public class BranchController extends BaseController<BranchDto, BranchFilteringOption> {
    private final BranchService branchService;
    private final TimeZoneService timeZoneService;

    public BranchController(BranchService branchService, TimeZoneService timeZoneService) {
        super(branchService);
        this.branchService = branchService;
        this.timeZoneService = timeZoneService;
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @Override
    public ResponseEntity<BranchDto> createEntity(BranchDto request) {
        Address address = request.getAddress();
        timeZoneService.checkZoneId(address.getCountry(), address.getCity());
        return ResponseEntity.ok(branchService.createEntity(request));
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @Override
    public ResponseEntity<BranchDto> updateEntity(Integer id, BranchDto request) {
        Address address = request.getAddress();
        timeZoneService.checkZoneId(address.getCountry(), address.getCity());
        return ResponseEntity.ok(branchService.updateEntity(id, request));
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @Override
    public ResponseEntity<Void> deleteEntity(Integer id) {
        branchService.deleteEntity(id);
        return ResponseEntity.ok().build();
    }
}
