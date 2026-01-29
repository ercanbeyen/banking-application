package com.ercanbeyen.bankingapplication.dto;

import com.ercanbeyen.bankingapplication.constant.enums.AccountType;
import com.ercanbeyen.bankingapplication.constant.enums.Currency;

public record AccountFinancialStatus(AccountType accountType, Currency currency, Double balance) {

}
