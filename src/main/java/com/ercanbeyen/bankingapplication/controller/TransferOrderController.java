package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.dto.TransferOrderDto;
import com.ercanbeyen.bankingapplication.option.TransferOrderOptions;
import com.ercanbeyen.bankingapplication.service.impl.TransferOrderService;
import com.ercanbeyen.bankingapplication.util.TransferOrderUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfer-orders")
public class TransferOrderController extends BaseController<TransferOrderDto, TransferOrderOptions> {
    private final TransferOrderService transferOrderService;

    public TransferOrderController(TransferOrderService transferOrderService) {
        super(transferOrderService);
        this.transferOrderService = transferOrderService;
    }

    @PostMapping
    @Override
    public ResponseEntity<TransferOrderDto> createEntity(@RequestBody @Valid TransferOrderDto request) {
        TransferOrderUtils.checkTransferDate(request.getTransferDate());
        return ResponseEntity.ok(transferOrderService.createEntity(request));
    }

    @PutMapping("/{id}")
    @Override
    public ResponseEntity<TransferOrderDto> updateEntity(@PathVariable("id") Integer id, @RequestBody @Valid TransferOrderDto request) {
        TransferOrderUtils.checkTransferDate(request.getTransferDate());
        return ResponseEntity.ok(transferOrderService.updateEntity(id, request));
    }
}
