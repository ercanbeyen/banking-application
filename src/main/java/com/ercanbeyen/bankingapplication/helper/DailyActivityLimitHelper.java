package com.ercanbeyen.bankingapplication.helper;


import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.ChannelType;
import com.ercanbeyen.bankingapplication.dto.DailyActivityLimitDto;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
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

    public void checkActivityLimits(AccountActivityType activityType, Double amount, ChannelType channelType) {
        if (channelExemptFromDailyActivityLimit(channelType)) {
            return;
        }

        DailyActivityLimitDto dailyAccountActivityLimitDto = dailyActivityLimitService.getDailyActivityLimit(activityType);
        Double lowerLimit = dailyAccountActivityLimitDto.lowerLimit();
        Double upperLimit = dailyAccountActivityLimitDto.upperLimit();

        log.info("Remaining daily activity limit: {}", upperLimit - amount);

        if (amount < lowerLimit || amount > upperLimit) {
            throw new BadRequestException(String.format(
                    "Amount is not in daily account activity limits. Limits for %s are between %s and %s",
                    activityType.getValue(),
                    FormatterUtil.convertNumberToFormalExpression(lowerLimit),
                    FormatterUtil.convertNumberToFormalExpression(upperLimit)
            ));
        }

        log.info("Daily limits of {} are not exceeded", activityType.getValue());
    }

    public static boolean channelExemptFromDailyActivityLimit(ChannelType channelType) {
        if (channelType == ChannelType.getChannelTypeWithNoDailyAccountActivityLimit()) {
            log.info("There is no daily activity limit for transactions made at the {}", channelType.getValue());
            return true;
        }

        return false;
    }
}
