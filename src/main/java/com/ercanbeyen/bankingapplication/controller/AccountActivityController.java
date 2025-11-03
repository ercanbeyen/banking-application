package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.dto.AccountActivityDto;
import com.ercanbeyen.bankingapplication.view.entity.AccountActivityView;
import com.ercanbeyen.bankingapplication.option.AccountActivityFilteringOption;
import com.ercanbeyen.bankingapplication.service.AccountActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.List;

@RestController
@RequestMapping("/api/v1/account-activities")
@RequiredArgsConstructor
public class AccountActivityController {
    private final AccountActivityService accountActivityService;

    @GetMapping
    public ResponseEntity<List<AccountActivityDto>> getAccountActivities(AccountActivityFilteringOption filteringOption) {
        return ResponseEntity.ok(accountActivityService.getAccountActivities(filteringOption));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountActivityDto> getAccountActivity(@PathVariable("id") String id) {
        return ResponseEntity.ok(accountActivityService.getAccountActivity(id));
    }

    @GetMapping("/views")
    public ResponseEntity<List<AccountActivityView>> getAccountActivityViews(
            @RequestParam(name = "senderAccountId") Integer senderAccountId,
            @RequestParam(name = "recipientAccountId") Integer recipientAccountId) {
        return ResponseEntity.ok(accountActivityService.getAccountActivityViews(senderAccountId, recipientAccountId));
    }

    @PostMapping("/{id}/receipt")
    public ResponseEntity<byte[]> generateReceipt(@PathVariable("id") String id) {
        /* Export pdf from Account Activity's summary */
        ByteArrayOutputStream receiptStream = accountActivityService.createReceiptStream(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receipt.pdf");
        headers.setContentLength(receiptStream.size());

        return new ResponseEntity<>(receiptStream.toByteArray(), headers, HttpStatus.OK);
    }
}
