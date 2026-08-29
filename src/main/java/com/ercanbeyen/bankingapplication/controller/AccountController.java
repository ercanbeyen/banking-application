package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.enums.*;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.AccountDto;
import com.ercanbeyen.bankingapplication.dto.request.AccountActivityFilteringRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyExchangeRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyTransferRequest;
import com.ercanbeyen.bankingapplication.dto.response.AccountActivityPreview;
import com.ercanbeyen.bankingapplication.embeddable.Address;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.exception.InternalServerErrorException;
import com.ercanbeyen.bankingapplication.dto.option.AccountFilteringOption;
import com.ercanbeyen.bankingapplication.dto.response.MessageResponse;
import com.ercanbeyen.bankingapplication.dto.response.CustomerStatisticsResponse;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.security.service.AccountSecurityService;
import com.ercanbeyen.bankingapplication.service.AccountService;
import com.ercanbeyen.bankingapplication.service.EmailService;
import com.ercanbeyen.bankingapplication.service.TimeZoneService;
import com.ercanbeyen.bankingapplication.util.exporter.ExcelExporter;
import com.ercanbeyen.bankingapplication.util.exporter.PdfExporter;
import com.ercanbeyen.bankingapplication.util.AccountActivityUtil;
import com.ercanbeyen.bankingapplication.util.AccountUtil;
import com.itextpdf.text.DocumentException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

@RestController
@RequestMapping("/api/v1/accounts")
@Slf4j
@SecurityRequirement(name = "Bearer Authentication")
public class AccountController extends BaseController<AccountDto, AccountFilteringOption> {
    private final AccountService accountService;
    private final AccountSecurityService accountSecurityService;
    private final EmailService emailService;
    private final TimeZoneService timeZoneService;

    public AccountController(AccountService accountService, AccountSecurityService accountSecurityService, EmailService emailService, TimeZoneService timeZoneService) {
        super(accountService);
        this.accountService = accountService;
        this.accountSecurityService = accountSecurityService;
        this.emailService = emailService;
        this.timeZoneService = timeZoneService;
    }

    @PreAuthorize("hasAuthority('READ_DATA')")
    @GetMapping
    @Override
    public ResponseEntity<List<AccountDto>> getEntities(AccountFilteringOption filteringOption) {
        return ResponseEntity.ok(accountService.getEntities(filteringOption));
    }

