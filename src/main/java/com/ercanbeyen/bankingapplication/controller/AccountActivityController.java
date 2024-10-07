package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.dto.AccountActivityDto;
import com.ercanbeyen.bankingapplication.view.entity.AccountActivityView;
import com.ercanbeyen.bankingapplication.option.AccountActivityFilteringOptions;
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
    public ResponseEntity<List<AccountActivityDto>> getAccountActivities(AccountActivityFilteringOptions options) {
        List<AccountActivityDto> accountActivityDtos = accountActivityService.getAccountActivities(options);
        return ResponseEntity.ok(accountActivityDtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountActivityDto> getAccountActivity(@PathVariable("id") String id) {
        AccountActivityDto accountActivityDto = accountActivityService.getAccountActivity(id);
        return ResponseEntity.ok(accountActivityDto);
    }

    @GetMapping("/views")
    public ResponseEntity<List<AccountActivityView>> getAccountActivityViews(
            @RequestParam(name = "senderAccountId") Integer senderAccountId,
            @RequestParam(name = "receiverAccountId") Integer receiverAccountId) {
        List<AccountActivityView> accountActivityViews = accountActivityService.getAccountActivityViews(senderAccountId, receiverAccountId);
        return ResponseEntity.ok(accountActivityViews);
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
