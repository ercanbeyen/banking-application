package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.BranchOrderStatus;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.BranchOrderDto;
import com.ercanbeyen.bankingapplication.entity.Branch;
import com.ercanbeyen.bankingapplication.entity.BranchOrder;
import com.ercanbeyen.bankingapplication.entity.Customer;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.BranchOrderMapper;
import com.ercanbeyen.bankingapplication.repository.BranchOrderRepository;
import com.ercanbeyen.bankingapplication.service.BranchService;
import com.ercanbeyen.bankingapplication.service.BranchOrderService;
import com.ercanbeyen.bankingapplication.service.CustomerService;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BranchOrderServiceImpl implements BranchOrderService {
    private final BranchOrderRepository branchOrderRepository;
    private final BranchOrderMapper branchOrderMapper;
    private final CustomerService customerService;
    private final BranchService branchService;

    @Override
    public BranchOrderDto createBranchOrder(BranchOrderDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        BranchOrder branchOrder = branchOrderMapper.dtoToEntity(request);
        Customer customer = customerService.findByNationalId(request.customerNationalId());
        Branch branch = branchService.findByName(request.branchName());

        branchOrder.setCustomer(customer);
        branchOrder.setBranch(branch);
        branchOrder.setStatus(BranchOrderStatus.WAIT);

        BranchOrder savedBranchOrder = branchOrderRepository.save(branchOrder);
        log.info(LogMessage.RESOURCE_CREATE_SUCCESS, Entity.BRANCH_ORDER.getValue(), savedBranchOrder.getId());

        return branchOrderMapper.entityToDto(savedBranchOrder);
    }

    @Override
    public BranchOrderDto updateBranchOrder(String id, BranchOrderDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        BranchOrder branchOrder = findById(id);

        if (branchOrder.getStatus() != BranchOrderStatus.WAIT && branchOrder.getStatus() != BranchOrderStatus.CANCELLED) {
            throw new ResourceConflictException(String.format(ResponseMessage.BRANCH_ORDER_IS_NOT_AVAILABLE, "updating"));
        }

        if (!branchOrder.getBranch().getName().equals(request.branchName())) {
            Branch branch = branchService.findByName(request.branchName());
            branchOrder.setBranch(branch);
        }

        branchOrder.setContent(request.content());
        branchOrder.setStatus(BranchOrderStatus.WAIT);

        return branchOrderMapper.entityToDto(branchOrderRepository.save(branchOrder));
    }

    @Override
    public List<BranchOrderDto> getBranchOrders(String customerNationalId, BranchOrderStatus status) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Customer customer = customerService.findByNationalId(customerNationalId);

        return branchOrderRepository.findByCustomer(customer)
                .stream()
                .filter(branchOrder -> branchOrder.getCustomer().getNationalId().equals(customerNationalId) && branchOrder.getStatus() == status)
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

        if (branchOrder.getStatus() != BranchOrderStatus.CANCELLED) {
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

        if (status == BranchOrderStatus.CANCELLED) {
            throw new ResourceConflictException(String.format("%s has already been cancelled before!", Entity.BRANCH_ORDER.getValue()));
        }

        if (status != BranchOrderStatus.WAIT) {
            throw new ResourceConflictException(String.format(ResponseMessage.BRANCH_ORDER_IS_NOT_AVAILABLE, "cancellation"));
        }

        branchOrder.setStatus(BranchOrderStatus.CANCELLED);
        branchOrderRepository.save(branchOrder);
    }

    private BranchOrder findById(String id) {
        return branchOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, Entity.BRANCH_ORDER.getValue())));
    }
}
