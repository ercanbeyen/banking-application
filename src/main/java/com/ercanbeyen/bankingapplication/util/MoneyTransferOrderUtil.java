package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.dto.MoneyTransferOrderDto;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.util.function.Predicate;

@UtilityClass
public class MoneyTransferOrderUtil {
    public void checkMoneyTransferDate(LocalDate request) {
        LocalDate today = LocalDate.now();

        if (!request.isAfter(today)) {
            throw new BadRequestException(String.format("The earliest date for the transfer order can be tomorrow (%s)", today.plusDays(1)));
        }
    }

    public void checkDatesBeforeFiltering(LocalDate fromDate, LocalDate toDate) {
        if (!toDate.isAfter(fromDate)) {
            throw new BadRequestException("\"From\" date must be less than \"To\" date");
        }
    }

    public Predicate<MoneyTransferOrderDto> getMoneyTransferOrderDtoPredicate() {
        /*
            Money Transfer Date check flow:
            1) Increase transfer date adding by period until reaches to today date
            2) If next transfer date comes then it returns true, else it returns false
         */
        return moneyTransferOrderDto -> {
            LocalDate nextMoneyTransferDate = moneyTransferOrderDto.getCreatedAt().toLocalDate();
            LocalDate todayDate = LocalDate.now();

            do {
                nextMoneyTransferDate = switch (moneyTransferOrderDto.getRegularMoneyTransferDto().paymentPeriod()) {
                    case ONE_TIME -> todayDate;
                    case DAILY -> nextMoneyTransferDate.plusDays(1);
                    case WEEKLY -> nextMoneyTransferDate.plusWeeks(1);
                    case MONTHLY -> nextMoneyTransferDate.plusMonths(1);
                };
            } while (nextMoneyTransferDate.isBefore(todayDate));

            return todayDate.isEqual(nextMoneyTransferDate);
        };
    }
}
