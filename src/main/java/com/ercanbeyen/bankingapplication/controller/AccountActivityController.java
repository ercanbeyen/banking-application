package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.enums.AttachmentFile;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.AccountActivityDto;
import com.ercanbeyen.bankingapplication.dto.response.MessageResponse;
import com.ercanbeyen.bankingapplication.security.service.AccountActivitySecurityService;
import com.ercanbeyen.bankingapplication.service.EmailService;
import com.ercanbeyen.bankingapplication.util.AccountActivityUtil;
import com.ercanbeyen.bankingapplication.view.entity.AccountActivityView;
import com.ercanbeyen.bankingapplication.dto.option.AccountActivityFilteringOption;
import com.ercanbeyen.bankingapplication.service.AccountActivityService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.List;

@RestController
@RequestMapping("/api/v1/account-activities")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class AccountActivityController {
    private final AccountActivityService accountActivityService;
    private final AccountActivitySecurityService accountActivitySecurityService;
    private final EmailService emailService;

    @PreAuthorize("hasAuthority('READ_DATA')")
    @GetMapping
    public ResponseEntity<List<AccountActivityDto>> getAccountActivities(AccountActivityFilteringOption filteringOption) {
        AccountActivityUtil.checkFilteringOption(filteringOption);
        return ResponseEntity.ok(accountActivityService.getAccountActivities(filteringOption));
    }

    @PreAuthorize("hasAuthority('READ_DATA')")
    @GetMapping("/{id}")
    public ResponseEntity<AccountActivityDto> getAccountActivity(@PathVariable("id") String id) {
        return ResponseEntity.ok(accountActivityService.getAccountActivity(id));
    }

    @PreAuthorize("hasAuthority('READ_DATA')")
    @GetMapping("/views")
    public ResponseEntity<List<AccountActivityView>> getAccountActivityViews(
            @RequestParam(name = "senderAccountId") Integer senderAccountId,
            @RequestParam(name = "recipientAccountId") Integer recipientAccountId) {
        return ResponseEntity.ok(accountActivityService.getAccountActivityViews(senderAccountId, recipientAccountId));
    }

    @PreAuthorize("hasAuthority('READ_DATA')")
    @PostMapping("/{id}/receipt/download")
    public ResponseEntity<byte[]> generateReceipt(@PathVariable("id") String id) {
        ByteArrayOutputStream outputStream = accountActivityService.generateReceiptStream(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(AccountActivityUtil.getAttachmentFileName(id, MediaType.APPLICATION_PDF_VALUE, AttachmentFile.RECEIPT))
                .build());
        headers.setContentLength(outputStream.size());

        return ResponseEntity.ok()
                .headers(headers)
                .body(outputStream.toByteArray());
    }

    @PreAuthorize("@accountActivitySecurityService.isOwner(#accountActivityId, authentication)")
    @PostMapping("/{id}/receipt/email")
    public ResponseEntity<MessageResponse<String>> sendReceipt(@PathVariable("id") @P("accountActivityId") String id, @RequestParam("to") String email) {
        ByteArrayOutputStream outputStream = accountActivityService.generateReceiptStream(id);
        AttachmentFile attachmentFile = AttachmentFile.RECEIPT;

        emailService.sendEmail(
                email,
                attachmentFile.getValue(),
                AccountActivityUtil.getAttachmentFileName(id, MediaType.APPLICATION_PDF_VALUE, attachmentFile),
                outputStream.toByteArray()
        );

        MessageResponse<String> messageResponse = new MessageResponse<>(ResponseMessage.EMAIL_SENT_SUCCESS);
        return ResponseEntity.ok(messageResponse);
    }
}
