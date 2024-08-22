package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.enums.AddressActivity;
import com.ercanbeyen.bankingapplication.dto.AddressDto;
import com.ercanbeyen.bankingapplication.dto.request.CreateAddressRequest;
import com.ercanbeyen.bankingapplication.dto.response.MessageResponse;
import com.ercanbeyen.bankingapplication.service.AddressService;
import com.ercanbeyen.bankingapplication.util.AddressUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
public class AddressController {
    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<List<AddressDto>> getEntities() {
        return ResponseEntity.ok(addressService.getEntities());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AddressDto> getEntity(@PathVariable("id") String id) {
        return ResponseEntity.ok(addressService.getEntity(id));
    }

    @PostMapping
    public ResponseEntity<AddressDto> createEntity(@RequestBody @Valid CreateAddressRequest request) {
        AddressUtils.checkAddressType(request.type(), request.companyName());
        return ResponseEntity.ok(addressService.createEntity(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressDto> updateEntity(@PathVariable("id") String id, @RequestBody @Valid AddressDto request) {
        AddressUtils.checkAddressType(request.type(), request.companyName());
        return ResponseEntity.ok(addressService.updateEntity(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEntity(@PathVariable("id") String id) {
        addressService.deleteEntity(id);
        return ResponseEntity.noContent()
                .build();
    }

    @PutMapping("/{addressId}/customers/{customerNationalId}")
    public ResponseEntity<MessageResponse<String>> modifyRelationshipBetweenAddressAndCustomer(
            @PathVariable("addressId") String addressId,
            @PathVariable("customerNationalId") String customerNationalId,
            @RequestParam("activity") AddressActivity activity) {
        MessageResponse<String> response = new MessageResponse<>(addressService.modifyRelationshipBetweenAddressAndCustomer(addressId, customerNationalId, activity));
        return ResponseEntity.ok(response);
    }
}
