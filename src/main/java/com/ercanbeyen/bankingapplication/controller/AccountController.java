package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.enums.*;
import com.ercanbeyen.bankingapplication.dto.AccountDto;
import com.ercanbeyen.bankingapplication.dto.request.ExchangeRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyTransferRequest;
import com.ercanbeyen.bankingapplication.option.AccountFilteringOptions;
import com.ercanbeyen.bankingapplication.dto.response.MessageResponse;
import com.ercanbeyen.bankingapplication.dto.response.CustomerStatisticsResponse;
import com.ercanbeyen.bankingapplication.service.impl.AccountService;
import com.ercanbeyen.bankingapplication.util.AccountUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController extends BaseController<AccountDto, AccountFilteringOptions> {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        super(accountService);
        this.accountService = accountService;
    }

    @PostMapping
    @Override
    public ResponseEntity<AccountDto> createEntity(@RequestBody @Valid AccountDto request) {
        AccountUtils.checkRequest(request);
        return new ResponseEntity<>(accountService.createEntity(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Override
    public ResponseEntity<AccountDto> updateEntity(@PathVariable("id") Integer id, @RequestBody @Valid AccountDto request) {
        AccountUtils.checkRequest(request);
        return new ResponseEntity<>(accountService.updateEntity(id, request), HttpStatus.OK);
    }

    @PutMapping("/{id}/balance")
    public ResponseEntity<MessageResponse<String>> updateBalanceAccount(
            @PathVariable("id") Integer id,
            @RequestParam("activityType") AccountActivityType activityType,
            @RequestParam("amount") @Valid @Min(value = 1, message = "Minimum amount should be {value}") Double amount) {
        AccountUtils.checkAccountActivityForCurrentAccount(activityType);
        MessageResponse<String> response = new MessageResponse<>(accountService.updateBalanceOfAccount(id, activityType, amount));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/{id}/deposit/monthly")
    public ResponseEntity<MessageResponse<String>> updateBalanceOfDepositAccountMonthly(@PathVariable("id") Integer id) {
        MessageResponse<String> response = new MessageResponse<>(accountService.updateBalanceOfDepositAccountMonthly(id));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/transfer")
    public ResponseEntity<MessageResponse<String>> transferMoney(@RequestBody @Valid MoneyTransferRequest request) {
        AccountUtils.checkMoneyTransferRequest(request);
        MessageResponse<String> response = new MessageResponse<>(accountService.transferMoney(request));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/exchange")
    public ResponseEntity<MessageResponse<String>> exchangeMoney(@RequestBody @Valid ExchangeRequest request) {
        MessageResponse<String> response = new MessageResponse<>(accountService.exchangeMoney(request));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PatchMapping("/{id}/block")
    public ResponseEntity<MessageResponse<String>> updateBlockStatus(@PathVariable("id") Integer id, @RequestParam("block") Boolean status) {
        MessageResponse<String> response = new MessageResponse<>(accountService.updateBlockStatus(id, status));
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<MessageResponse<String>> closeAccount(@PathVariable("id") Integer id) {
        MessageResponse<String> response = new MessageResponse<>(accountService.closeAccount(id));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/total")
    public ResponseEntity<MessageResponse<String>> getTotalAccounts(
            @RequestParam("type") AccountType type,
            @RequestParam("currency") Currency currency,
            @RequestParam(name = "city", required = false) City city) {
        MessageResponse<String> response = new MessageResponse<>(accountService.getTotalActiveAccounts(type, currency, city));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/statistics/maximum-balances")
    public ResponseEntity<MessageResponse<List<CustomerStatisticsResponse>>> getCustomerInformationWithMaximumBalance(
            @RequestParam("type") AccountType type,
            @RequestParam("currency") Currency currency) {
        MessageResponse<List<CustomerStatisticsResponse>> response = new MessageResponse<>(accountService.getCustomersHaveMaximumBalance(type, currency));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
