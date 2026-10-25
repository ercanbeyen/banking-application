package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.ExchangeDto;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ExchangeUtil {
    public void checkRequest(ExchangeDto request) {
        if (request.getBaseCurrency() == request.getTargetCurrency()) {
            throw new BadRequestException(String.format(ResponseMessage.UNPAIRED_CURRENCIES, "different"));
        }
    }
}
