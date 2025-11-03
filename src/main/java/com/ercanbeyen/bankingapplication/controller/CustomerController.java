package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.constant.enums.PaymentType;
import com.ercanbeyen.bankingapplication.dto.*;
import com.ercanbeyen.bankingapplication.dto.response.CustomerStatusResponse;
import com.ercanbeyen.bankingapplication.embeddable.ExpectedTransaction;
import com.ercanbeyen.bankingapplication.embeddable.RegisteredRecipient;
import com.ercanbeyen.bankingapplication.entity.File;
import com.ercanbeyen.bankingapplication.option.AccountFilteringOption;
import com.ercanbeyen.bankingapplication.option.CustomerFilteringOption;
import com.ercanbeyen.bankingapplication.dto.response.MessageResponse;
import com.ercanbeyen.bankingapplication.service.CustomerService;
import com.ercanbeyen.bankingapplication.util.CashFlowCalendarUtil;
import com.ercanbeyen.bankingapplication.util.CustomerUtil;
import com.ercanbeyen.bankingapplication.util.PhotoUtil;
import com.ercanbeyen.bankingapplication.util.MoneyTransferOrderUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Range;
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
public class CustomerController extends BaseController<CustomerDto, CustomerFilteringOption> {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        super(customerService);
        this.customerService = customerService;
    }

    @PostMapping
    @Override
    public ResponseEntity<CustomerDto> createEntity(@RequestBody @Valid CustomerDto request) {
        CustomerUtil.checkRequest(request);
        return new ResponseEntity<>(customerService.createEntity(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Override
    public ResponseEntity<CustomerDto> updateEntity(@PathVariable("id") Integer id, @RequestBody @Valid CustomerDto request) {
        CustomerUtil.checkRequest(request);
        return ResponseEntity.ok(customerService.updateEntity(id, request));
    }

    @PatchMapping("/{id}/registered-recipients")
    public ResponseEntity<String> addRegisteredRecipient(@PathVariable("id") Integer id, @RequestBody @Valid RegisteredRecipient request) {
        return ResponseEntity.ok(customerService.addRegisteredRecipient(id, request));
    }

    @DeleteMapping("/{id}/registered-recipients/{accountId}")
    public ResponseEntity<String> removeRegisteredRecipient(@PathVariable("id") Integer id, @PathVariable("accountId") Integer accountId) {
        return ResponseEntity.ok(customerService.removeRegisteredRecipient(id, accountId));
    }

    @PostMapping("/{id}")
    public ResponseEntity<MessageResponse<String>> uploadProfilePhoto(@PathVariable("id") Integer id, @RequestParam("file") MultipartFile request) {
        PhotoUtil.checkPhoto(request);
        MessageResponse<String> response = new MessageResponse<>(customerService.uploadProfilePhoto(id, request));
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
    public ResponseEntity<List<AccountDto>> getAccounts(@PathVariable("id") Integer id, AccountFilteringOption option) {
        return ResponseEntity.ok(customerService.getAccounts(id, option));
    }

    @GetMapping("/{id}/notifications")
    public ResponseEntity<List<NotificationDto>> getNotifications(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(customerService.getNotifications(id));
    }

    @GetMapping("/{id}/money-transfer-orders")
    public ResponseEntity<List<MoneyTransferOrderDto>> getMoneyTransferOrders(
            @PathVariable("id") Integer id,
            @RequestParam("from") LocalDate fromDate,
            @RequestParam("to") LocalDate toDate,
            @RequestParam(value = "currency", required = false) Currency currency,
            @RequestParam(value = "payment-type", required = false) PaymentType paymentType) {
        MoneyTransferOrderUtil.checkDatesBeforeFiltering(fromDate, toDate);
        return ResponseEntity.ok(customerService.getMoneyTransferOrders(id, fromDate, toDate, currency, paymentType));
    }

    @GetMapping("/{id}/cash-flow-calendar")
    public ResponseEntity<CashFlowCalendarDto> getCashFlowCalendar(
            @PathVariable("id") Integer id,
            @RequestParam("year") Integer year,
            @RequestParam("month") @Range(min = 1, max = 12, message = "Month should be between {min} and {max}") Integer month) {
        CashFlowCalendarUtil.checkMonthAndYearForCashFlowCalendar(year, month);
        return ResponseEntity.ok(customerService.getCashFlowCalendar(id, year, month));
    }

    @GetMapping("/{id}/expected-transactions")
    public ResponseEntity<List<ExpectedTransaction>> getExpectedTransactions(@PathVariable("id") Integer id, @RequestParam(value = "month", defaultValue = "1") Integer month) {
        CashFlowCalendarUtil.checkMonthValueForExpectedTransactions(month);
        return ResponseEntity.ok(customerService.getExpectedTransactions(id, month));
    }

    @GetMapping("/{id}/agreements")
    public ResponseEntity<MessageResponse<List<String>>> getAgreementSubjects(@PathVariable("id") Integer id) {
        List<String> agreementSubjects = customerService.getAgreementSubjects(id);
        MessageResponse<List<String>> response = new MessageResponse<>(agreementSubjects);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/registered-recipients")
    public ResponseEntity<List<RegisteredRecipient>> addRegisteredRecipient(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(customerService.getRegisteredRecipients(id));
    }
}
