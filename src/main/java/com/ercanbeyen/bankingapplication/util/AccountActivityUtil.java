package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.constant.enums.AttachmentFile;
import com.ercanbeyen.bankingapplication.dto.request.AccountActivityFilteringRequest;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import com.ercanbeyen.bankingapplication.dto.option.AccountActivityFilteringOption;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Predicate;

@Slf4j
@UtilityClass
public class AccountActivityUtil {
    public void checkFilteringOption(AccountActivityFilteringOption filteringOption) {
        checkDates(filteringOption.fromDate(), filteringOption.toDate());
    }

    public void checkFilteringRequest(AccountActivityFilteringRequest request) {
        checkDates(request.fromDate(), request.toDate());
    }

    public String getAttachmentFileName(String id, AttachmentFile attachmentFile, AttachmentFile.Format fileFormat) {
        String fileType = switch (fileFormat) {
            case AttachmentFile.Format.PDF -> "pdf";
            case AttachmentFile.Format.EXCEL -> "xlsx";
            case AttachmentFile.Format.WORD -> "docx";
        };

        String fileNameTemplate = switch (attachmentFile) {
            case ACCOUNT_STATEMENT -> "account_%s_statement.%s";
            case FINANCIAL_STATUS_REPORT -> "customer_%s_financial_status_report.%s";
            case RECEIPT -> "account_activity_%s_receipt.%s";
        };

        return String.format(fileNameTemplate, id, fileType);
    }

    private void checkDates(LocalDate fromDate, LocalDate toDate) {
        if (isDateEmpty.test(fromDate)) {
            log.warn("From date is null");
            return;
        }

        if (isDateEmpty.test(toDate)) {
            log.warn("To date is null");
            return;
        }

        if (toDate.isBefore(fromDate)) {
            throw new BadRequestException("To date cannot be before from date");
        }

        log.info("Dates are compatible");
    }

    private final Predicate<LocalDate> isDateEmpty = localDate -> Optional.ofNullable(localDate).isEmpty();
}
