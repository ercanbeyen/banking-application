package com.ercanbeyen.bankingapplication.util;

import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.time.ZoneId;

@UtilityClass
public class TimeUtil {
    public LocalDateTime getCurrentTimeStampInTurkey() {
        return LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
    }
}
