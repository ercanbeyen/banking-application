package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.enums.*;
import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.*;
import com.ercanbeyen.bankingapplication.dto.request.AccountActivityFilteringRequest;
import com.ercanbeyen.bankingapplication.dto.response.AccountActivityPreview;
import com.ercanbeyen.bankingapplication.dto.response.CustomerFinancialSummaryResponse;
import com.ercanbeyen.bankingapplication.dto.response.ReceiptPreview;
import com.ercanbeyen.bankingapplication.embeddable.ExpectedTransaction;
import com.ercanbeyen.bankingapplication.embeddable.RegisteredRecipient;
import com.ercanbeyen.bankingapplication.entity.Customer;
import com.ercanbeyen.bankingapplication.entity.File;
import com.ercanbeyen.bankingapplication.exception.InternalServerErrorException;
import com.ercanbeyen.bankingapplication.service.EmailService;
import com.ercanbeyen.bankingapplication.util.*;
import com.ercanbeyen.bankingapplication.util.exporter.PdfExporter;
import com.ercanbeyen.bankingapplication.dto.option.AccountFilteringOption;
import com.ercanbeyen.bankingapplication.dto.option.CustomerFilteringOption;
import com.ercanbeyen.bankingapplication.dto.response.MessageResponse;
import com.ercanbeyen.bankingapplication.service.AccountService;
import com.ercanbeyen.bankingapplication.service.CustomerService;
import com.itextpdf.text.DocumentException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Range;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/customers")
@Slf4j
@SecurityRequirement(name = "Bearer Authentication")
public class CustomerController extends BaseController<CustomerDto, CustomerFilteringOption> {
    private final CustomerService customerService;
    private final AccountService accountService;
    private final EmailService emailService;

    public CustomerController(CustomerService customerService, AccountService accountService, EmailService emailService) {
        super(customerService);
        this.customerService = customerService;
        this.accountService = accountService;
        this.emailService = emailService;
    }

    @PreAuthorize("hasAuthority('READ_DATA')")
    @Override
    public ResponseEntity<List<CustomerDto>> getEntities(CustomerFilteringOption filteringOption) {
        return ResponseEntity.ok(customerService.getEntities(filteringOption));
    }

    @PreAuthorize("#customerId == authentication.principal.id")
    @GetMapping("/{id}")
    @Override
    public ResponseEntity<CustomerDto> getEntity(@PathVariable("id") @P("customerId") Integer id) {
        return ResponseEntity.ok(customerService.getEntity(id));
    }

    @PostMapping
    @Override
    public ResponseEntity<CustomerDto> createEntity(@RequestBody @Valid CustomerDto request) {
        CustomerUtil.checkRequest(request);
        return new ResponseEntity<>(customerService.createEntity(request), HttpStatus.CREATED);
    }

