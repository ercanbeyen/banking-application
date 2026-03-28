package com.ercanbeyen.bankingapplication.security.service;

import com.ercanbeyen.bankingapplication.dto.AccountDto;
import com.ercanbeyen.bankingapplication.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountSecurityService {
    private final AccountService accountService;

    public boolean isOwner(Integer accountId, Authentication authentication) {
        AccountDto requestedAccount = accountService.getEntity(accountId);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String customerNationalId = userDetails.getUsername();
        return requestedAccount.getCustomerNationalId().equals(customerNationalId);
    }
}
