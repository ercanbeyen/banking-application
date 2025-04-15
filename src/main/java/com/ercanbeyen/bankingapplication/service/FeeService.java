package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.dto.FeeDto;
import com.ercanbeyen.bankingapplication.option.FeeFilteringOption;

public interface FeeService extends BaseService<FeeDto, FeeFilteringOption> {
    double getInterestRatio(Currency currency, int depositPeriod, double balance);
}
