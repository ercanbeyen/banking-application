package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.enums.*;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.constant.query.HeaderField;
import com.ercanbeyen.bankingapplication.dto.AccountDto;
import com.ercanbeyen.bankingapplication.dto.BranchOrderDto;
import com.ercanbeyen.bankingapplication.dto.BranchOrderInformation;
import com.ercanbeyen.bankingapplication.dto.ChannelInformation;
import com.ercanbeyen.bankingapplication.dto.request.AccountActivityFilteringRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyExchangeRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyTransferRequest;
import com.ercanbeyen.bankingapplication.dto.response.AccountActivityPreview;
import com.ercanbeyen.bankingapplication.embeddable.Address;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import com.ercanbeyen.bankingapplication.exception.InternalServerErrorException;
import com.ercanbeyen.bankingapplication.dto.option.AccountFilteringOption;
import com.ercanbeyen.bankingapplication.dto.response.MessageResponse;
import com.ercanbeyen.bankingapplication.dto.response.CustomerStatisticsResponse;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.helper.DailyActivityLimitHelper;
import com.ercanbeyen.bankingapplication.security.service.AccountSecurityService;
import com.ercanbeyen.bankingapplication.security.util.UserDetailsUtil;
import com.ercanbeyen.bankingapplication.service.AccountService;
import com.ercanbeyen.bankingapplication.service.BranchOrderService;
import com.ercanbeyen.bankingapplication.service.EmailService;
import com.ercanbeyen.bankingapplication.service.TimeZoneService;
import com.ercanbeyen.bankingapplication.util.exporter.ExcelExporter;
import com.ercanbeyen.bankingapplication.util.exporter.PdfExporter;
import com.ercanbeyen.bankingapplication.util.AccountActivityUtil;
import com.ercanbeyen.bankingapplication.util.AccountUtil;
import com.itextpdf.text.DocumentException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.parameters.P;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

@RestController
@RequestMapping("/api/v1/accounts")
@Slf4j
@SecurityRequirement(name = "Bearer Authentication")
public class AccountController extends BaseController<AccountDto, AccountFilteringOption> {
    private final AccountService accountService;
    private final AccountSecurityService accountSecurityService;
    private final DailyActivityLimitHelper dailyActivityLimitHelper;
    private final BranchOrderService branchOrderService;
    private final EmailService emailService;
    private final TimeZoneService timeZoneService;


    public AccountController(
            AccountService accountService,
            AccountSecurityService accountSecurityService,
            DailyActivityLimitHelper dailyActivityLimitHelper,
            BranchOrderService branchOrderService,
            EmailService emailService,
            TimeZoneService timeZoneService) {
        super(accountService);
        this.accountService = accountService;
        this.accountSecurityService = accountSecurityService;
        this.dailyActivityLimitHelper = dailyActivityLimitHelper;
        this.branchOrderService = branchOrderService;
        this.emailService = emailService;
        this.timeZoneService = timeZoneService;
    }

    @PreAuthorize("hasAuthority('READ_DATA')")
    @GetMapping
    @Override
    public ResponseEntity<List<AccountDto>> getEntities(AccountFilteringOption filteringOption) {
        return ResponseEntity.ok(accountService.getEntities(filteringOption));
    }

