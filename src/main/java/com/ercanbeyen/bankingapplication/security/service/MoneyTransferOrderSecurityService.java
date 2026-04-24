package com.ercanbeyen.bankingapplication.security.service;

import com.ercanbeyen.bankingapplication.dto.MoneyTransferOrderDto;
import com.ercanbeyen.bankingapplication.service.MoneyTransferOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MoneyTransferOrderSecurityService {
    private final MoneyTransferOrderService moneyTransferOrderService;
    private final AccountSecurityService accountSecurityService;

    public boolean isOwner(Integer id, Authentication authentication) {
        MoneyTransferOrderDto moneyTransferOrder = moneyTransferOrderService.getEntity(id);
        return accountSecurityService.isOwner(moneyTransferOrder.getSenderAccountId(), authentication);
    }
}
