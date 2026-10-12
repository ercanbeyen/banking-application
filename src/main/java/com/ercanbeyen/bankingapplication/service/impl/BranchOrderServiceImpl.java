package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.BranchOrderStatus;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.BranchOrderDto;
import com.ercanbeyen.bankingapplication.dto.request.FileUploadRequest;
import com.ercanbeyen.bankingapplication.entity.*;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceExpectationFailedException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.BranchOrderMapper;
import com.ercanbeyen.bankingapplication.repository.BranchOrderRepository;
import com.ercanbeyen.bankingapplication.service.*;
import com.ercanbeyen.bankingapplication.util.FileUtil;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BranchOrderServiceImpl implements BranchOrderService {
    private final BranchOrderRepository branchOrderRepository;
    private final BranchOrderMapper branchOrderMapper;
    private final CustomerService customerService;
    private final BranchService branchService;
    private final AccountActivityService accountActivityService;
    private final FileService fileService;

    @Override
    public void createBranchOrder(BranchOrderDto request, MultipartFile file) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        BranchOrder branchOrder = branchOrderMapper.dtoToEntity(request);
        Customer customer = customerService.findByNationalId(request.customerNationalId());
        Branch branch = branchService.findByName(request.branchName());

        branchOrder.setCustomer(customer);
        branchOrder.setBranch(branch);

        constructBranchOrder(null, request, file, branchOrder);
    }

    @Override
    public void updateBranchOrder(String id, BranchOrderDto request, MultipartFile file) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        BranchOrder branchOrder = findById(id);

        if (branchOrder.getStatus() != BranchOrderStatus.WAIT && branchOrder.getStatus() != BranchOrderStatus.CANCELED) {
            throw new ResourceConflictException(String.format(ResponseMessage.BRANCH_ORDER_IS_NOT_AVAILABLE, "updating"));
        }

        constructBranchOrder(id, request, file, branchOrder);
    }

    @Override
    public List<BranchOrderDto> getBranchOrders(String branchName, String customerNationalId, BranchOrderStatus status) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        boolean doesBranchNamePresent = Optional.ofNullable(branchName).isPresent();
        boolean doesCustomerNationalIdPresent = Optional.ofNullable(customerNationalId).isPresent();
        BranchOrderStatus statusFilter = Optional.ofNullable(status).isPresent() ? status : BranchOrderStatus.WAIT;

        List<BranchOrder> branchOrders;

        if (doesBranchNamePresent && doesCustomerNationalIdPresent) {
            Branch branch = branchService.findByName(branchName);
            Customer customer = customerService.findByNationalId(customerNationalId);
            branchOrders = branchOrderRepository.findByBranchAndCustomerAndStatus(branch, customer, statusFilter);
        } else if (doesBranchNamePresent) {
            Branch branch = branchService.findByName(branchName);
            branchOrders = branchOrderRepository.findByBranchAndStatus(branch, statusFilter);
        } else if (doesCustomerNationalIdPresent) {
            Customer customer = customerService.findByNationalId(customerNationalId);
            branchOrders = branchOrderRepository.findByCustomerAndStatus(customer, statusFilter);
        } else {
            branchOrders = branchOrderRepository.findByStatus(statusFilter);
        }

        return branchOrders.stream()
                .map(branchOrderMapper::entityToDto)
                .toList();
    }

    @Override
    public BranchOrderDto getBranchOrder(String id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        BranchOrder branchOrder = branchOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, Entity.BRANCH_ORDER.getValue())));

        return branchOrderMapper.entityToDto(branchOrder);
    }

    @Override
    public void deleteBranchOrder(String id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        BranchOrder branchOrder = findById(id);

        if (branchOrder.getStatus() != BranchOrderStatus.CANCELED) {
            throw new ResourceConflictException(String.format(ResponseMessage.BRANCH_ORDER_IS_NOT_AVAILABLE, "deletion"));
        }

        branchOrderRepository.delete(branchOrder);
        log.info(LogMessage.RESOURCE_DELETE_SUCCESS, Entity.BRANCH_ORDER.getValue(), id);
    }

    @Override
    public void cancelBranchOrder(String id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        BranchOrder branchOrder = findById(id);
        BranchOrderStatus status = branchOrder.getStatus();

        if (status == BranchOrderStatus.CANCELED) {
            throw new ResourceConflictException(String.format("%s has already been cancelled before!", Entity.BRANCH_ORDER.getValue()));
        }

        if (status != BranchOrderStatus.WAIT) {
            throw new ResourceConflictException(String.format(ResponseMessage.BRANCH_ORDER_IS_NOT_AVAILABLE, "cancellation"));
        }

        branchOrder.setStatus(BranchOrderStatus.CANCELED);
        branchOrderRepository.save(branchOrder);
    }

    @Override
    public void updateStatusOfBranchOrder(String branchOrderId, BranchOrderStatus status, String accountActivityId) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        BranchOrder branchOrder = findById(branchOrderId);

        /*
            Branch Order Status Flow:
            canceled <-> wait -> in process -> completed
                                            |
                                            -> incompleted
         */
        boolean statusFlowApplied = (branchOrder.getStatus() == BranchOrderStatus.WAIT && status == BranchOrderStatus.IN_PROCESS)
                || (branchOrder.getStatus() == BranchOrderStatus.IN_PROCESS && (status == BranchOrderStatus.COMPLETED || status == BranchOrderStatus.INCOMPLETED));

        if (!statusFlowApplied) {
            throw new ResourceConflictException(String.format("%s status flow is violated!", Entity.BRANCH_ORDER.getValue()));
        }


        if (status == BranchOrderStatus.COMPLETED) {
            AccountActivity accountActivity = accountActivityService.findById(accountActivityId);
            branchOrder.setAccountActivity(accountActivity);
        }

        branchOrder.setStatus(status);
        branchOrderRepository.save(branchOrder);
    }

    private BranchOrder findById(String id) {
        return branchOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, Entity.BRANCH_ORDER.getValue())));
    }

    private void constructBranchOrder(String id, BranchOrderDto request, MultipartFile file, BranchOrder branchOrder) {
        Customer customer;
        Branch branch;

        if (Optional.ofNullable(id).isPresent()) { // update operation
            customer = branchOrder.getCustomer();
            branch = branchOrder.getBranch().getName().equals(request.branchName()) ? branchOrder.getBranch() : branchService.findByName(request.branchName());
        } else { // create operation
            customer = customerService.findByNationalId(request.customerNationalId());
            branch = branchService.findByName(request.branchName());
        }

        String entity = Entity.BRANCH_ORDER.getValue();

        Optional.ofNullable(file).ifPresentOrElse(petitionRequest -> {
                    FileUploadRequest fileUploadRequest = FileUtil.createFileUploadRequest(petitionRequest, null);
                    fileService.saveFile(fileUploadRequest)
                            .thenAccept(petition -> {
                                branchOrder.setCustomer(customer);
                                branchOrder.setBranch(branch);
                                branchOrder.setStatus(BranchOrderStatus.WAIT);
                                branchOrder.setPetition(petition);
                                branchOrder.setContent(null);

                                BranchOrder savedBranchOrder = branchOrderRepository.save(branchOrder);
                                log.info(LogMessage.RESOURCE_CREATE_SUCCESS, entity, savedBranchOrder.getId());
                            })
                            .exceptionally(exception -> {
                                log.error("Unable to upload {}. Error: {}", Entity.FILE.getValue().toLowerCase(), exception.getMessage());
                                throw new ResourceExpectationFailedException("Unable to upload petition!");
                            });
                }, () -> {
                    branchOrder.setCustomer(customer);
                    branchOrder.setBranch(branch);
                    branchOrder.setStatus(BranchOrderStatus.WAIT);
                    branchOrder.setPetition(null);
                    branchOrder.setContent(request.content());

                    BranchOrder savedBranchOrder = branchOrderRepository.save(branchOrder);
                    log.info(LogMessage.RESOURCE_CREATE_SUCCESS, entity, savedBranchOrder.getId());
                }
        );
    }
}