    @PreAuthorize("#customerId == authentication.principal.id")
    @PutMapping("/{id}")
    @Override
    public ResponseEntity<CustomerDto> updateEntity(@PathVariable("id") @P("customerId") Integer id, @RequestBody @Valid CustomerDto request) {
        CustomerUtil.checkRequest(request);
        return ResponseEntity.ok(customerService.updateEntity(id, request));
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @Override
    public ResponseEntity<Void> deleteEntity(Integer id) {
        customerService.deleteEntity(id);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAuthority('READ_DATA')")
    @GetMapping("/search")
    public ResponseEntity<CustomerDto> getCustomerByNationalId(@RequestParam(value = "nationalId") String nationalId) {
        return ResponseEntity.ok(customerService.getCustomerByNationalId(nationalId));
    }

    @PreAuthorize("#customerId == authentication.principal.id")
    @PostMapping("/{id}/agreements/{title}")
    public ResponseEntity<MessageResponse<String>> approveAgreement(@PathVariable("id") @P("customerId") Integer id, @PathVariable("title") String title) {
        customerService.approveAgreement(id, title);
        MessageResponse<String> response = new MessageResponse<>(Entity.AGREEMENT.getValue() + " is successfully approved!");
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("#customerId == authentication.principal.id")
    @PatchMapping("/{id}/registered-recipients")
    public ResponseEntity<String> addRegisteredRecipient(@PathVariable("id") @P("customerId") Integer id, @RequestBody @Valid RegisteredRecipient request) {
        return ResponseEntity.ok(customerService.addRegisteredRecipient(id, request));
    }

    @PreAuthorize("#customerId == authentication.principal.id")
    @DeleteMapping("/{id}/registered-recipients/{accountId}")
    public ResponseEntity<String> removeRegisteredRecipient(@PathVariable("id") @P("customerId") Integer id, @PathVariable("accountId") Integer accountId) {
        return ResponseEntity.ok(customerService.removeRegisteredRecipient(id, accountId));
    }

    @PreAuthorize("#customerId == authentication.principal.id")
    @PostMapping(value = "/{id}/photo/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponse<String>> uploadProfilePhoto(@PathVariable("id") @P("customerId") Integer id, @RequestParam("file") MultipartFile request) {
        PhotoUtil.checkPhoto(request);
        customerService.uploadProfilePhoto(id, request);
        MessageResponse<String> response = new MessageResponse<>(ResponseMessage.FILE_UPLOAD_APPROVAL);
        return ResponseEntity.accepted().body(response);
    }

    @PreAuthorize("#customerId == authentication.principal.id")
    @GetMapping(value = "/{id}/photo/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> downloadProfilePhoto(@PathVariable("id") @P("customerId") Integer id) {
        File file = customerService.downloadProfilePhoto(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(file.getType()));
        headers.setContentLength(file.getData().length);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(file.getName())
                .build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(file.getData());
    }

    @PreAuthorize("#customerId == authentication.principal.id")
    @DeleteMapping("/{id}/photo")
    public ResponseEntity<MessageResponse<String>> deleteProfilePhoto(@PathVariable("id") @P("customerId") Integer id) {
        customerService.deleteProfilePhoto(id);
        MessageResponse<String> response = new MessageResponse<>(String.format(ResponseMessage.DELETE_SUCCESS, Entity.FILE.getValue()));
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("#customerNationalId == authentication.principal.username")
    @GetMapping("/{nationalId}/financial-summary")
    public ResponseEntity<CustomerFinancialSummaryResponse> calculateFinancialSummary(@PathVariable("nationalId") @P("customerNationalId") String nationalId, @RequestParam("base") Currency baseCurrency) {
        return ResponseEntity.ok(customerService.calculateFinancialSummary(nationalId, baseCurrency));
    }

    @PreAuthorize("#customerId == authentication.principal.id")
    @GetMapping("/{id}/accounts")
    public ResponseEntity<List<AccountDto>> getAccounts(@PathVariable("id") @P("customerId") Integer id, AccountFilteringOption option) {
        return ResponseEntity.ok(customerService.getAccounts(id, option));
    }

    @PreAuthorize("#customerId == authentication.principal.id")
    @GetMapping("/{id}/accounts/receipt-previews")
    public ResponseEntity<List<ReceiptPreview>> getReceiptPreviews(@PathVariable("id") @P("customerId") Integer id) {
        AccountActivityFilteringRequest request = new AccountActivityFilteringRequest(null, null, null, null, null, Arrays.asList(Channel.values()));
        SortedSet<AccountActivityPreview> accountActivityPreviews = new TreeSet<>(Comparator.comparing(AccountActivityPreview::createdAt).reversed());

        customerService.findById(id)
                .getAccounts()
                .forEach(account -> accountActivityPreviews.addAll(accountService.getAccountActivityPreviews(account.getId(), request)));

        List<ReceiptPreview> receiptPreviews = accountActivityPreviews.stream()
                .map(accountActivityPreview -> new ReceiptPreview(accountActivityPreview.accountActivityId(), accountActivityPreview.accountActivityType(), accountActivityPreview.createdAt(), accountActivityPreview.amount()))
                .toList();

        return ResponseEntity.ok(receiptPreviews);
    }

    @PreAuthorize("#customerId == authentication.principal.id")
    @GetMapping("/{id}/notifications")
    public ResponseEntity<List<NotificationDto>> getNotifications(@PathVariable("id") @P("customerId") Integer id) {
        return ResponseEntity.ok(customerService.getNotifications(id));
    }

    @PreAuthorize("#customerId == authentication.principal.id")
    @GetMapping("/{id}/money-transfer-orders")
    public ResponseEntity<List<MoneyTransferOrderDto>> getMoneyTransferOrders(
            @PathVariable("id") @P("customerId") Integer id,
            @RequestParam("from") LocalDate fromDate,
            @RequestParam("to") LocalDate toDate,
            @RequestParam(value = "currency", required = false) Currency currency,
            @RequestParam(value = "payment-type", required = false) PaymentType paymentType) {
        MoneyTransferOrderUtil.checkDatesBeforeFiltering(fromDate, toDate);
        return ResponseEntity.ok(customerService.getMoneyTransferOrders(id, fromDate, toDate, currency, paymentType));
    }

    @PreAuthorize("#customerId == authentication.principal.id")
    @GetMapping("/{id}/cash-flow-calendar")
    public ResponseEntity<CashFlowCalendarDto> getCashFlowCalendar(
            @PathVariable("id") @P("customerId") Integer id,
            @RequestParam("year") Integer year,
            @RequestParam("month") @Range(min = 1, max = 12, message = "Month should be between {min} and {max}") Integer month) {
        CashFlowCalendarUtil.checkMonthAndYearForCashFlowCalendar(year, month);
        return ResponseEntity.ok(customerService.getCashFlowCalendar(id, year, month));
    }

    @PreAuthorize("#customerId == authentication.principal.id")
    @GetMapping("/{id}/expected-transactions")
    public ResponseEntity<List<ExpectedTransaction>> getExpectedTransactions(@PathVariable("id") @P("customerId") Integer id, @RequestParam(value = "month", defaultValue = "1") Integer month) {
        CashFlowCalendarUtil.checkMonthValueForExpectedTransactions(month);
        return ResponseEntity.ok(customerService.getExpectedTransactions(id, month));
    }

    @PreAuthorize("#customerId == authentication.principal.id")
    @GetMapping("/{id}/agreements")
    public ResponseEntity<List<CustomerAgreementDto>> getAgreements(@PathVariable("id") @P("customerId") Integer id) {
        return ResponseEntity.ok(customerService.getAgreements(id));
    }

    @PreAuthorize("#customerId == authentication.principal.id")
    @GetMapping("/{id}/registered-recipients")
    public ResponseEntity<List<RegisteredRecipient>> getRegisteredRecipients(@PathVariable("id") @P("customerId") Integer id) {
        return ResponseEntity.ok(customerService.getRegisteredRecipients(id));
    }

    @PreAuthorize("hasAuthority('READ_DATA')")
    @PostMapping(value = "/{id}/financial-status/report/pdf/download", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadFinancialStatusReportPdf(@PathVariable("id") Integer id) {
        ByteArrayOutputStream byteArrayOutputStream = generateFinancialStatusReportPdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentLength(byteArrayOutputStream.size());
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(AccountActivityUtil.getAttachmentFileName(Integer.toString(id), MediaType.APPLICATION_PDF_VALUE, AttachmentFile.FINANCIAL_STATUS_REPORT))
                .build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(byteArrayOutputStream.toByteArray());
    }

    @PreAuthorize("#customerId == authentication.principal.id")
    @PostMapping(value = "/{id}/financial-status/report/pdf/email")
    public ResponseEntity<MessageResponse<String>> sendFinancialStatusReportPdf(@PathVariable("id") @P("customerId") Integer id, @RequestParam("to") String email) {
        ByteArrayOutputStream byteArrayOutputStream = generateFinancialStatusReportPdf(id);
        AttachmentFile attachmentFile = AttachmentFile.FINANCIAL_STATUS_REPORT;

        emailService.sendEmail(
                email,
                attachmentFile.getValue(),
                AccountActivityUtil.getAttachmentFileName(Integer.toString(id), MediaType.APPLICATION_PDF_VALUE, attachmentFile),
                byteArrayOutputStream.toByteArray()
        );

        MessageResponse<String> messageResponse = new MessageResponse<>(ResponseMessage.EMAIL_SENT_SUCCESS);
        return ResponseEntity.ok(messageResponse);
    }

    private ByteArrayOutputStream generateFinancialStatusReportPdf(Integer id) {
        Customer customer = customerService.findById(id);
        Double netBalanceOfCustomer = customerService.calculateNetBalance(id, null, Currency.getDeductionCurrency());
        Map<AccountType, List<List<AccountFinancialStatus>>> accountFinancialStatusesWithConvertedCurrencies = customerService.calculateFinancialStatus(id);
        Map<AccountType, Double> accountTypeNetBalancesWithConvertedCurrencies = new EnumMap<>(AccountType.class);

        for (Map.Entry<AccountType, List<List<AccountFinancialStatus>>> financialStatusOfAccountTypesWithConvertedCurrency : accountFinancialStatusesWithConvertedCurrencies.entrySet()) {
            AccountType accountType = financialStatusOfAccountTypesWithConvertedCurrency.getKey();
            Double balance = customerService.calculateNetBalance(id, accountType, Currency.getDeductionCurrency());
            accountTypeNetBalancesWithConvertedCurrencies.put(accountType, balance);
        }

        try {
            return PdfExporter.generatePdfStreamOfFinancialStatusReport(customer, netBalanceOfCustomer, accountTypeNetBalancesWithConvertedCurrencies, accountFinancialStatusesWithConvertedCurrencies);
        } catch (DocumentException | IOException exception) {
            throw new InternalServerErrorException(exception.getMessage());
        }
    }
}
