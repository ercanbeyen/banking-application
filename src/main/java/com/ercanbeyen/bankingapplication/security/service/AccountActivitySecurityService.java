package com.ercanbeyen.bankingapplication.security.service;

import com.ercanbeyen.bankingapplication.dto.AccountActivityDto;
import com.ercanbeyen.bankingapplication.service.AccountActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class AccountActivitySecurityService {
    private final AccountActivityService accountActivityService;
    private final AccountSecurityService accountSecurityService;

    public boolean isOwner(String accountActivityId, Authentication authentication) {
        AccountActivityDto requestedAccountActivity = accountActivityService.getAccountActivity(accountActivityId);

        Predicate<Integer> checkAccountId = Objects::nonNull;
        Predicate<Integer> checkAccountOwner = accountId -> accountSecurityService.isOwner(accountId, authentication);

        boolean isSenderAccount = checkAccountId.and(checkAccountOwner).test(requestedAccountActivity.senderAccountId());
        boolean isRecipientAccount = checkAccountId.and(checkAccountOwner).test(requestedAccountActivity.recipientAccountId());

        return isSenderAccount || isRecipientAccount;
    }
}
