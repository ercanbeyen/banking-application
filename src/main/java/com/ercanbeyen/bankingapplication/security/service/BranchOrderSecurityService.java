package com.ercanbeyen.bankingapplication.security.service;

import com.ercanbeyen.bankingapplication.dto.BranchOrderDto;
import com.ercanbeyen.bankingapplication.security.util.UserDetailsUtil;
import com.ercanbeyen.bankingapplication.service.BranchOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BranchOrderSecurityService {
    private final BranchOrderService branchOrderService;

    public boolean isOwner(String id, Authentication authentication) {
        BranchOrderDto requestedBranchOrder = branchOrderService.getBranchOrder(id);
        String customerNationalId = UserDetailsUtil.getUserDetails(authentication).getUsername();
        return requestedBranchOrder.customerNationalId().equals(customerNationalId);
    }
}
