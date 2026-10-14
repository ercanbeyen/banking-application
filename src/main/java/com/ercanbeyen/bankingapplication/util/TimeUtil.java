package com.ercanbeyen.bankingapplication.util;

import lombok.experimental.UtilityClass;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Predicate;

@UtilityClass
public class TimeUtil {
    private final LocalTime workStartTime = LocalTime.of(9, 0);
    private final LocalTime workEndTime = LocalTime.of(17, 0);
    private final List<DayOfWeek> workDays = new ArrayList<>(Arrays.asList(DayOfWeek.values()));

    static {
        workDays.removeAll(List.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));
    }

    public boolean isLocalDateTimeInWorkDaysAndHours(LocalDateTime localDateTime) {
        Predicate<LocalDateTime> isWorkDay = givenLocalDateTime -> workDays.contains(givenLocalDateTime.getDayOfWeek());
        Predicate<LocalDateTime> isWorkTime = givenLocalDateTime -> givenLocalDateTime.toLocalTime().isAfter(workStartTime) && givenLocalDateTime.toLocalTime().isBefore(workEndTime);

        return isWorkDay.and(isWorkTime).test(localDateTime);
    }

    public String getLocalDateTimeStatement(LocalDateTime localDateTime) {
        return localDateTime.toLocalDate().toString() + " " + TimeUtil.getTimeStatement(localDateTime.toLocalTime());
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
