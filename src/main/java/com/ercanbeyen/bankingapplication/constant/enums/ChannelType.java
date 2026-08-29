package com.ercanbeyen.bankingapplication.constant.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChannelType {
    MOBILE_BANKING("Mobile Banking"),
    INTERNET_BANKING("Internet Banking"),
    ATM("ATM"),
    BRANCH("Branch"),
    SYSTEM("System");

    private static final String SYSTEM_CHANNEL_PLACE_NAME = "Application";
    private final String value;

    public static String getPlaceNameForSystemChannel() {
        return SYSTEM_CHANNEL_PLACE_NAME;
    }
}
