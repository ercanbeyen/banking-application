package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.constant.enums.PaymentType;
import com.ercanbeyen.bankingapplication.dto.*;
import com.ercanbeyen.bankingapplication.dto.response.CustomerStatusResponse;
import com.ercanbeyen.bankingapplication.embeddable.ExpectedTransaction;
import com.ercanbeyen.bankingapplication.entity.File;
import com.ercanbeyen.bankingapplication.option.AccountFilteringOptions;
import com.ercanbeyen.bankingapplication.option.CustomerFilteringOptions;
import com.ercanbeyen.bankingapplication.option.AccountActivityFilteringOptions;
import com.ercanbeyen.bankingapplication.dto.response.MessageResponse;
import com.ercanbeyen.bankingapplication.service.impl.CustomerService;
import com.ercanbeyen.bankingapplication.util.CustomerUtils;
import com.ercanbeyen.bankingapplication.util.PhotoUtils;
import com.ercanbeyen.bankingapplication.util.TransferOrderUtils;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@Slf4j
public class CustomerController extends BaseController<CustomerDto, CustomerFilteringOptions> {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        super(customerService);
        this.customerService = customerService;
    }

    @PostMapping
    @Override
    public ResponseEntity<CustomerDto> createEntity(@RequestBody @Valid CustomerDto request) {
        CustomerUtils.checkRequest(request);
        return new ResponseEntity<>(customerService.createEntity(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Override
    public ResponseEntity<CustomerDto> updateEntity(@PathVariable("id") Integer id, @RequestBody @Valid CustomerDto request) {
        CustomerUtils.checkRequest(request);
        return ResponseEntity.ok(customerService.updateEntity(id, request));
    }

    @PostMapping("/{id}")
    public ResponseEntity<MessageResponse<String>> uploadProfilePhoto(@PathVariable("id") Integer id, @RequestParam("file") MultipartFile file) {
        PhotoUtils.checkPhoto(file);
        MessageResponse<String> response = new MessageResponse<>(customerService.uploadProfilePhoto(id, file));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> downloadProfilePhoto(@PathVariable("id") Integer id) {
        File file = customerService.downloadProfilePhoto(id);

        String fileName = file.getName();
        log.info("file.getName() and its length: {} - {}", fileName, fileName.length());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(file.getData());
    }

    @DeleteMapping("/{id}/photo")
    public ResponseEntity<MessageResponse<String>> deleteProfilePhoto(@PathVariable("id") Integer id) {
        MessageResponse<String> response = new MessageResponse<>(customerService.deleteProfilePhoto(id));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{nationalId}/status")
    public ResponseEntity<CustomerStatusResponse> calculateStatus(@PathVariable("nationalId") String nationalId, @RequestParam("base") Currency baseCurrency) {
        return ResponseEntity.ok(customerService.calculateStatus(nationalId, baseCurrency));
    }

    @GetMapping("/{id}/accounts")
    public ResponseEntity<List<AccountDto>> getAccounts(@PathVariable("id") Integer id, AccountFilteringOptions options) {
        return ResponseEntity.ok(customerService.getAccounts(id, options));
    }

    @GetMapping("/{id}/account-activities")
    public ResponseEntity<List<AccountActivityDto>> getAccountActivities(@PathVariable("id") Integer id, AccountActivityFilteringOptions options) {
        return ResponseEntity.ok(customerService.getAccountActivities(id, options));
    }

    @GetMapping("/{id}/notifications")
    public ResponseEntity<List<NotificationDto>> getNotifications(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(customerService.getNotifications(id));
    }

    @GetMapping("/{id}/transfer-orders")
    public ResponseEntity<List<TransferOrderDto>> getTransferOrders(
            @PathVariable("id") Integer id,
            @RequestParam("from") LocalDate fromDate,
            @RequestParam("to") LocalDate toDate,
            @RequestParam(value = "currency", required = false) Currency currency,
            @RequestParam(value = "payment-type", required = false) PaymentType paymentType) {
        TransferOrderUtils.checkDatesBeforeFiltering(fromDate, toDate);
        return ResponseEntity.ok(customerService.getTransferOrders(id, fromDate, toDate, currency, paymentType));
    }

    @GetMapping("/{id}/cash-flow-calendar")
    public ResponseEntity<CashFlowCalendarDto> getCashFlowCalendar(
            @PathVariable("id") Integer id,
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) {
        return ResponseEntity.ok(customerService.getCashFlowCalendar(id, year, month));
    }

    @GetMapping("/{id}/expected-transactions")
    public ResponseEntity<List<ExpectedTransaction>> getExpectedTransactions(@PathVariable("id") Integer id, @RequestParam("months") Integer months) {
        return ResponseEntity.ok(customerService.getExpectedTransactions(id, months));
    }
}
