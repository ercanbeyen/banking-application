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

    public String getBankName() {
        return "Online Bank";
    }

    public String getLawMessage() {
        return """
                If the information on this document does not match the bank records,
                the bank records will be taken as basis and this document will not even constitute the beginning of
                written evidence.
                """;
    }

    public String getTimeZoneMessage() {
        return "Trading hours are shown according to Turkey time.";
    }
}
