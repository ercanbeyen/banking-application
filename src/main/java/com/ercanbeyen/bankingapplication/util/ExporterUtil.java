package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.dto.AccountActivityDto;
import lombok.experimental.UtilityClass;

import java.util.Objects;
import java.util.Optional;

@UtilityClass
public class ExporterUtil {
    public Double calculateAmountForDataLine(Integer accountId, AccountActivityDto accountActivityDto) {
        Double amount = accountActivityDto.amount();
        boolean accountActivityForSenderExists = Optional.ofNullable(accountActivityDto.senderAccountId()).isPresent()
                && Objects.equals(accountActivityDto.senderAccountId(), accountId);

        if (accountActivityForSenderExists) {
            amount *= -1;
        }

        return amount;
    }
}
