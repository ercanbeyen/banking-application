package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.AccountType;
import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessages;
import com.ercanbeyen.bankingapplication.dto.AccountDto;
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
    private final List<Integer> DEPOSIT_PERIODS = List.of(1, 3, 6, 12);
    private final Double MAXIMUM_TRANSFER_LIMIT_PER_REQUEST = 100_000D;
    private final double LOWEST_THRESHOLD = 0;

    public void checkRequest(AccountDto accountDto) {
        checkAccountType(accountDto);

        AccountType accountType = accountDto.getType();

        if (accountType == AccountType.DEPOSIT) {
            checkValidityOfDepositPeriod(accountDto.getDepositPeriod());
        } else {
            log.warn("{} account does not have deposit period", accountType.getValue());
        }

        Double balance = accountDto.getBalance();

        if (balance != null && balance != 0) {
            throw new ResourceConflictException("Not any balance value should be assigned directly from request");
        }
    }

    public void checkMoneyTransferRequest(MoneyTransferRequest request) {
        if (Objects.equals(request.senderAccountId(), request.receiverAccountId())) {
            throw new ResourceExpectationFailedException("Identity of sender and receiver accounts should not be equal");
        }

        if (request.amount() >= MAXIMUM_TRANSFER_LIMIT_PER_REQUEST) {
            String formattedValue = NumberFormatterUtil.convertNumberToFormalExpression(MAXIMUM_TRANSFER_LIMIT_PER_REQUEST);
            throw new ResourceExpectationFailedException(String.format("Maximum %s limit per request (%s) is exceeded", AccountActivityType.MONEY_TRANSFER.getValue(), formattedValue));
        }
    }

    public void checkAccountActivityForCurrentAccount(AccountActivityType activityType) {
        if (activityType != AccountActivityType.MONEY_DEPOSIT && activityType != AccountActivityType.WITHDRAWAL) {
            throw new ResourceConflictException(ResponseMessages.IMPROPER_ACCOUNT_ACTIVITY);
        }
    }

    public void checkCurrentAccountBeforeUpdateBalance(Double balance, Double request, AccountActivityType activityType) {
        if (activityType == AccountActivityType.WITHDRAWAL) {
            AccountUtils.checkBalance(balance, request);
        } else {
            log.warn("{} is {}. So no need to check balance", Entity.ACCOUNT_ACTIVITY.getValue(), activityType.getValue());
        }
    }

    public void checkBalance(Double balance, Double threshold) {
        if (Optional.ofNullable(threshold).isEmpty()) {
            threshold = LOWEST_THRESHOLD;
        }

        if (balance < threshold) {
            throw new ResourceExpectationFailedException("Insufficient funds");
        }
    }

    public double calculateInterest(Double balance, Double interestRatio) {
        checkValidityOfBalanceAndInterestRatio(balance, interestRatio);
        return (balance == LOWEST_THRESHOLD || interestRatio == LOWEST_THRESHOLD) ? LOWEST_THRESHOLD : ((interestRatio * balance) / 100);
    }

    public boolean checkAccountForPeriodicMoneyAdd(AccountType accountType, LocalDateTime updatedAt, Integer depositPeriod) {
        checkAccountTypeAndDepositPeriodForPeriodBalanceUpdate(accountType, depositPeriod);

        LocalDate isGoingToBeUpdatedAt = updatedAt
                .toLocalDate()
                .plusMonths(depositPeriod);

        return isGoingToBeUpdatedAt.isEqual(LocalDate.now());
    }

    public void checkCurrencies(Currency from, Currency to) {
        if (from != to) {
            throw new ResourceConflictException(String.format(ResponseMessages.UNPAIRED_CURRENCIES, "same"));
        }
    }

    private void checkAccountTypeAndDepositPeriodForPeriodBalanceUpdate(AccountType accountType, Integer depositPeriod) {
        if (accountType != AccountType.DEPOSIT) {
            throw new ResourceConflictException("Fees are for deposit accounts");
        }

        checkValidityOfDepositPeriod(depositPeriod);
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
    }

    private static void checkValidityOfDepositPeriod(Integer depositPeriod) {
        if (!DEPOSIT_PERIODS.contains(depositPeriod)) {
            throw new ResourceExpectationFailedException("Deposit period is invalid. Valid values are " + DEPOSIT_PERIODS);
        }
    }

    private static final Predicate<Object> isNull = Objects::isNull;
}
