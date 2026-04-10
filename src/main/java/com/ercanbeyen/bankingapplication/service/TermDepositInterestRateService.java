package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.dto.TermDepositInterestRateDto;
import com.ercanbeyen.bankingapplication.dto.option.TermDepositInterestRateFilteringOption;

public interface TermDepositInterestRateService extends BaseService<TermDepositInterestRateDto, TermDepositInterestRateFilteringOption> {
    double getInterestRate(Currency currency, int depositMaturity, double balance);
}
