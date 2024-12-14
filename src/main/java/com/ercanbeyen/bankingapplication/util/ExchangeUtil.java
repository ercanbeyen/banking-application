package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ExchangeUtil {
    public void checkCurrenciesBeforeMoneyExchange(Currency from, Currency to) {
        if (from == to) {
            throw new BadRequestException(String.format(ResponseMessage.UNPAIRED_CURRENCIES, "different"));
        }
    }
}
