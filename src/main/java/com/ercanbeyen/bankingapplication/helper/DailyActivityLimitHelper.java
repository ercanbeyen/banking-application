package com.ercanbeyen.bankingapplication.helper;


import com.ercanbeyen.bankingapplication.constant.enums.ActivityType;
import com.ercanbeyen.bankingapplication.dto.DailyActivityLimitDto;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.service.DailyActivityLimitService;
import com.ercanbeyen.bankingapplication.util.FormatterUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class DailyActivityLimitHelper {
    private final DailyActivityLimitService dailyActivityLimitService;

    public void checkActivityLimits(ActivityType activityType, Double amount) {
        DailyActivityLimitDto dailyAccountActivityLimitDto = dailyActivityLimitService.getDailyActivityLimit(activityType);
        Double lowerLimit = dailyAccountActivityLimitDto.lowerLimit();
        Double upperLimit = dailyAccountActivityLimitDto.upperLimit();

        log.info("Remaining daily activity limit: {}", upperLimit - amount);

        if (amount < lowerLimit || amount > upperLimit) {
            throw new ResourceConflictException(String.format(
                    "Amount is not in daily activity limits. Limits for %s are between %s and %s",
                    activityType.getValue(),
                    FormatterUtil.convertNumberToFormalExpression(lowerLimit),
                    FormatterUtil.convertNumberToFormalExpression(upperLimit)
            ));
        }

        log.info("Daily activity limits of {} are not exceeded", activityType.getValue());
    }
}
