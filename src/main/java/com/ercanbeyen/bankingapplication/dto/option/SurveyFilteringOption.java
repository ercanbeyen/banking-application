package com.ercanbeyen.bankingapplication.dto.option;

import com.ercanbeyen.bankingapplication.constant.enums.ActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.ChannelType;
import com.ercanbeyen.bankingapplication.constant.enums.SurveyType;

import java.time.LocalDate;

public record SurveyFilteringOption(
        String customerNationalId,
        ActivityType activityType,
        SurveyType surveyType,
        ChannelType channelType,
        LocalDate createdAt,
        LocalDate validUntil) {

}
