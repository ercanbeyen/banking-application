package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.constant.enums.*;
import com.ercanbeyen.bankingapplication.dto.AccountDto;
import com.ercanbeyen.bankingapplication.dto.ChannelInformation;
import com.ercanbeyen.bankingapplication.dto.request.MoneyExchangeRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyTransferRequest;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceExpectationFailedException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
@UtilityClass
public class AccountUtil {

    public void checkRequest(AccountDto request) {
        if (Optional.ofNullable(request.getIsBlocked()).isPresent() || Optional.ofNullable(request.getClosedAt()).isPresent()) {
            throw new BadRequestException("Request should not contain block and closed at statuses");
        }

        AccountType accountType = request.getType();

        if (accountType == AccountType.DEPOSIT) {
            if (Optional.ofNullable(request.getInterestRate()).isEmpty() || Optional.ofNullable(request.getDepositMaturity()).isEmpty()) {
                throw new ResourceExpectationFailedException(String.format("%s must have interest and deposit maturity values", accountType.getValue()));
            }

            TermDepositInterestRateUtil.checkValidityOfDepositMaturity(request.getDepositMaturity());
        } else {
            request.setInterestRate(null);
            request.setDepositMaturity(null);
            request.setDepositMaturityDate(null);

            log.warn("{} account does not have deposit maturity", accountType.getValue());
        }

        Double balance = request.getBalance();

        if (Optional.ofNullable(balance).isPresent() && balance != 0) {
            throw new BadRequestException("Not any balance value should be assigned directly from request");
        }
    }

    public void checkMoneyTransferRequest(MoneyTransferRequest request, ChannelInformation channelInformation) {
        checkAccountActivityWithChannelType(channelInformation, ActivityType.MONEY_TRANSFER);
        checkHeaderParametersForMoneyTransferAndMoneyExchange(channelInformation);

        if (Objects.equals(request.senderAccountId(), request.recipientAccountId())) {
            throw new BadRequestException("Identity of sender and recipient accounts should not be equal");
        }
    }

    public void checkMoneyExchangeRequest(MoneyExchangeRequest request, ChannelInformation channelInformation) {
        checkAccountActivityWithChannelType(channelInformation, ActivityType.MONEY_EXCHANGE);
        checkHeaderParametersForMoneyTransferAndMoneyExchange(channelInformation);

        if (Objects.equals(request.sellerAccountId(), request.buyerAccountId())) {
            throw new BadRequestException("Identity of seller and buyer accounts should not be equal");
        }
    }

    public void checkAccountActivityWithChannelType(ChannelInformation channelInformation, ActivityType activityType) {
        ChannelType channelType = channelInformation.channelType();
        List<ChannelType> channelTypes = activityType.getAvailableChannelTypes();

        if (!channelTypes.contains(channelType)) {
            throw new BadRequestException("Invalid channel type!");
        }
    }

    public void checkAccountActivityAndAccountTypeMatch(AccountType givenAccountType, AccountType expectedAccountType, ActivityType activityType) {
        if (givenAccountType != expectedAccountType) {
            throw new ResourceConflictException(activityType.getValue() + " can only be done from " + expectedAccountType.getValue() + " Accounts");
        }

        log.info("Account Type {} can apply account activity {}", givenAccountType.getValue(), activityType.getValue());
    }

    public void checkTypesOfAccountsBeforeMoneyTransferAndExchange(AccountType from, AccountType to, ActivityType activityType) {
        AccountType expectedAccountType = AccountType.CURRENT;

        checkAccountActivityAndAccountTypeMatch(from, expectedAccountType, activityType);
        checkAccountActivityAndAccountTypeMatch(to, expectedAccountType, activityType);

        log.info("Both accounts are {}", expectedAccountType.getValue());
    }

    private void checkHeaderParametersForMoneyTransferAndMoneyExchange(ChannelInformation channelInformation) {
        ChannelType channelType = channelInformation.channelType();
        Integer channelId = channelInformation.channelId();

        List<ChannelType> channelsRequiredChannelId = new ArrayList<>(ChannelType.channelsRequiredChannelId());
        List<ChannelType> channelsNotRequiredChannelId = new ArrayList<>(Arrays.asList(ChannelType.values()));
        channelsNotRequiredChannelId.removeAll(channelsRequiredChannelId);

        if (channelsRequiredChannelId.contains(channelType) && Optional.ofNullable(channelId).isEmpty()) {
            throw new BadRequestException("Channel Id is required for " + channelsRequiredChannelId);
        }

        if (channelsNotRequiredChannelId.contains(channelType) && Optional.ofNullable(channelId).isPresent()) {
            throw new BadRequestException("Channel Id should not be there for " + channelsNotRequiredChannelId);
        }
    }
}
