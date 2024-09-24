package com.ercanbeyen.bankingapplication.constant.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum City {
    ANTALYA("Antalya"),
    ANKARA("Ankara"),
    BURSA("Bursa"),
    ISTANBUL("İstanbul"),
    IZMIR("İzmir");

    private final String value;
}
