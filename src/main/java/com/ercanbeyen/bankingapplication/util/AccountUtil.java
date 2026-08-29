package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.constant.enums.*;
import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.constant.query.HeaderField;
import com.ercanbeyen.bankingapplication.dto.AccountDto;
import com.ercanbeyen.bankingapplication.dto.request.MoneyExchangeRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyTransferRequest;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceExpectationFailedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

@Slf4j
@UtilityClass
public class AccountUtil {
    private final int CHANNEL_ID_DOES_NOT_EXIST = -1;

    public void checkRequest(AccountDto accountDto) {
        if (Optional.ofNullable(accountDto.getIsBlocked()).isPresent() || Optional.ofNullable(accountDto.getClosedAt()).isPresent()) {
            throw new BadRequestException("Request should not contain block and closed at statuses");
        }

        checkAccountType(accountDto);
        Double balance = accountDto.getBalance();

        if (Optional.ofNullable(balance).isPresent() && balance != 0) {
            throw new BadRequestException("Not any balance value should be assigned directly from request");
        }
    }

    public void checkMoneyDepositAndWithdrawalRequests(HttpServletRequest httpServletRequest) {
        ChannelType channelType = getChannelType(httpServletRequest);
        int channelId = httpServletRequest.getIntHeader(HeaderField.CHANNEL_ID);

        if (channelType != ChannelType.BRANCH && channelType != ChannelType.ATM) {
            throw new BadRequestException("Invalid channel type!");
        }

        if (channelId == CHANNEL_ID_DOES_NOT_EXIST) {
            throw new BadRequestException("Channel Id does not exist!");
        }
    }

    public void checkMoneyTransferRequest(MoneyTransferRequest moneyTransferRequest, HttpServletRequest httpServletRequest) {
        checkHeaderParametersForMoneyTransferAndMoneyExchange(httpServletRequest);

        if (Objects.equals(moneyTransferRequest.senderAccountId(), moneyTransferRequest.recipientAccountId())) {
            throw new BadRequestException("Identity of sender and recipient accounts should not be equal");
        }

        AccountActivityType activityType = AccountActivityType.MONEY_TRANSFER;
        Double maximumMoneyTransferAmountPerRequest = AccountActivityType.getMaximumAmountPerRequestOfActivity(activityType);

        if (moneyTransferRequest.amount() >= maximumMoneyTransferAmountPerRequest) {
            String formattedValue = FormatterUtil.convertNumberToFormalExpression(maximumMoneyTransferAmountPerRequest);
            throw new ResourceExpectationFailedException(String.format("Maximum %s limit per request (%s) is exceeded", activityType.getValue(), formattedValue));
        }
    }

    public void checkMoneyExchangeRequest(MoneyExchangeRequest moneyExchangeRequest, HttpServletRequest httpServletRequest) {
        checkHeaderParametersForMoneyTransferAndMoneyExchange(httpServletRequest);

        if (Objects.equals(moneyExchangeRequest.sellerAccountId(), moneyExchangeRequest.buyerAccountId())) {
            throw new BadRequestException("Identity of seller and buyer accounts should not be equal");
        }

        AccountActivityType activityType = AccountActivityType.MONEY_EXCHANGE;
        Double maximumMoneyExchangeAmountPerRequest = AccountActivityType.getMaximumAmountPerRequestOfActivity(activityType);

        if (moneyExchangeRequest.amount() >= maximumMoneyExchangeAmountPerRequest) {
            String formattedValue = FormatterUtil.convertNumberToFormalExpression(maximumMoneyExchangeAmountPerRequest);
            throw new ResourceExpectationFailedException(String.format("Maximum %s limit per request (%s) is exceeded", activityType.getValue(), formattedValue));
        }
    }

    public void checkAccountActivityAndAccountTypeMatch(AccountType givenAccountType, AccountType expectedAccountType, AccountActivityType activityType) {
        if (!checkAccountTypeMatch.test(givenAccountType, expectedAccountType)) {
            throw new ResourceConflictException(activityType.getValue() + " can only be done from " + expectedAccountType.getValue() + " Accounts");
        }

        log.info("Account Type {} can apply account activity {}", givenAccountType.getValue(), activityType.getValue());
    }

    public double calculateInterestIncome(Double balance, Integer depositMaturity, Double interestRate) {
        checkValidityOfBalanceAndInterestRate(balance, interestRate);
        double interestIncome = (balance / 100) * (interestRate / 12) * depositMaturity;
        log.info("Interest after calculation with balance ({}), interest rate ({}) and deposit maturity ({}): {}", balance, interestRate, depositMaturity, interestIncome);
        return interestIncome;
    }

    public double calculateBalanceAfterNextInterestIncome(Double balance, Integer depositMaturity, Double interestRate) {
        double interestIncome = AccountUtil.calculateInterestIncome(balance, depositMaturity, interestRate);
        double balanceAfterNextInterestIncome = balance + interestIncome;
        log.info("Balance after interest income: {}", balanceAfterNextInterestIncome);
        return balanceAfterNextInterestIncome;
    }

