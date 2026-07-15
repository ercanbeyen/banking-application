package com.ercanbeyen.bankingapplication.util;

import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.function.IntFunction;

@UtilityClass
public class TimeUtil {
    private final String ZONE_ID = "Europe/Istanbul";

    public LocalDateTime getTurkeyDateTime() {
        return LocalDateTime.now(ZoneId.of(ZONE_ID));
    }

    public LocalDate getTurkeyDate() {
        return LocalDate.now(ZoneId.of(ZONE_ID));
    }

    public String getTimeStatement(LocalTime localTime) {
        final String separator = ":";
        StringBuilder stringBuilder = new StringBuilder();
        final IntFunction<String> convertLocalTimeToTimeStatement = time -> time < 10 ? "0" + time : String.valueOf(time);

        String hour = convertLocalTimeToTimeStatement.apply(localTime.getHour());
        String minute = convertLocalTimeToTimeStatement.apply(localTime.getMinute());
        String second = convertLocalTimeToTimeStatement.apply(localTime.getSecond());

        return stringBuilder.append(hour)
                .append(separator)
                .append(minute)
                .append(separator)
                .append(second)
                .toString();
    }
}
