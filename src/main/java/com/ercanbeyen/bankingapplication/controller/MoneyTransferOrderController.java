package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.dto.MoneyTransferOrderDto;
import com.ercanbeyen.bankingapplication.dto.option.MoneyTransferOrderOption;
import com.ercanbeyen.bankingapplication.security.service.AccountSecurityService;
import com.ercanbeyen.bankingapplication.security.service.MoneyTransferOrderSecurityService;
import com.ercanbeyen.bankingapplication.service.MoneyTransferOrderService;
import com.ercanbeyen.bankingapplication.util.MoneyTransferOrderUtil;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/money-transfer-orders")
@SecurityRequirement(name = "Bearer Authentication")
public class MoneyTransferOrderController extends BaseController<MoneyTransferOrderDto, MoneyTransferOrderOption> {
    private final MoneyTransferOrderService moneyTransferOrderService;
    private final MoneyTransferOrderSecurityService moneyTransferOrderSecurityService;
    private final AccountSecurityService accountSecurityService;

    public MoneyTransferOrderController(
            MoneyTransferOrderService moneyTransferOrderService,
            MoneyTransferOrderSecurityService moneyTransferOrderSecurityService,
            AccountSecurityService accountSecurityService) {
        super(moneyTransferOrderService);
        this.moneyTransferOrderService = moneyTransferOrderService;
        this.moneyTransferOrderSecurityService = moneyTransferOrderSecurityService;
        this.accountSecurityService = accountSecurityService;
    }

    @Override
    public ResponseEntity<List<MoneyTransferOrderDto>> getEntities(MoneyTransferOrderOption filteringOption) {
        return ResponseEntity.ok(moneyTransferOrderService.getEntities(filteringOption));
    }

    @PostAuthorize("@accountSecurityService.isOwner(returnObject.body.senderAccountId, authentication)")
    @GetMapping("/{id}")
    @Override
    public ResponseEntity<MoneyTransferOrderDto> getEntity(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(moneyTransferOrderService.getEntity(id));
    }

    @PreAuthorize("@accountSecurityService.isOwner(#moneyTransferOrder.senderAccountId, authentication)")
    @PostMapping
    @Override
    public ResponseEntity<MoneyTransferOrderDto> createEntity(@RequestBody @Valid @P("moneyTransferOrder") MoneyTransferOrderDto request) {
        MoneyTransferOrderUtil.checkMoneyTransferDate(request.getTransferDate());
        return ResponseEntity.ok(moneyTransferOrderService.createEntity(request));
    }

    @PreAuthorize("@moneyTransferOrderSecurityService.isOwner(#moneyTransferOrderId, authentication)")
    @PutMapping("/{id}")
    @Override
    public ResponseEntity<MoneyTransferOrderDto> updateEntity(@PathVariable("id") @P("moneyTransferOrderId") Integer id, @RequestBody @Valid MoneyTransferOrderDto request) {
        MoneyTransferOrderUtil.checkMoneyTransferDate(request.getTransferDate());
        return ResponseEntity.ok(moneyTransferOrderService.updateEntity(id, request));
    }

    @PreAuthorize("@moneyTransferOrderSecurityService.isOwner(#moneyTransferOrderId, authentication)")
    @DeleteMapping("/{id}")
    @Override
    public ResponseEntity<Void> deleteEntity(@PathVariable("id") @P("moneyTransferOrderId") Integer id) {
        moneyTransferOrderService.deleteEntity(id);
        return ResponseEntity.ok().build();
    }
}
