package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.dto.AtmDto;
import com.ercanbeyen.bankingapplication.dto.option.AtmFilteringOption;
import com.ercanbeyen.bankingapplication.embeddable.Address;
import com.ercanbeyen.bankingapplication.service.AtmService;
import com.ercanbeyen.bankingapplication.service.TimeZoneService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/atms")
@SecurityRequirement(name = "Bearer Authentication")
public class AtmController extends BaseController<AtmDto, AtmFilteringOption> {
    private final AtmService atmService;
    private final TimeZoneService timeZoneService;

    public AtmController(AtmService atmService, TimeZoneService timeZoneService) {
        super(atmService);
        this.atmService = atmService;
        this.timeZoneService = timeZoneService;
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @PostMapping
    @Override
    public ResponseEntity<AtmDto> createEntity(@RequestBody @Valid AtmDto request) {
        Address address = request.getAddress();
        timeZoneService.checkZoneId(address.getCountry(), address.getCity());
        return ResponseEntity.ok(atmService.createEntity(request));
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @PutMapping("/{id}")
    @Override
    public ResponseEntity<AtmDto> updateEntity(@PathVariable("id") Integer id, @RequestBody @Valid AtmDto request) {
        Address address = request.getAddress();
        timeZoneService.checkZoneId(address.getCountry(), address.getCity());
        return ResponseEntity.ok(atmService.updateEntity(id, request));
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @DeleteMapping("/{id}")
    @Override
    public ResponseEntity<Void> deleteEntity(@PathVariable("id") Integer id) {
        atmService.deleteEntity(id);
        return ResponseEntity.noContent().build();
    }
}
