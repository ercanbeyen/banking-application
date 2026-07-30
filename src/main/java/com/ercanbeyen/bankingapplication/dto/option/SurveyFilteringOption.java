package com.ercanbeyen.bankingapplication.dto.option;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.Channel;
import com.ercanbeyen.bankingapplication.constant.enums.SurveyType;

import java.time.LocalDate;

public record SurveyFilteringOption(
        String customerNationalId,
        AccountActivityType accountActivityType,
        SurveyType surveyType,
        Channel channel,
        LocalDate createdAt,
        LocalDate validUntil) {

}
