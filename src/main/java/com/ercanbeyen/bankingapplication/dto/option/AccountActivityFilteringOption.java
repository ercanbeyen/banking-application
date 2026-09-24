package com.ercanbeyen.bankingapplication.dto.option;

import com.ercanbeyen.bankingapplication.constant.enums.ActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.ChannelType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record AccountActivityFilteringOption(
        List<ActivityType> activityTypes,
        Integer senderAccountId,
        Integer recipientAccountId,
        @NotNull(message = "Minimum amount should not be null")
        @Min(value = 0, message = "Minimum amount value should be at least {value}")
        Double minimumAmount,
        LocalDate fromDate,
        LocalDate toDate,
        List<ChannelType> channelTypes) {

}