    @PostAuthorize("returnObject.body.customerNationalId == authentication.principal.username OR hasAuthority('READ_DATA')")
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
    @PutMapping("{id}/deposit")
    public ResponseEntity<MessageResponse<String>> depositMoney(
            @PathVariable("id") @P("accountId") Integer id,
            @RequestParam("amount") @Valid @Min(value = 1, message = "Minimum amount should be {value}") Double amount,
            HttpServletRequest httpServletRequest) {
        AccountUtil.checkMoneyDepositAndWithdrawalRequests(httpServletRequest);
        accountService.depositMoney(id, amount, httpServletRequest);
        MessageResponse<String> response = new MessageResponse<>(String.format(ResponseMessage.SUCCESS, AccountActivityType.MONEY_DEPOSIT.getValue()));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PreAuthorize("@accountSecurityService.isOwner(#accountId, authentication)")
    @PutMapping("{id}/withdrawal")
    public ResponseEntity<MessageResponse<String>> withdrawMoney(
            @PathVariable("id") @P("accountId") Integer id,
            @RequestParam("amount") @Valid @Min(value = 1, message = "Minimum amount should be {value}") Double amount,
            HttpServletRequest httpServletRequest) {
        AccountUtil.checkMoneyDepositAndWithdrawalRequests(httpServletRequest);
        accountService.withdrawMoney(id, amount, httpServletRequest);
        MessageResponse<String> response = new MessageResponse<>(String.format(ResponseMessage.SUCCESS, AccountActivityType.WITHDRAWAL.getValue()));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @PutMapping("/{id}/pay/interest-income")
    public ResponseEntity<MessageResponse<String>> payInterestIncome(@PathVariable("id") Integer id) {
        MessageResponse<String> response = new MessageResponse<>(accountService.payInterestIncome(id));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PreAuthorize("@accountSecurityService.isOwner(#moneyTransfer.senderAccountId, authentication) OR hasRole('ADMIN')")
    @PutMapping("/transfer")
    public ResponseEntity<MessageResponse<String>> transferMoney(@RequestBody @Valid @P("moneyTransfer") MoneyTransferRequest moneyTransferRequest, HttpServletRequest httpServletRequest) {
        AccountUtil.checkMoneyTransferRequest(moneyTransferRequest, httpServletRequest);
        accountService.transferMoney(moneyTransferRequest, httpServletRequest);
        MessageResponse<String> response = new MessageResponse<>(String.format(ResponseMessage.SUCCESS, AccountActivityType.MONEY_TRANSFER.getValue()));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PreAuthorize("@accountSecurityService.isOwner(#moneyExchange.sellerAccountId, authentication)")
    @PutMapping("/exchange")
    public ResponseEntity<MessageResponse<String>> exchangeMoney(@RequestBody @Valid @P("moneyExchange") MoneyExchangeRequest moneyExchangeRequest, HttpServletRequest httpServletRequest) {
        AccountUtil.checkMoneyExchangeRequest(moneyExchangeRequest, httpServletRequest);
        accountService.exchangeMoney(moneyExchangeRequest, httpServletRequest);
        MessageResponse<String> response = new MessageResponse<>(String.format(ResponseMessage.SUCCESS, AccountActivityType.MONEY_EXCHANGE.getValue()));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/block")
    public ResponseEntity<MessageResponse<String>> updateBlockStatus(@PathVariable("id") Integer id, @RequestParam("status") Boolean status) {
        MessageResponse<String> response = new MessageResponse<>(accountService.updateBlockStatus(id, status));
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@accountSecurityService.isOwner(#accountId, authentication)")
    @PatchMapping("/{id}/close")
    public ResponseEntity<MessageResponse<String>> closeAccount(@PathVariable("id") @P("accountId") Integer id) {
        accountService.closeAccount(id);
        MessageResponse<String> response = new MessageResponse<>(String.format(ResponseMessage.SUCCESS, AccountActivityType.ACCOUNT_CLOSING.getValue()));
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('TELLER')")
    @GetMapping("/total")
    public ResponseEntity<MessageResponse<String>> getTotalAccounts(
            @RequestParam("type") AccountType type,
            @RequestParam("currency") Currency currency,
            @RequestParam(name = "city", required = false) String city) {
        Integer count = accountService.getTotalActiveAccounts(type, currency, city);
        MessageResponse<String> response = new MessageResponse<>(String.format("Total %s %s accounts is %d", type.getValue(), currency, count));
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
    @GetMapping("/{id}/account-activity-previews")
    public ResponseEntity<List<AccountActivityPreview>> getAccountActivityPreviews(@PathVariable("id") @P("accountId") Integer id, AccountActivityFilteringRequest request) {
        AccountActivityUtil.checkFilteringRequest(request);
        return ResponseEntity.ok(accountService.getAccountActivityPreviews(id, request));
    }

    @PreAuthorize("hasAuthority('READ_DATA')")
    @PostMapping(value = "/{id}/statement/pdf/download", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadAccountStatementPdf(@PathVariable("id") @P("accountId") Integer id, AccountActivityFilteringRequest request) {
        ByteArrayOutputStream byteArrayOutputStream = generateAccountStatementPdf(id, request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentLength(byteArrayOutputStream.size());
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(AccountActivityUtil.getAttachmentFileName(Integer.toString(id), MediaType.APPLICATION_PDF_VALUE, AttachmentFile.ACCOUNT_STATEMENT))
                .build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(byteArrayOutputStream.toByteArray());
    }

    @PreAuthorize("hasAuthority('READ_DATA')")
    @PostMapping(value = "/{id}/statement/excel/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> downloadAccountStatementExcel(@PathVariable("id") @P("accountId") Integer id, AccountActivityFilteringRequest request) {
        ByteArrayOutputStream byteArrayOutputStream = generateAccountStatementExcel(id, request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentLength(byteArrayOutputStream.size());
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(AccountActivityUtil.getAttachmentFileName(Integer.toString(id), MediaType.APPLICATION_OCTET_STREAM_VALUE, AttachmentFile.ACCOUNT_STATEMENT))
                .build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(byteArrayOutputStream.toByteArray());
    }

    @PreAuthorize("@accountSecurityService.isOwner(#accountId, authentication)")
    @PostMapping("/{id}/statement/pdf/email")
    public ResponseEntity<MessageResponse<String>> sendAccountStatementPdf(@PathVariable("id") @P("accountId") Integer id, @RequestParam("to") String email, AccountActivityFilteringRequest request) {
        ByteArrayOutputStream byteArrayOutputStream = generateAccountStatementPdf(id, request);
        AttachmentFile attachmentFile = AttachmentFile.ACCOUNT_STATEMENT;

        emailService.sendEmail(
                email,
                attachmentFile.getValue(),
                AccountActivityUtil.getAttachmentFileName(Integer.toString(id), MediaType.APPLICATION_PDF_VALUE, attachmentFile),
                byteArrayOutputStream.toByteArray()
        );

        MessageResponse<String> messageResponse = new MessageResponse<>(ResponseMessage.EMAIL_SENT_SUCCESS);
        return ResponseEntity.ok(messageResponse);
    }

    @PreAuthorize("@accountSecurityService.isOwner(#accountId, authentication)")
    @PostMapping("/{id}/statement/excel/email")
    public ResponseEntity<MessageResponse<String>> sendAccountStatementExcel(@PathVariable("id") @P("accountId") Integer id, @RequestParam("to") String email, AccountActivityFilteringRequest request) {
        ByteArrayOutputStream byteArrayOutputStream = generateAccountStatementExcel(id, request);
        AttachmentFile attachmentFile = AttachmentFile.ACCOUNT_STATEMENT;

        emailService.sendEmail(
                email,
                attachmentFile.getValue(),
                AccountActivityUtil.getAttachmentFileName(Integer.toString(id), MediaType.APPLICATION_OCTET_STREAM_VALUE, attachmentFile),
                byteArrayOutputStream.toByteArray()
        );

        MessageResponse<String> messageResponse = new MessageResponse<>(ResponseMessage.EMAIL_SENT_SUCCESS);
        return ResponseEntity.ok(messageResponse);
    }

    private ByteArrayOutputStream generateAccountStatementPdf(Integer id, AccountActivityFilteringRequest request) {
        AccountActivityUtil.checkFilteringRequest(request);

        List<AccountActivityPreview> accountActivityPreviews = accountService.getAccountActivityPreviews(id, request);
        Account account = accountService.findById(id);

        ZoneId zoneId = getTimeZoneOfBranch(account.getBranch().getAddress());

        LocalDate fromDate = fillDateInFilteringRequest.apply(request.fromDate());
        LocalDate toDate = fillDateInFilteringRequest.apply(request.toDate());

        try {
            return PdfExporter.generateAccountStatementPdf(account, zoneId, fromDate, toDate, accountActivityPreviews);
        } catch (DocumentException | IOException exception) {
            throw new InternalServerErrorException(exception.getMessage());
        }
    }

    private ByteArrayOutputStream generateAccountStatementExcel(Integer id, AccountActivityFilteringRequest request) {
        AccountActivityUtil.checkFilteringRequest(request);

        List<AccountActivityPreview> accountActivityPreviews = accountService.getAccountActivityPreviews(id, request);
        Account account = accountService.findById(id);

        ZoneId zoneId = getTimeZoneOfBranch(account.getBranch().getAddress());

        LocalDate fromDate = fillDateInFilteringRequest.apply(request.fromDate());
        LocalDate toDate = fillDateInFilteringRequest.apply(request.toDate());

        try (Workbook workbook = ExcelExporter.generateAccountStatementWorkbook(account, zoneId, accountActivityPreviews, fromDate, toDate); ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            workbook.write(byteArrayOutputStream);
            return byteArrayOutputStream;
        } catch (IOException exception) {
            throw new InternalServerErrorException(exception.getMessage());
        }
    }

    private ZoneId getTimeZoneOfBranch(Address address) {
        return timeZoneService.getZoneId(address.getCountry(), address.getCity())
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, "Time Zone of branch")));
    }

    private final UnaryOperator<LocalDate> fillDateInFilteringRequest = request -> Optional.ofNullable(request).isPresent() ? request : LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault());
}
