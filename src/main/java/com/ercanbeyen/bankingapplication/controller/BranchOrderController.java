package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.enums.BranchOrderStatus;
import com.ercanbeyen.bankingapplication.dto.BranchOrderDto;
import com.ercanbeyen.bankingapplication.security.service.BranchOrderSecurityService;
import com.ercanbeyen.bankingapplication.service.BranchOrderService;
import com.ercanbeyen.bankingapplication.util.BranchOrderUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/branch-orders")
@RequiredArgsConstructor
public class BranchOrderController {
    private final BranchOrderService branchOrderService;
    private final BranchOrderSecurityService branchOrderSecurityService;

    @PreAuthorize("#branchOrder.customerNationalId == authentication.principal.username")
    @PostMapping
    public ResponseEntity<BranchOrderDto> createBranchOrder(@Valid @RequestBody @P("branchOrder") BranchOrderDto request) {
        BranchOrderUtil.checkRequest(request);
        return new ResponseEntity<>(branchOrderService.createBranchOrder(request), HttpStatus.CREATED);
    }

    @PreAuthorize("#branchOrder.customerNationalId == authentication.principal.username")
    @PutMapping("/{id}")
    public ResponseEntity<BranchOrderDto> updateBranchOrder(@PathVariable("id") String id, @Valid @RequestBody @P("branchOrder") BranchOrderDto request) {
        BranchOrderUtil.checkRequest(request);
        return new ResponseEntity<>(branchOrderService.updateBranchOrder(id, request), HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('READ_DATA')")
    @GetMapping
    public ResponseEntity<List<BranchOrderDto>> getBranchOrders(@RequestParam("customer-national-id") String customerNationalId, @RequestParam("status") BranchOrderStatus status) {
        return new ResponseEntity<>(branchOrderService.getBranchOrders(customerNationalId, status), HttpStatus.OK);
    }

    @PostAuthorize("returnObject.body.customerNationalId == authentication.principal.username OR hasAuthority('READ_DATA')")
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
    public ResponseEntity<Void> cancelBranchOrder(@PathVariable("id") @P("branchOrderId") String id) {
        branchOrderService.cancelBranchOrder(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
