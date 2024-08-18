package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.dto.AddressDto;
import com.ercanbeyen.bankingapplication.dto.request.CreateAddressRequest;
import com.ercanbeyen.bankingapplication.service.AddressService;
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
        return ResponseEntity.ok(addressService.createEntity(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressDto> updateEntity(@PathVariable("id") String id, @RequestBody @Valid AddressDto addressDto) {
        return ResponseEntity.ok(addressService.updateEntity(id, addressDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEntity(@PathVariable("id") String id) {
        addressService.deleteEntity(id);
        return ResponseEntity.noContent()
                .build();
    }
}
