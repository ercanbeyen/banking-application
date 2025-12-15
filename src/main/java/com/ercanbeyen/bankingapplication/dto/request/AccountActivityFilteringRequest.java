package com.ercanbeyen.bankingapplication.dto.request;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.BalanceActivity;

import java.time.LocalDate;
import java.util.List;

public record AccountActivityFilteringRequest(
        BalanceActivity balanceActivity,
        LocalDate fromDate,
        LocalDate toDate,
        Double minimumAmount,
        List<AccountActivityType> activityTypes) {

}
