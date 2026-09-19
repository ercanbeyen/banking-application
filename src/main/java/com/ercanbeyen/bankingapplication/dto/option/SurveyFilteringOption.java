package com.ercanbeyen.bankingapplication.dto.option;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.ChannelType;
import com.ercanbeyen.bankingapplication.constant.enums.SurveyType;

import java.time.LocalDate;

public record SurveyFilteringOption(
        String customerNationalId,
        AccountActivityType accountActivityType,
        SurveyType surveyType,
        ChannelType channelType,
        LocalDate createdAt,
        LocalDate validUntil) {

}
