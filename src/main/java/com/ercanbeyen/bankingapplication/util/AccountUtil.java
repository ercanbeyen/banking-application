package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.constant.enums.*;
import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.AccountDto;
import com.ercanbeyen.bankingapplication.dto.ChannelInformation;
import com.ercanbeyen.bankingapplication.dto.request.MoneyExchangeRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyTransferRequest;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceExpectationFailedException;
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

    public void checkMoneyTransferRequest(MoneyTransferRequest moneyTransferRequest, ChannelInformation channelInformation) {
        AccountActivityType accountActivityType = AccountActivityType.MONEY_TRANSFER;

        checkAccountActivityWithChannelType(channelInformation, accountActivityType);
        checkHeaderParametersForMoneyTransferAndMoneyExchange(channelInformation);

        if (Objects.equals(moneyTransferRequest.senderAccountId(), moneyTransferRequest.recipientAccountId())) {
            throw new BadRequestException("Identity of sender and recipient accounts should not be equal");
        }

        Double maximumMoneyTransferAmountPerRequest = AccountActivityType.getMaximumAmountPerRequestOfActivity(accountActivityType);

        if (moneyTransferRequest.amount() >= maximumMoneyTransferAmountPerRequest) {
            String formattedValue = FormatterUtil.convertNumberToFormalExpression(maximumMoneyTransferAmountPerRequest);
            throw new ResourceExpectationFailedException(String.format("Maximum %s limit per request (%s) is exceeded", accountActivityType.getValue(), formattedValue));
        }
    }

    public void checkMoneyExchangeRequest(MoneyExchangeRequest moneyExchangeRequest, ChannelInformation channelInformation) {
        AccountActivityType accountActivityType = AccountActivityType.MONEY_EXCHANGE;

        checkAccountActivityWithChannelType(channelInformation, accountActivityType);
        checkHeaderParametersForMoneyTransferAndMoneyExchange(channelInformation);

        if (Objects.equals(moneyExchangeRequest.sellerAccountId(), moneyExchangeRequest.buyerAccountId())) {
            throw new BadRequestException("Identity of seller and buyer accounts should not be equal");
        }

        Double maximumMoneyExchangeAmountPerRequest = AccountActivityType.getMaximumAmountPerRequestOfActivity(accountActivityType);

        if (moneyExchangeRequest.amount() >= maximumMoneyExchangeAmountPerRequest) {
            String formattedValue = FormatterUtil.convertNumberToFormalExpression(maximumMoneyExchangeAmountPerRequest);
            throw new ResourceExpectationFailedException(String.format("Maximum %s limit per request (%s) is exceeded", accountActivityType.getValue(), formattedValue));
        }
    }

    public void checkAccountActivityWithChannelType(ChannelInformation channelInformation, AccountActivityType accountActivityType) {
        ChannelType channelType = channelInformation.channelType();
        List<ChannelType> channelTypes = accountActivityType.getAvailableChannelTypes();

        if (!channelTypes.contains(channelType)) {
            throw new BadRequestException("Invalid channel type!");
        }
    }

    public void checkAccountActivityAndAccountTypeMatch(AccountType givenAccountType, AccountType expectedAccountType, AccountActivityType accountActivityType) {
        if (!checkAccountTypeMatch.test(givenAccountType, expectedAccountType)) {
            throw new ResourceConflictException(accountActivityType.getValue() + " can only be done from " + expectedAccountType.getValue() + " Accounts");
        }

        log.info("Account Type {} can apply account activity {}", givenAccountType.getValue(), accountActivityType.getValue());
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

    public void checkTypesOfAccountsBeforeMoneyTransferAndExchange(AccountType from, AccountType to, AccountActivityType accountActivityType) {
        AccountType expectedAccountType = AccountType.CURRENT;

        checkAccountActivityAndAccountTypeMatch(from, expectedAccountType, accountActivityType);
        checkAccountActivityAndAccountTypeMatch(to, expectedAccountType, accountActivityType);

        log.info("Both accounts are {}", expectedAccountType.getValue());
    }

    public final BiPredicate<AccountType, AccountType> checkAccountTypeMatch = (givenAccountType, expectedAccountType) -> givenAccountType == expectedAccountType;

    private void checkHeaderParametersForMoneyTransferAndMoneyExchange(ChannelInformation channelInformation) {
        ChannelType channelType = channelInformation.channelType();
        Integer channelId = channelInformation.channelId();

        List<ChannelType> channelsRequiredChannelId = new ArrayList<>(ChannelType.channelsRequiredChannelId());
        List<ChannelType> channelsNotRequiredChannelId = new ArrayList<>(Arrays.asList(ChannelType.values()));
        channelsNotRequiredChannelId.removeAll(channelsRequiredChannelId);

        if (channelsRequiredChannelId.contains(channelType) && channelId == null) {
            throw new BadRequestException("Channel Id is required for " + channelsRequiredChannelId);
        }

        if (channelsNotRequiredChannelId.contains(channelType) && channelId != null) {
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

    private final Predicate<Object> isNull = Objects::isNull;
}
