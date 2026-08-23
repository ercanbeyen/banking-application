package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.constant.enums.BalanceActivity;
import com.ercanbeyen.bankingapplication.constant.query.SummaryField;
import com.ercanbeyen.bankingapplication.dto.response.AccountActivityPreview;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@UtilityClass
public class ExporterUtil {
    private final String BANK_NAME = "Online Bank";
    private final String ACCOUNT_STATEMENT_TITLE = "account statement";
    private final String LAW_MESSAGE = """
            If the information on this document does not match the bank records,
            the bank records will be taken as basis and this document will not even constitute the beginning of
            written evidence.
            """;
    private final String LOGO_PATH = "/app/photo/logo.png";
    private final String TIME_ZONE_MESSAGE = "Trading hours are displayed according to the time zone of the region where the account is located.";

    public final List<String> maskedSummaryFields = List.of(SummaryField.FULL_NAME, SummaryField.NATIONAL_IDENTITY);

    public Double calculateAmountForDataLine(AccountActivityPreview accountActivityPreview) {
        Double amount = accountActivityPreview.amount();

        if (accountActivityPreview.balanceActivity() == BalanceActivity.DECREASE) {
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

    public String maskField(Map.Entry<String, Object> entry) {
        String key = entry.getKey();
        String value = entry.getValue().toString();
        StringBuilder valueBuilder = new StringBuilder();

        Function<String, StringBuilder> maskWordInFullName = word -> {
            int length = word.length();
            int endIndex = length < 5 ? 1 : 2;

            return new StringBuilder()
                    .append(word, 0, endIndex)
                    .append("*".repeat(length - endIndex));
        };

        if (key.contains(SummaryField.FULL_NAME)) {
            int spaceIndex = value.indexOf(' ');
            String name = value.substring(0, spaceIndex);
            String surname = value.substring(spaceIndex + 1);

            valueBuilder.append(maskWordInFullName.apply(name))
                    .append(" ")
                    .append(maskWordInFullName.apply(surname));
        } else if (key.contains(SummaryField.NATIONAL_IDENTITY)) {
            int length = value.length();
            valueBuilder.append(value, 0, 3)
                    .append("*".repeat(length - 5))
                    .append(value, length - 2, length);
        } else {
            throw new ResourceConflictException(String.format("Summary field %s is not in %s", key, maskedSummaryFields));
        }

        return valueBuilder.toString();
    }
}
