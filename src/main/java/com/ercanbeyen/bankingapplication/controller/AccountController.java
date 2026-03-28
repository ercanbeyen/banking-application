package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.enums.*;
import com.ercanbeyen.bankingapplication.dto.AccountActivityDto;
import com.ercanbeyen.bankingapplication.dto.AccountDto;
import com.ercanbeyen.bankingapplication.dto.request.AccountActivityFilteringRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyExchangeRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyTransferRequest;
import com.ercanbeyen.bankingapplication.model.Account;
import com.ercanbeyen.bankingapplication.exception.InternalServerErrorException;
import com.ercanbeyen.bankingapplication.dto.option.AccountFilteringOption;
import com.ercanbeyen.bankingapplication.dto.response.MessageResponse;
import com.ercanbeyen.bankingapplication.dto.response.CustomerStatisticsResponse;
import com.ercanbeyen.bankingapplication.security.service.AccountSecurityService;
import com.ercanbeyen.bankingapplication.service.AccountService;
import com.ercanbeyen.bankingapplication.util.exporter.ExcelExporter;
import com.ercanbeyen.bankingapplication.util.exporter.PdfExporter;
import com.ercanbeyen.bankingapplication.util.AccountActivityUtil;
import com.ercanbeyen.bankingapplication.util.AccountUtil;
import com.itextpdf.text.DocumentException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
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
    private final AccountSecurityService accountSecurityService;

    public AccountController(AccountService accountService, AccountSecurityService accountSecurityService) {
        super(accountService);
        this.accountService = accountService;
        this.accountSecurityService = accountSecurityService;
    }

    @PreAuthorize("hasAuthority('READ_DATA')")
    @GetMapping
    @Override
    public ResponseEntity<List<AccountDto>> getEntities(AccountFilteringOption filteringOption) {
        return ResponseEntity.ok(accountService.getEntities(filteringOption));
    }

    @PreAuthorize("hasAuthority('READ_DATA') OR @accountSecurityService.isOwner(#accountId, authentication)")
    @GetMapping("/{id}")
    @Override
    public ResponseEntity<AccountDto> getEntity(@PathVariable("id") @P("accountId") Integer id) {
        return ResponseEntity.ok(accountService.getEntity(id));
    }

    @PreAuthorize("#account.customerNationalId == authentication.principal.username")
    @PostMapping
    @Override
    public ResponseEntity<AccountDto> createEntity(@RequestBody @Valid @P("account") AccountDto request) {
        AccountUtil.checkRequest(request);
        return new ResponseEntity<>(accountService.createEntity(request), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @PutMapping("/{id}")
    @Override
    public ResponseEntity<AccountDto> updateEntity(@PathVariable("id") Integer id, @RequestBody @Valid AccountDto request) {
        AccountUtil.checkRequest(request);
        return new ResponseEntity<>(accountService.updateEntity(id, request), HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @DeleteMapping("/{id}")
    @Override
    public ResponseEntity<Void> deleteEntity(@PathVariable("id") Integer id) {
        accountService.deleteEntity(id);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@accountSecurityService.isOwner(#accountId, authentication)")
    @PutMapping("/deposit/{id}")
    public ResponseEntity<MessageResponse<String>> depositMoney(
            @PathVariable("id") @P("accountId") Integer id,
            @RequestParam("amount") @Valid @Min(value = 1, message = "Minimum amount should be {value}") Double amount) {
        MessageResponse<String> response = new MessageResponse<>(accountService.depositMoney(id, amount));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PreAuthorize("@accountSecurityService.isOwner(#accountId, authentication)")
    @PutMapping("/withdrawal/{id}")
    public ResponseEntity<MessageResponse<String>> withdrawMoney(
            @PathVariable("id") @P("accountId") Integer id,
            @RequestParam("amount") @Valid @Min(value = 1, message = "Minimum amount should be {value}") Double amount) {
        MessageResponse<String> response = new MessageResponse<>(accountService.withdrawMoney(id, amount));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @PutMapping("/pay/interest/{id}")
    public ResponseEntity<MessageResponse<String>> payInterest(@PathVariable("id") Integer id) {
        MessageResponse<String> response = new MessageResponse<>(accountService.payInterest(id));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PreAuthorize("@accountSecurityService.isOwner(#moneyTransfer.senderAccountId, authentication)")
    @PutMapping("/transfer")
    public ResponseEntity<MessageResponse<String>> transferMoney(@RequestBody @Valid @P("moneyTransfer") MoneyTransferRequest request) {
        AccountUtil.checkMoneyTransferRequest(request);
        MessageResponse<String> response = new MessageResponse<>(accountService.transferMoney(request));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PreAuthorize("@accountSecurityService.isOwner(#moneyExchange.sellerAccountId, authentication)")
    @PutMapping("/exchange")
    public ResponseEntity<MessageResponse<String>> exchangeMoney(@RequestBody @Valid @P("moneyExchange") MoneyExchangeRequest request) {
        AccountUtil.checkMoneyExchangeRequest(request);
        MessageResponse<String> response = new MessageResponse<>(accountService.exchangeMoney(request));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/block/{id}")
    public ResponseEntity<MessageResponse<String>> updateBlockStatus(@PathVariable("id") Integer id, @RequestParam("block") Boolean status) {
        MessageResponse<String> response = new MessageResponse<>(accountService.updateBlockStatus(id, status));
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@accountSecurityService.isOwner(#accountId, authentication)")
    @PatchMapping("/close/{id}")
    public ResponseEntity<MessageResponse<String>> closeAccount(@PathVariable("id") @P("accountId") Integer id) {
        MessageResponse<String> response = new MessageResponse<>(accountService.closeAccount(id));
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('TELLER')")
    @GetMapping("/total")
    public ResponseEntity<MessageResponse<String>> getTotalAccounts(
            @RequestParam("type") AccountType type,
            @RequestParam("currency") Currency currency,
            @RequestParam(name = "city", required = false) City city) {
        MessageResponse<String> response = new MessageResponse<>(accountService.getTotalActiveAccounts(type, currency, city));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('TELLER')")
    @GetMapping("/statistics/maximum-balances")
    public ResponseEntity<MessageResponse<List<CustomerStatisticsResponse>>> getCustomerInformationWithMaximumBalance(
            @RequestParam("type") AccountType type,
            @RequestParam("currency") Currency currency) {
        MessageResponse<List<CustomerStatisticsResponse>> response = new MessageResponse<>(accountService.getCustomersHaveMaximumBalance(type, currency));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('READ_DATA') OR @accountSecurityService.isOwner(#accountId, authentication)")
    @GetMapping("/{id}/account-activities")
    public ResponseEntity<List<AccountActivityDto>> getAccountActivities(@PathVariable("id") @P("accountId") Integer id, AccountActivityFilteringRequest request) {
        AccountActivityUtil.checkFilteringRequest(request);
        return ResponseEntity.ok(accountService.getAccountActivities(id, request));
    }

    @PreAuthorize("hasAuthority('READ_DATA') OR @accountSecurityService.isOwner(#accountId, authentication)")
    @PostMapping("/{id}/statement/pdf")
    public ResponseEntity<byte[]> generateAccountStatementPdf(@PathVariable("id") @P("accountId") Integer id, AccountActivityFilteringRequest request) {
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

    @PreAuthorize("hasAuthority('READ_DATA') OR @accountSecurityService.isOwner(#accountId, authentication)")
    @PostMapping("/{id}/statement/excel")
    public ResponseEntity<byte[]> generateAccountStatementExcel(@PathVariable("id") @P("accountId") Integer id, AccountActivityFilteringRequest request) {
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
                    .filename("account_" + account.getId() + "_statement.xlsx")
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
