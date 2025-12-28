package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.enums.*;
import com.ercanbeyen.bankingapplication.dto.AccountActivityDto;
import com.ercanbeyen.bankingapplication.dto.AccountDto;
import com.ercanbeyen.bankingapplication.dto.request.AccountActivityFilteringRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyExchangeRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyTransferRequest;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.exception.InternalServerErrorException;
import com.ercanbeyen.bankingapplication.option.AccountFilteringOption;
import com.ercanbeyen.bankingapplication.dto.response.MessageResponse;
import com.ercanbeyen.bankingapplication.dto.response.CustomerStatisticsResponse;
import com.ercanbeyen.bankingapplication.service.AccountService;
import com.ercanbeyen.bankingapplication.exporter.ExcelExporter;
import com.ercanbeyen.bankingapplication.exporter.PdfExporter;
import com.ercanbeyen.bankingapplication.util.AccountActivityUtil;
import com.ercanbeyen.bankingapplication.util.AccountUtil;
import com.itextpdf.text.DocumentException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

@Slf4j
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController extends BaseController<AccountDto, AccountFilteringOption> {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        super(accountService);
        this.accountService = accountService;
    }

    @PostMapping
    @Override
    public ResponseEntity<AccountDto> createEntity(@RequestBody @Valid AccountDto request) {
        AccountUtil.checkRequest(request);
        return new ResponseEntity<>(accountService.createEntity(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Override
    public ResponseEntity<AccountDto> updateEntity(@PathVariable("id") Integer id, @RequestBody @Valid AccountDto request) {
        AccountUtil.checkRequest(request);
        return new ResponseEntity<>(accountService.updateEntity(id, request), HttpStatus.OK);
    }

    @PutMapping("/deposit/{id}")
    public ResponseEntity<MessageResponse<String>> depositMoney(
            @PathVariable("id") Integer id,
            @RequestParam("amount") @Valid @Min(value = 1, message = "Minimum amount should be {value}") Double amount) {
        MessageResponse<String> response = new MessageResponse<>(accountService.depositMoney(id, amount));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/withdrawal/{id}")
    public ResponseEntity<MessageResponse<String>> withdrawMoney(
            @PathVariable("id") Integer id,
            @RequestParam("amount") @Valid @Min(value = 1, message = "Minimum amount should be {value}") Double amount) {
        MessageResponse<String> response = new MessageResponse<>(accountService.withdrawMoney(id, amount));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/pay/interest/{id}")
    public ResponseEntity<MessageResponse<String>> payInterest(@PathVariable("id") Integer id) {
        MessageResponse<String> response = new MessageResponse<>(accountService.payInterest(id));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/transfer")
    public ResponseEntity<MessageResponse<String>> transferMoney(@RequestBody @Valid MoneyTransferRequest request) {
        AccountUtil.checkMoneyTransferRequest(request);
        MessageResponse<String> response = new MessageResponse<>(accountService.transferMoney(request));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/exchange")
    public ResponseEntity<MessageResponse<String>> exchangeMoney(@RequestBody @Valid MoneyExchangeRequest request) {
        AccountUtil.checkMoneyExchangeRequest(request);
        MessageResponse<String> response = new MessageResponse<>(accountService.exchangeMoney(request));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PatchMapping("/block/{id}")
    public ResponseEntity<MessageResponse<String>> updateBlockStatus(@PathVariable("id") Integer id, @RequestParam("block") Boolean status) {
        MessageResponse<String> response = new MessageResponse<>(accountService.updateBlockStatus(id, status));
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/close/{id}")
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

    @GetMapping("/{id}/account-activities")
    public ResponseEntity<List<AccountActivityDto>> getAccountActivities(@PathVariable("id") Integer id, AccountActivityFilteringRequest request) {
        AccountActivityUtil.checkFilteringRequest(request);
        return ResponseEntity.ok(accountService.getAccountActivities(id, request));
    }

    @PostMapping("/{id}/statement/pdf")
    public ResponseEntity<byte[]> generateAccountStatement(@PathVariable("id") Integer id, AccountActivityFilteringRequest request) {
        AccountActivityUtil.checkFilteringRequest(request);

        Account account = accountService.findActiveAccountById(id);
        List<AccountActivityDto> accountActivityDtos = accountService.getAccountActivities(id, request);

        LocalDate fromDate = fillDateInFilteringRequest.apply(request.fromDate());
        LocalDate toDate = fillDateInFilteringRequest.apply(request.toDate());

        ByteArrayOutputStream outputStream;

        try {
            outputStream = PdfExporter.generateAccountStatementPdf(account, fromDate, toDate, accountActivityDtos);
            log.info("Account Statement Pdf is successfully generated");
        } catch (DocumentException | IOException exception) {
            throw new InternalServerErrorException(exception.getMessage());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("account_" + account.getId() + "_statement.pdf")
                .build());
        headers.setContentLength(outputStream.size());

        return ResponseEntity.ok()
                .headers(headers)
                .body(outputStream.toByteArray());
    }

    @PostMapping("/{id}/statement/excel")
    public ResponseEntity<byte[]> exportAccountActivitiesToExcel(@PathVariable("id") Integer id, AccountActivityFilteringRequest request) {
        AccountActivityUtil.checkFilteringRequest(request);

        Account account = accountService.findActiveAccountById(id);
        List<AccountActivityDto> accountActivityDtos = accountService.getAccountActivities(id, request);

        LocalDate fromDate = fillDateInFilteringRequest.apply(request.fromDate());
        LocalDate toDate = fillDateInFilteringRequest.apply(request.toDate());

        try (Workbook workbook = ExcelExporter.generateAccountStatementWorkbook(account, accountActivityDtos, fromDate, toDate); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("account_" + account.getId() + "_activities.xlsx")
                    .build());
            headers.setContentLength(outputStream.size());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(outputStream.toByteArray());
        } catch (IOException exception) {
            throw new InternalServerErrorException(exception.getMessage());
        }
    }

    private final UnaryOperator<LocalDate> fillDateInFilteringRequest = request -> Optional.ofNullable(request).isPresent() ? request : LocalDate.now();
}
