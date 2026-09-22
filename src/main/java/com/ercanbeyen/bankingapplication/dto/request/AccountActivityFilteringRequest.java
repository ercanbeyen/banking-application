package com.ercanbeyen.bankingapplication.dto.request;

import com.ercanbeyen.bankingapplication.constant.enums.ActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.BalanceActivity;
import com.ercanbeyen.bankingapplication.constant.enums.ChannelType;

import java.time.LocalDate;
import java.util.List;

public record AccountActivityFilteringRequest(
        BalanceActivity balanceActivity,
        LocalDate fromDate,
        LocalDate toDate,
        Double minimumAmount,
        List<ActivityType> activityTypes,
        List<ChannelType> channelTypes) {

}
