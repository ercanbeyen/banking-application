package com.ercanbeyen.bankingapplication.controller;

import com.ercanbeyen.bankingapplication.constant.enums.ActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.BranchOrderStatus;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.BranchOrderDto;
import com.ercanbeyen.bankingapplication.dto.response.MessageResponse;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import com.ercanbeyen.bankingapplication.security.service.BranchOrderSecurityService;
import com.ercanbeyen.bankingapplication.service.BranchOrderService;
import com.ercanbeyen.bankingapplication.util.TimeUtil;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/branch-orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class BranchOrderController {
    private final BranchOrderService branchOrderService;
    private final BranchOrderSecurityService branchOrderSecurityService;

    @PreAuthorize("#branchOrder.customerNationalId == authentication.principal.username")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponse<String>> createBranchOrder(@Valid @RequestPart("branchOrder") @P("branchOrder") BranchOrderDto request, @RequestPart(value = "petition", required = false) MultipartFile file) {
        checkRequest(request, file);

        branchOrderService.createBranchOrder(request, file);

        MessageResponse<String> response = new MessageResponse<>(String.format(ResponseMessage.SUCCESS, Entity.BRANCH_ORDER.getValue() + " creation"));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PreAuthorize("#branchOrder.customerNationalId == authentication.principal.username")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponse<String>> updateBranchOrder(@PathVariable("id") String id, @Valid @RequestPart("branchOrder") @P("branchOrder") BranchOrderDto request, @RequestPart(value = "petition", required = false) MultipartFile file) {
        checkRequest(request, file);

        branchOrderService.updateBranchOrder(id, request, file);

        MessageResponse<String> response = new MessageResponse<>(String.format(ResponseMessage.SUCCESS, Entity.BRANCH_ORDER.getValue() + " update"));
        return new ResponseEntity<>(response, HttpStatus.OK);
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
    public ResponseEntity<MessageResponse<String>> updateBranchOrderStatus(
            @PathVariable("id") String branchOrderId,
            @RequestParam("status") BranchOrderStatus status,
            @RequestParam(value = "account-activity-id", required = false) String accountActivityId) {
        checkBranchOrderStatusUpdate(status, accountActivityId);

        branchOrderService.updateBranchOrderStatus(branchOrderId, status, accountActivityId);

        MessageResponse<String> response = new MessageResponse<>(String.format("%s status is successfully updated!", Entity.BRANCH_ORDER.getValue()));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private void checkRequest(BranchOrderDto request, MultipartFile file) {
        if (!ActivityType.getBranchOrderActivities().contains(request.activityType())) {
            throw new BadRequestException("Invalid activity type!");
        }

        boolean petitionPresent = Optional.ofNullable(file).isPresent();

        if (petitionPresent) {
            if (file.isEmpty()) {
                throw new BadRequestException("Petition file is empty!");
            }

            final String permittedFileFormatForPetition = MediaType.APPLICATION_PDF_VALUE;

            if (!Objects.equals(file.getContentType(), permittedFileFormatForPetition)) {
                int index = permittedFileFormatForPetition.indexOf("/");
                throw new BadRequestException("Only " + permittedFileFormatForPetition.substring(index + 1) + " file format is permitted for petition!");
            }
        } else if (StringUtils.isBlank(request.content())) {
            throw new BadRequestException("No content or petition is submitted!");
        }
    }

    private static void checkBranchOrderStatusUpdate(BranchOrderStatus status, String accountActivityId) {
        String entity = Entity.BRANCH_ORDER.getValue();

        if (!TimeUtil.isLocalDateTimeInWorkDaysAndHours(LocalDateTime.now(ZoneId.systemDefault()))) {
            throw new BadRequestException(String.format("%ss may be processed on business days and hours.", entity));
        }

        if (status == BranchOrderStatus.PENDING || status == BranchOrderStatus.CANCELED) {
            throw new BadRequestException(String.format("Invalid %s status update for the teller", entity.toLowerCase()));
        }

        if (status == BranchOrderStatus.COMPLETED && Optional.ofNullable(accountActivityId).isEmpty()) {
            throw new BadRequestException(String.format("%s id is empty!", Entity.ACCOUNT_ACTIVITY.getValue()));
        }
    }
}
