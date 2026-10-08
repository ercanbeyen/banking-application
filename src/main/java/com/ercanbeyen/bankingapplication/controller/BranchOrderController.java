package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.enums.ActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.BranchOrderStatus;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.dto.BranchOrderDto;
import com.ercanbeyen.bankingapplication.dto.response.MessageResponse;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import com.ercanbeyen.bankingapplication.security.service.BranchOrderSecurityService;
import com.ercanbeyen.bankingapplication.service.BranchOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/branch-orders")
@RequiredArgsConstructor
public class BranchOrderController {
    private final BranchOrderService branchOrderService;
    private final BranchOrderSecurityService branchOrderSecurityService;

    @PreAuthorize("#branchOrder.customerNationalId == authentication.principal.username")
    @PostMapping
    public ResponseEntity<BranchOrderDto> createBranchOrder(@Valid @RequestBody @P("branchOrder") BranchOrderDto request) {
        checkRequest(request);
        return new ResponseEntity<>(branchOrderService.createBranchOrder(request), HttpStatus.CREATED);
    }

    @PreAuthorize("#branchOrder.customerNationalId == authentication.principal.username")
    @PutMapping("/{id}")
    public ResponseEntity<BranchOrderDto> updateBranchOrder(@PathVariable("id") String id, @Valid @RequestBody @P("branchOrder") BranchOrderDto request) {
        checkRequest(request);
        return new ResponseEntity<>(branchOrderService.updateBranchOrder(id, request), HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('READ_DATA')")
    @GetMapping
    public ResponseEntity<List<BranchOrderDto>> getBranchOrders(
            @RequestParam(value = "branch", required = false) String branchName,
            @RequestParam(value = "customer-national-id", required = false) String customerNationalId,
            @RequestParam(value = "status", required = false) BranchOrderStatus status) {
        return new ResponseEntity<>(branchOrderService.getBranchOrders(branchName, customerNationalId, status), HttpStatus.OK);
    }

    @PostAuthorize("hasAuthority('READ_DATA') OR returnObject.body.customerNationalId == authentication.principal.username")
    @GetMapping("/{id}")
    public ResponseEntity<BranchOrderDto> getBranchOrder(@PathVariable("id") String id) {
        return new ResponseEntity<>(branchOrderService.getBranchOrder(id), HttpStatus.OK);
    }

    @PreAuthorize("@branchOrderSecurityService.isOwner(#branchOrderId, authentication)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBranchOrder(@PathVariable("id") @P("branchOrderId") String id) {
        branchOrderService.deleteBranchOrder(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("@branchOrderSecurityService.isOwner(#branchOrderId, authentication)")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<MessageResponse<String>> cancelBranchOrder(@PathVariable("id") @P("branchOrderId") String id) {
        branchOrderService.cancelBranchOrder(id);
        MessageResponse<String> response = new MessageResponse<>(String.format("%s is successfully cancelled!", Entity.BRANCH_ORDER.getValue()));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('TELLER')")
    @PatchMapping("/{id}")
    public ResponseEntity<Void> updateStatusOfBranchOrder(
            @PathVariable("id") String branchOrderId,
            @RequestParam("status") BranchOrderStatus status,
            @RequestParam(value = "account-activity-id", required = false) String accountActivityId) {
        if (status == BranchOrderStatus.WAIT || status == BranchOrderStatus.CANCELED) {
            throw new BadRequestException("Invalid branch order status update for the teller");
        }

        if (status == BranchOrderStatus.COMPLETED && Optional.ofNullable(accountActivityId).isEmpty()) {
            throw new BadRequestException("Account activity id is empty!");
        }

        branchOrderService.updateStatusOfBranchOrder(branchOrderId, status, accountActivityId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private void checkRequest(BranchOrderDto request) {
        List<ActivityType> validActivityTypes = List.of(ActivityType.MONEY_DEPOSIT, ActivityType.WITHDRAWAL, ActivityType.MONEY_TRANSFER, ActivityType.MONEY_EXCHANGE);

        if (!validActivityTypes.contains(request.activityType())) {
            throw new BadRequestException("Invalid activity type!");
        }
    }
}
