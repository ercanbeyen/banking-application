package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.constant.enums.BranchOrderStatus;
import com.ercanbeyen.bankingapplication.dto.BranchOrderDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BranchOrderService {
    void createBranchOrder(BranchOrderDto request, MultipartFile file);
    void updateBranchOrder(String id, BranchOrderDto request, MultipartFile file);
    List<BranchOrderDto> getBranchOrders(String branchName, String customerNationalId, BranchOrderStatus status);
    BranchOrderDto getBranchOrder(String id);
    void deleteBranchOrder(String id);
    void cancelBranchOrder(String id);
    void updateBranchOrderStatus(String branchOrderId, BranchOrderStatus status, String accountActivityId);
}
