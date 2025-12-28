package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.dto.AccountActivityDto;
import lombok.experimental.UtilityClass;

import java.util.Objects;
import java.util.Optional;

@UtilityClass
public class ExporterUtil {
    private final String BANK_NAME = "Online Bank";
    private final String ACCOUNT_STATEMENT_TITLE = "ACCOUNT STATEMENT";
    private final String LAW_MESSAGE = """
            If the information on this document does not match the bank records,
            the bank records will be taken as basis and this document will not even constitute the beginning of
            written evidence.
            """;
    private final String LOGO_PATH = "/app/photo/logo.png";
    private final String TIME_ZONE_MESSAGE = "Trading hours are shown according to Turkey time.";

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
        return BANK_NAME;
    }

    public String getAccountStatementTitle() {
        return ACCOUNT_STATEMENT_TITLE;
    }

    public String getLogoPath() {
        return LOGO_PATH;
    }

    public String getLawMessage() {
        return LAW_MESSAGE;
    }

    public String getTimeZoneMessage() {
        return TIME_ZONE_MESSAGE;
    }
}