    public boolean checkAccountForPeriodicMoneyAdd(AccountType accountType, LocalDateTime updatedAt, Integer depositMaturity) {
        checkAccountTypeAndDepositMaturityForPeriodBalanceUpdate(accountType, depositMaturity);
        LocalDate isGoingToBeUpdatedAt = updatedAt.toLocalDate().plusMonths(depositMaturity);
        return isGoingToBeUpdatedAt.isEqual(LocalDate.now(ZoneId.systemDefault()));
    }

    public void checkCurrenciesBeforeMoneyTransfer(Currency from, Currency to) {
        if (from != to) {
            throw new ResourceConflictException(String.format(ResponseMessage.UNPAIRED_CURRENCIES, "same"));
        }
    }

    public void checkTypesOfAccountsBeforeMoneyTransferAndExchange(AccountType from, AccountType to, AccountActivityType activityType) {
        AccountType expectedAccountType = AccountType.CURRENT;

        checkAccountActivityAndAccountTypeMatch(from, expectedAccountType, activityType);
        checkAccountActivityAndAccountTypeMatch(to, expectedAccountType, activityType);

        log.info("Both accounts are {}", expectedAccountType.getValue());
    }

    public final BiPredicate<AccountType, AccountType> checkAccountTypeMatch = (givenAccountType, expectedAccountType) -> givenAccountType == expectedAccountType;

    private void checkHeaderParametersForMoneyTransferAndMoneyExchange(HttpServletRequest request) {
        ChannelType channelType = getChannelType(request);

        int channelId = request.getIntHeader(HeaderField.CHANNEL_ID);
        List<ChannelType> channelsRequiredChannelId = new ArrayList<>(Arrays.asList(ChannelType.BRANCH, ChannelType.ATM));
        List<ChannelType> channelsNotRequiredChannelId = new ArrayList<>(Arrays.asList(ChannelType.values()));
        channelsNotRequiredChannelId.removeAll(channelsRequiredChannelId);

        if (channelsRequiredChannelId.contains(channelType) && channelId == CHANNEL_ID_DOES_NOT_EXIST) {
            throw new BadRequestException("Channel Id is required for " + channelsRequiredChannelId);
        }

        if (channelsNotRequiredChannelId.contains(channelType) && channelId != CHANNEL_ID_DOES_NOT_EXIST) {
            throw new BadRequestException("Channel Id should not be there for " + channelsNotRequiredChannelId);
        }
    }

    private void checkAccountTypeAndDepositMaturityForPeriodBalanceUpdate(AccountType accountType, Integer depositMaturity) {
        checkAccountActivityAndAccountTypeMatch(accountType, AccountType.DEPOSIT, AccountActivityType.INTEREST_INCOME);
        TermDepositInterestRateUtil.checkValidityOfDepositMaturity(depositMaturity);
    }

    private void checkValidityOfBalanceAndInterestRate(Double balance, Double interestRate) {
        boolean isBalanceValid = balance >= 0;
        boolean isInterestRateValid = interestRate >= 0;

        if (!isBalanceValid || !isInterestRateValid) {
            throw new BadRequestException(String.format("Balance and interest rate must be greater than or equal to %s", 0));
        }
    }

    private void checkAccountType(AccountDto accountDto) {
        checkOptionalFieldsOfAccount(accountDto);

        if (accountDto.getType() == AccountType.DEPOSIT) {
            TermDepositInterestRateUtil.checkValidityOfDepositMaturity(accountDto.getDepositMaturity());
            accountDto.setBalanceAfterNextInterestIncome(0D);
        } else {
            log.warn("{} account does not have deposit maturity", accountDto.getType().getValue());
        }
    }

    private void checkOptionalFieldsOfAccount(AccountDto accountDto) {
        boolean isInterestNull = isNull.test(accountDto.getInterestRate());
        boolean isDepositPeriodNull = isNull.test(accountDto.getDepositMaturity());

        AccountType accountType = accountDto.getType();
        String message = "have interest and deposit maturity values";

        if ((accountType == AccountType.DEPOSIT) && (isInterestNull || isDepositPeriodNull)) {
            String exceptionMessage = accountType.getValue() + " must " + message;
            throw new ResourceExpectationFailedException(exceptionMessage);
        } else if ((accountType == AccountType.CURRENT) && (!isInterestNull || !isDepositPeriodNull)) {
            String exceptionMessage = accountType + " " + Entity.ACCOUNT.getValue().toLowerCase() + " does not " + message;
            throw new ResourceExpectationFailedException(exceptionMessage);
        }
    }

    private ChannelType getChannelType(HttpServletRequest httpServletRequest) {
        try {
            String header = httpServletRequest.getHeader(HeaderField.CHANNEL_TYPE);

            if (Optional.ofNullable(header).isEmpty()) {
                throw new IllegalArgumentException("Channel type is null");
            }

            return ChannelType.valueOf(header);
        } catch (IllegalArgumentException _) {
            throw new BadRequestException("Channel type is not found!");
        }
    }

    private final Predicate<Object> isNull = Objects::isNull;
}
