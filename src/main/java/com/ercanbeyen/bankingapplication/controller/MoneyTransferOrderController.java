package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.dto.MoneyTransferOrderDto;
import com.ercanbeyen.bankingapplication.option.MoneyTransferOrderOption;
import com.ercanbeyen.bankingapplication.service.MoneyTransferOrderService;
import com.ercanbeyen.bankingapplication.util.MoneyTransferOrderUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/money-transfer-orders")
public class MoneyTransferOrderController extends BaseController<MoneyTransferOrderDto, MoneyTransferOrderOption> {
    private final MoneyTransferOrderService moneyTransferOrderService;

    public MoneyTransferOrderController(MoneyTransferOrderService moneyTransferOrderService) {
        super(moneyTransferOrderService);
        this.moneyTransferOrderService = moneyTransferOrderService;
    }

    @PostMapping
    @Override
    public ResponseEntity<MoneyTransferOrderDto> createEntity(@RequestBody @Valid MoneyTransferOrderDto request) {
        MoneyTransferOrderUtil.checkMoneyTransferDate(request.getTransferDate());
        return ResponseEntity.ok(moneyTransferOrderService.createEntity(request));
    }

    @PutMapping("/{id}")
    @Override
    public ResponseEntity<MoneyTransferOrderDto> updateEntity(@PathVariable("id") Integer id, @RequestBody @Valid MoneyTransferOrderDto request) {
        MoneyTransferOrderUtil.checkMoneyTransferDate(request.getTransferDate());
        return ResponseEntity.ok(moneyTransferOrderService.updateEntity(id, request));
    }
}