    @PostAuthorize("hasAuthority('READ_DATA') OR returnObject.body.customerNationalId == authentication.principal.username")
    @GetMapping("/{id}")
    @Override
    public ResponseEntity<AccountDto> getEntity(@PathVariable("id") Integer id) {
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

    @PreAuthorize("hasRole('TELLER') OR @accountSecurityService.isOwner(#accountId, authentication)")
    @PutMapping("{id}/deposit")
    public ResponseEntity<MessageResponse<String>> depositMoney(
            @PathVariable(value = "id") @P(value = "accountId") Integer id,
            @RequestParam(value = "amount") @Valid @Min(value = 1, message = "Minimum amount should be {value}") Double amount,
            @RequestParam(value = "branch-order-id", required = false) String branchOrderId,
            @RequestParam(value = "third-party-national-id", required = false) String thirdPartyNationalId,
            @RequestHeader(value = HeaderField.CHANNEL_TYPE) ChannelType channelType,
            @RequestHeader(value = HeaderField.CHANNEL_ID) Integer channelId) {
        ActivityType activityType = Optional.ofNullable(branchOrderId).isPresent() ? ActivityType.THIRD_PARTY_MONEY_DEPOSIT : ActivityType.MONEY_DEPOSIT;
        BranchOrderInformation branchOrderInformation = constructBranchOrderInformationForMoneyDepositAndWithdrawal(branchOrderId, activityType, thirdPartyNationalId);

        checkThirdPersonNationalId(thirdPartyNationalId, activityType);
        checkAccountActivityRequestWithBranchOrder(id, branchOrderId, activityType);

        ChannelInformation channelInformation = new ChannelInformation(channelId, channelType);
        AccountUtil.checkAccountActivityWithChannelType(channelInformation, activityType);

        dailyActivityLimitHelper.checkActivityLimits(activityType, amount, channelType);

        accountService.depositMoney(id, activityType, amount, channelInformation, branchOrderInformation);

        MessageResponse<String> response = new MessageResponse<>(String.format(ResponseMessage.SUCCESS, activityType.getValue()));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('TELLER') OR @accountSecurityService.isOwner(#accountId, authentication)")
    @PutMapping("{id}/withdrawal")
    public ResponseEntity<MessageResponse<String>> withdrawMoney(
            @PathVariable(value = "id") @P(value = "accountId") Integer id,
            @RequestParam(value = "amount") @Valid @Min(value = 1, message = "Minimum amount should be {value}") Double amount,
            @RequestParam(value = "branch-order-id", required = false) String branchOrderId,
            @RequestParam(value = "third-party-national-id", required = false) String thirdPartyNationalId,
            @RequestHeader(HeaderField.CHANNEL_TYPE) ChannelType channelType,
            @RequestHeader(HeaderField.CHANNEL_ID) Integer channelId) {
        ActivityType activityType = Optional.ofNullable(branchOrderId).isPresent() ? ActivityType.THIRD_PARTY_WITHDRAWAL : ActivityType.WITHDRAWAL;
        BranchOrderInformation branchOrderInformation = constructBranchOrderInformationForMoneyDepositAndWithdrawal(branchOrderId, activityType, thirdPartyNationalId);

        checkThirdPersonNationalId(thirdPartyNationalId, activityType);
        checkAccountActivityRequestWithBranchOrder(id, branchOrderId, activityType);

        ChannelInformation channelInformation = new ChannelInformation(channelId, channelType);
        AccountUtil.checkAccountActivityWithChannelType(channelInformation, activityType);

        dailyActivityLimitHelper.checkActivityLimits(activityType, amount, channelType);

        accountService.withdrawMoney(id, activityType, amount, channelInformation, branchOrderInformation);

        MessageResponse<String> response = new MessageResponse<>(String.format(ResponseMessage.SUCCESS, activityType.getValue()));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('MANAGE_ENTITY')")
    @PutMapping("/{id}/pay/interest-income")
    public ResponseEntity<MessageResponse<String>> payInterestIncome(@PathVariable("id") Integer id) {
        MessageResponse<String> response = new MessageResponse<>(accountService.payInterestIncome(id));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('TELLER') OR @accountSecurityService.isOwner(#moneyTransfer.senderAccountId, authentication)")
    @PutMapping("/transfer")
    public ResponseEntity<MessageResponse<String>> transferMoney(
            @RequestBody @Valid @P(value = "moneyTransfer") MoneyTransferRequest request,
            @RequestParam(value = "branch-order-id", required = false) String branchOrderId,
            @RequestHeader(value = HeaderField.CHANNEL_TYPE) ChannelType channelType,
            @RequestHeader(value = HeaderField.CHANNEL_ID, required = false) Integer channelId) {
        ActivityType activityType = ActivityType.MONEY_TRANSFER;
        checkAccountActivityRequestWithBranchOrder(request.senderAccountId(), branchOrderId, activityType);

        BranchOrderInformation branchOrderInformation = constructBranchOrderInformationForMoneyTransferAndMoneyExchange.apply(branchOrderId, activityType);
        ChannelInformation channelInformation = new ChannelInformation(channelId, channelType);

        AccountUtil.checkMoneyTransferRequest(request, channelInformation);
        dailyActivityLimitHelper.checkActivityLimits(activityType, request.amount(), channelType);

        accountService.transferMoney(request, channelInformation, branchOrderInformation);
        MessageResponse<String> response = new MessageResponse<>(String.format(ResponseMessage.SUCCESS, activityType.getValue()));

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('TELLER') OR @accountSecurityService.isOwner(#moneyExchange.sellerAccountId, authentication)")
    @PutMapping("/exchange")
    public ResponseEntity<MessageResponse<String>> exchangeMoney(
            @RequestBody @Valid @P(value = "moneyExchange") MoneyExchangeRequest request,
            @RequestParam(value = "branch-order-id", required = false) String branchOrderId,
            @RequestHeader(value = HeaderField.CHANNEL_TYPE) ChannelType channelType,
            @RequestHeader(value = HeaderField.CHANNEL_ID, required = false) Integer channelId) {
        ActivityType activityType = ActivityType.MONEY_EXCHANGE;
        checkAccountActivityRequestWithBranchOrder(request.sellerAccountId(), branchOrderId, activityType);

        BranchOrderInformation branchOrderInformation = constructBranchOrderInformationForMoneyTransferAndMoneyExchange.apply(branchOrderId, activityType);
        ChannelInformation channelInformation = new ChannelInformation(channelId, channelType);

        AccountUtil.checkMoneyExchangeRequest(request, channelInformation);
        dailyActivityLimitHelper.checkActivityLimits(activityType, request.amount(), channelType);

        accountService.exchangeMoney(request, channelInformation, branchOrderInformation);
        MessageResponse<String> response = new MessageResponse<>(String.format(ResponseMessage.SUCCESS, activityType.getValue()));

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/block")
    public ResponseEntity<MessageResponse<String>> updateBlockStatus(@PathVariable("id") Integer id, @RequestParam("status") Boolean status) {
        accountService.updateBlockStatus(id, status);
        MessageResponse<String> response = new MessageResponse<>(String.format("Blocked status of %s is successfully updated!", Entity.ACCOUNT.getValue().toLowerCase()));
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@accountSecurityService.isOwner(#accountId, authentication)")
    @PatchMapping("/{id}/close")
    public ResponseEntity<MessageResponse<String>> closeAccount(@PathVariable("id") @P("accountId") Integer id) {
        accountService.closeAccount(id);
        MessageResponse<String> response = new MessageResponse<>(String.format("%s is successfully closed!", Entity.ACCOUNT.getValue()));
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('TELLER')")
    @GetMapping("/total")
    public ResponseEntity<MessageResponse<String>> getTotalAccounts(
            @RequestParam("type") AccountType type,
            @RequestParam("currency") Currency currency,
            @RequestParam(name = "city", required = false) String city) {
        Integer count = accountService.getTotalActiveAccounts(type, currency, city);
        MessageResponse<String> response = new MessageResponse<>(String.format("Total %s %s %ss is %d", type.getValue(), currency, Entity.ACCOUNT.getValue().toLowerCase(), count));
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

    private void checkAccountActivityRequestWithBranchOrder(Integer accountId, String branchOrderId, ActivityType expectedActivityType) {
        if (Optional.ofNullable(branchOrderId).isEmpty()) {
            return;
        }
        BranchOrderDto branchOrderDto = branchOrderService.getBranchOrder(branchOrderId);

        if (branchOrderDto.activityType() != expectedActivityType) {
            throw new ResourceConflictException("Activity types are mismatching!");
        }


        if (branchOrderDto.status() != BranchOrderStatus.IN_PROCESS) {
            throw new ResourceConflictException(ResponseMessage.BRANCH_ORDER_STATUS_VIOLATED);
        }

        AccountDto accountDto = accountService.getEntity(accountId);

        if (!accountDto.getCustomerNationalId().equals(branchOrderDto.customerNationalId())) {
            throw new ResourceConflictException(String.format("%s does not belong to %s", Entity.ACCOUNT.getValue(), Entity.CUSTOMER.getValue()));
        }


        UserDetails userDetails = UserDetailsUtil.getUserDetails(Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()));
        Set<ERole> eRoles = UserDetailsUtil.getERoles(userDetails);

        if (!eRoles.contains(ERole.TELLER)) {
            throw new AccessDeniedException(String.format(ResponseMessage.ACCESS_DENIED, ResponseMessage.USER_CANNOT_ACCESS_TO_ACCOUNT));
        }
    }

    private BranchOrderInformation constructBranchOrderInformationForMoneyDepositAndWithdrawal(String branchOrderId, ActivityType activityType, String thirdPartyNationalId) {
        if (!ActivityType.getThirdPartyActivities().contains(activityType)) {
            return null;
        }

        return new BranchOrderInformation(branchOrderId, activityType, thirdPartyNationalId);
    }

    private ZoneId getTimeZoneOfBranch(Address address) {
        return timeZoneService.getZoneId(address.getCountry(), address.getCity())
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, "Time Zone of branch")));
    }

    private static void checkThirdPersonNationalId(String nationalId, ActivityType activityType) {
        if (Optional.ofNullable(nationalId).isEmpty() && ActivityType.getThirdPartyActivities().contains(activityType)) {
            throw new BadRequestException("Third party national id must be present!");
        }
    }

    private final BiFunction<String, ActivityType, BranchOrderInformation> constructBranchOrderInformationForMoneyTransferAndMoneyExchange = (branchOrderId, activityType) -> Optional.ofNullable(branchOrderId).isPresent() ? new BranchOrderInformation(branchOrderId, activityType, null) : null;
    private final UnaryOperator<LocalDate> fillDateInFilteringRequest = request -> Optional.ofNullable(request).isPresent() ? request : LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault());
}
