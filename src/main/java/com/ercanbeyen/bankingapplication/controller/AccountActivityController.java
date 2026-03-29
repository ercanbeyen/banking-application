package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.dto.AccountActivityDto;
import com.ercanbeyen.bankingapplication.security.service.AccountActivitySecurityService;
import com.ercanbeyen.bankingapplication.util.AccountActivityUtil;
import com.ercanbeyen.bankingapplication.view.entity.AccountActivityView;
import com.ercanbeyen.bankingapplication.dto.option.AccountActivityFilteringOption;
import com.ercanbeyen.bankingapplication.service.AccountActivityService;
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
public class AccountActivityController {
    private final AccountActivityService accountActivityService;
    private final AccountActivitySecurityService accountActivitySecurityService;

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

    @PreAuthorize("hasAuthority('READ_DATA') OR @accountActivitySecurityService.isOwner(#accountActivityId, authentication)")
    @PostMapping("/{id}/receipt")
    public ResponseEntity<byte[]> generateReceipt(@PathVariable("id") @P("accountActivityId") String id) {
        ByteArrayOutputStream receiptStream = accountActivityService.generateReceiptStream(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("receipt.pdf")
                .build());
        headers.setContentLength(receiptStream.size());

        return ResponseEntity.ok()
                .headers(headers)
                .body(receiptStream.toByteArray());
    }
}
