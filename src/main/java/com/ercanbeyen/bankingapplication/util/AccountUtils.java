package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.AccountType;
import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessages;
import com.ercanbeyen.bankingapplication.dto.AccountDto;
import com.ercanbeyen.bankingapplication.dto.request.MoneyExchangeRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyTransferRequest;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceExpectationFailedException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

@Slf4j
@UtilityClass
public class AccountUtils {
    private final double LOWEST_THRESHOLD = 0;

    public void checkRequest(AccountDto accountDto) {
        if (Optional.ofNullable(accountDto.getIsBlocked()).isPresent() || Optional.ofNullable(accountDto.getClosedAt()).isPresent()) {
            throw new ResourceConflictException("Request should not contain block and closed at statuses");
        }

        checkAccountType(accountDto);
        Double balance = accountDto.getBalance();

        if (Optional.ofNullable(balance).isPresent() && balance != 0) {
            throw new ResourceConflictException("Not any balance value should be assigned directly from request");
        }
    }

    public void checkMoneyTransferRequest(MoneyTransferRequest request) {
        if (Objects.equals(request.senderAccountId(), request.receiverAccountId())) {
            throw new ResourceExpectationFailedException("Identity of sender and receiver accounts should not be equal");
        }

        AccountActivityType activityType = AccountActivityType.MONEY_TRANSFER;
        Double maximumMoneyTransferAmountPerRequest = AccountActivityType.getMaximumAmountPerRequestOfActivity(activityType);

        if (request.amount() >= maximumMoneyTransferAmountPerRequest) {
            String formattedValue = FormatterUtil.convertNumberToFormalExpression(maximumMoneyTransferAmountPerRequest);
            throw new ResourceExpectationFailedException(String.format("Maximum %s limit per request (%s) is exceeded", activityType.getValue(), formattedValue));
        }
    }

    public void checkMoneyExchangeRequest(MoneyExchangeRequest request) {
        if (Objects.equals(request.sellerAccountId(), request.buyerAccountId())) {
            throw new ResourceExpectationFailedException("Identity of seller and buyer accounts should not be equal");
        }

        AccountActivityType activityType = AccountActivityType.MONEY_EXCHANGE;
        Double maximumMoneyExchangeAmountPerRequest = AccountActivityType.getMaximumAmountPerRequestOfActivity(activityType);

        if (request.amount() >= maximumMoneyExchangeAmountPerRequest) {
            String formattedValue = FormatterUtil.convertNumberToFormalExpression(maximumMoneyExchangeAmountPerRequest);
            throw new ResourceExpectationFailedException(String.format("Maximum %s limit per request (%s) is exceeded", activityType.getValue(), formattedValue));
        }
    }

    public void checkAccountActivityForCurrentAccount(AccountActivityType activityType) {
        List<AccountActivityType> accountActivityTypes = List.of(
                AccountActivityType.MONEY_DEPOSIT, AccountActivityType.WITHDRAWAL, AccountActivityType.FEE, AccountActivityType.CHARGE);

        if (!accountActivityTypes.contains(activityType)) {
            throw new ResourceConflictException(ResponseMessages.IMPROPER_ACCOUNT_ACTIVITY);
        }
    }

    public double calculateInterest(Double balance, Double interestRatio) {
        checkValidityOfBalanceAndInterestRatio(balance, interestRatio);
        return (balance == LOWEST_THRESHOLD || interestRatio == LOWEST_THRESHOLD) ? LOWEST_THRESHOLD : ((interestRatio * balance) / 1200);
    }

    public boolean checkAccountForPeriodicMoneyAdd(AccountType accountType, LocalDateTime updatedAt, Integer depositPeriod) {
        checkAccountTypeAndDepositPeriodForPeriodBalanceUpdate(accountType, depositPeriod);

        LocalDate isGoingToBeUpdatedAt = updatedAt
                .toLocalDate()
                .plusMonths(depositPeriod);

        return isGoingToBeUpdatedAt.isEqual(LocalDate.now());
    }

    public void checkCurrenciesBeforeMoneyTransfer(Currency from, Currency to) {
        if (from != to) {
            throw new ResourceConflictException(String.format(ResponseMessages.UNPAIRED_CURRENCIES, "same"));
        }
    }

    public void checkAccountsTypesBeforeMoneyTransferAndExchange(AccountType from, AccountType to) {
        AccountType accountType = AccountType.CURRENT;

        if (from != accountType || to != accountType) {
            throw new ResourceConflictException(String.format("Both accounts must be %s", accountType.getValue()));
        }

        log.info("Both accounts are {}", accountType.getValue());
    }

    private void checkAccountTypeAndDepositPeriodForPeriodBalanceUpdate(AccountType accountType, Integer depositPeriod) {
        if (accountType != AccountType.DEPOSIT) {
            throw new ResourceConflictException("Fees are for deposit accounts");
        }

        FeeUtils.checkValidityOfDepositPeriod(depositPeriod);
    }

    private void checkValidityOfBalanceAndInterestRatio(Double balance, Double interestRatio) {
        boolean isBalanceValid = balance >= LOWEST_THRESHOLD;
        boolean isInterestRatioValid = interestRatio >= LOWEST_THRESHOLD;

        if (!isBalanceValid || !isInterestRatioValid) {
            throw new ResourceConflictException(String.format("Balance and interest ratio must be greater than or equal to %s", LOWEST_THRESHOLD));
        }
    }

    private void checkAccountType(AccountDto accountDto) {
        boolean isInterestNull = isNull.test(accountDto.getInterestRatio());
        boolean isDepositPeriodNull = isNull.test(accountDto.getDepositPeriod());

        AccountType accountType = accountDto.getType();
        String message = "have interest and deposit period values";

        if ((accountType == AccountType.DEPOSIT) && (isInterestNull || isDepositPeriodNull)) {
            String exceptionMessage = accountType + " must " + message;
            throw new ResourceExpectationFailedException(exceptionMessage);
        } else if ((accountType == AccountType.CURRENT) && (!isInterestNull || !isDepositPeriodNull)) {
            String exceptionMessage = accountType + " account does not " + message;
            throw new ResourceExpectationFailedException(exceptionMessage);
        }

        if (accountType == AccountType.DEPOSIT) {
            FeeUtils.checkValidityOfDepositPeriod(accountDto.getDepositPeriod());
        } else {
            log.warn("{} account does not have deposit period", accountType.getValue());
        }
    }

    private static final Predicate<Object> isNull = Objects::isNull;
}
