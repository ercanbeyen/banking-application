package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.constant.enums.BranchOrderStatus;
import com.ercanbeyen.bankingapplication.dto.BranchOrderDto;

import java.util.List;

public interface BranchOrderService {
    BranchOrderDto createBranchOrder(BranchOrderDto request);
    BranchOrderDto updateBranchOrder(String id, BranchOrderDto request);
    List<BranchOrderDto> getBranchOrders(String branchName, String customerNationalId, BranchOrderStatus status);
    BranchOrderDto getBranchOrder(String id);
    void deleteBranchOrder(String id);
    void cancelBranchOrder(String id);
    void updateStatusOfBranchOrder(String branchOrderId, BranchOrderStatus status, String accountActivityId);
}
