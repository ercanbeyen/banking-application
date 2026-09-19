package com.ercanbeyen.bankingapplication.dto;

import com.ercanbeyen.bankingapplication.constant.enums.ChannelType;

import java.time.ZoneId;

public record TransactionInformation(String placeName, ChannelType channel, ZoneId zoneId) {

}
