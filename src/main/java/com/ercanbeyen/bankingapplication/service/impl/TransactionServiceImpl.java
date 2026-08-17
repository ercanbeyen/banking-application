package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.*;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.constant.query.SummaryField;
import com.ercanbeyen.bankingapplication.dto.TermDepositInterestRateDto;
import com.ercanbeyen.bankingapplication.dto.request.AccountActivityRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyExchangeRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyTransferRequest;
import com.ercanbeyen.bankingapplication.embeddable.Address;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.entity.AccountActivity;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceExpectationFailedException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.dto.option.TermDepositInterestRateFilteringOption;
import com.ercanbeyen.bankingapplication.repository.AccountRepository;
import com.ercanbeyen.bankingapplication.service.*;
import com.ercanbeyen.bankingapplication.util.AccountUtil;
import com.ercanbeyen.bankingapplication.util.FormatterUtil;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import com.ercanbeyen.bankingapplication.view.entity.ExchangeView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.DoublePredicate;

@RequiredArgsConstructor
@Slf4j
@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {
    private final AccountRepository accountRepository;
    private final AccountActivityService accountActivityService;
    private final ExchangeService exchangeService;
    private final DeductionService deductionService;
    private final TermDepositInterestRateService termDepositInterestRateService;
    private final CashFlowCalendarService cashFlowCalendarService;
    private final TimeZoneService timeZoneService;

    @Override
    public void createAccountActivityForAccountStatusUpdate(Account account, AccountActivityType activityType) {
        Channel channel = Channel.APP;

        Map<String, Object> summary = new HashMap<>();
        summary.put(SummaryField.ACCOUNT_ACTIVITY, activityType.getValue());
        summary.put(SummaryField.ACCOUNT_IDENTITY, account.getId());
        summary.put(SummaryField.FULL_NAME, account.getCustomer().getFullName());
        summary.put(SummaryField.NATIONAL_IDENTITY, account.getCustomer().getNationalId());
        summary.put(SummaryField.ACCOUNT_TYPE, account.getCurrency() + " " + account.getType());
        summary.put(SummaryField.BRANCH, account.getBranch().getName());
        summary.put(SummaryField.CHANNEL, channel);

        Address address = account.getBranch().getAddress();
        timeZoneService.getZoneId(address.getCountry(), address.getCity()).ifPresentOrElse(
                zoneId -> summary.put(SummaryField.TIME, LocalDateTime.now(zoneId).toString()),
                () -> summary.put(SummaryField.TIME, LocalDateTime.now(ZoneId.systemDefault()).toString())
        );

        AccountActivityRequest request = new AccountActivityRequest(activityType, null, null, 0D, summary, null, channel);
        accountActivityService.createAccountActivity(request);
    }

    @Override
    public void applyAccountActivityForSingleAccount(AccountActivityType activityType, Double amount, Account account, String cashFlowExplanation, Channel channel) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account[] accounts = new Account[2]; // first account is sender, second account is recipient
        Double transactionFee = getTransactionFee(activityType, List.of(account));
        log.info(LogMessage.ACCOUNT_ACTIVITY_STATUS_ECHO, activityType.getValue(), amount, transactionFee);

        final double previousBalance = account.getBalance();
        DoublePredicate validBalancePredicate = balance -> balance >= 0;

        double newBalance = switch (activityType) {
            case MONEY_DEPOSIT, INTEREST_INCOME -> {
                double updatedBalance = previousBalance + amount - transactionFee;

                if (!validBalancePredicate.test(updatedBalance)) {
                    throw new ResourceExpectationFailedException(ResponseMessage.TRANSACTION_FEE_CANNOT_BE_PAYED);
                }

                /* Balance update of recipient account */
                accounts[1] = account;
                yield updatedBalance;
            }
            case WITHDRAWAL -> {
                double updatedBalance = previousBalance - (amount + transactionFee);

                if (!validBalancePredicate.test(updatedBalance)) {
                    throw new ResourceExpectationFailedException(ResponseMessage.INSUFFICIENT_FUNDS);
                }

                /* Balance update of sender account */
                accounts[0] = account;
                yield updatedBalance;
            }
            default -> throw new ResourceConflictException(ResponseMessage.IMPROPER_ACCOUNT_ACTIVITY);
        };

        log.info(LogMessage.ENOUGH_BALANCE, activityType.getValue());

        updateBalance(account, newBalance);
        accountRepository.saveAndFlush(account);

        String amountInSummary = FormatterUtil.convertNumberToFormalExpression(amount);

        Map<String, Object> summary = new HashMap<>();
        summary.put(SummaryField.ACCOUNT_ACTIVITY, activityType.getValue());
        summary.put(SummaryField.FULL_NAME, account.getCustomer().getFullName());
        summary.put(SummaryField.NATIONAL_IDENTITY, account.getCustomer().getNationalId());
        summary.put(SummaryField.ACCOUNT_IDENTITY, account.getId());
        summary.put(SummaryField.AMOUNT, amountInSummary + " " + account.getCurrency());
        summary.put(SummaryField.TRANSACTION_FEE, transactionFee);
        summary.put(SummaryField.CHANNEL, channel);

        Address address = account.getBranch().getAddress();
        timeZoneService.getZoneId(address.getCountry(), address.getCity()).ifPresentOrElse(
                zoneId -> summary.put(SummaryField.TIME, LocalDateTime.now(zoneId).toString()),
                () -> summary.put(SummaryField.TIME, LocalDateTime.now(ZoneId.systemDefault()).toString())
        );

        AccountActivity accountActivity = createAccountActivity(activityType, amount, summary, accounts, null, channel);
        createAccountActivityForDeduction(transactionFee, summary, account);

        cashFlowCalendarService.createCashFlow(account.getCustomer().getCashFlowCalendar(), accountActivity, cashFlowExplanation);
    }

    @Override
    public void transferMoneyBetweenAccounts(MoneyTransferRequest request, Double amount, Account senderAccount, Account recipientAccount, Account deducteeAccount, Channel channel) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        AccountActivityType activityType = AccountActivityType.MONEY_TRANSFER;
        List<Account> accountsInMoneyTransfer = List.of(senderAccount, recipientAccount);
        double transactionFee = getTransactionFee(activityType, accountsInMoneyTransfer);
        checkBalanceBeforeMoneyTransferAndExchange(deducteeAccount, accountsInMoneyTransfer, amount, transactionFee, activityType);

        /* Balance update of sender account */
        double newBalance = senderAccount.getBalance() - amount;
        updateBalance(senderAccount, newBalance);

        /* Balance update of deductee account */
        newBalance = deducteeAccount.getBalance() - transactionFee;
        updateBalance(deducteeAccount, newBalance);

        /* Balance update of recipient account */
        newBalance = recipientAccount.getBalance() + amount;
        updateBalance(recipientAccount, newBalance);

        accountRepository.saveAllAndFlush(List.of(senderAccount, deducteeAccount, recipientAccount));

        Account[] accounts = {senderAccount, recipientAccount};
        String amountInSummary = FormatterUtil.convertNumberToFormalExpression(amount);
        final String senderWord = "Sender ";
        final String recipientWord = "Recipient ";

        Map<String, Object> summary = new HashMap<>();
        summary.put(Entity.ACCOUNT_ACTIVITY.getValue(), activityType.getValue());

        boolean areAccountsOwnedBySameCustomer = Objects.equals(senderAccount.getCustomer().getId(), recipientAccount.getCustomer().getId());

        if (areAccountsOwnedBySameCustomer) {
            summary.put(SummaryField.FULL_NAME, senderAccount.getCustomer().getFullName());
            summary.put(SummaryField.NATIONAL_IDENTITY, senderAccount.getCustomer().getNationalId());
        } else {
            summary.put(senderWord + SummaryField.FULL_NAME, senderAccount.getCustomer().getFullName());
            summary.put(recipientWord + SummaryField.FULL_NAME, recipientAccount.getCustomer().getFullName());
            summary.put(senderWord + SummaryField.NATIONAL_IDENTITY, senderAccount.getCustomer().getNationalId());
            summary.put(recipientWord + SummaryField.NATIONAL_IDENTITY, recipientAccount.getCustomer().getNationalId());
            summary.put(SummaryField.PAYMENT_TYPE, request.paymentType().getValue());
        }

        summary.put(senderWord + SummaryField.ACCOUNT_IDENTITY, senderAccount.getId());
        summary.put(recipientWord + SummaryField.ACCOUNT_IDENTITY, recipientAccount.getId());

        putDeducteeAccountInformationIntoSummary(senderAccount, deducteeAccount, summary);

        summary.put(SummaryField.AMOUNT, amountInSummary + " " + senderAccount.getCurrency());
        summary.put(SummaryField.TRANSACTION_FEE, transactionFee + " " + Currency.getDeductionCurrency());
        summary.put(SummaryField.CHANNEL, channel);

        Address address = senderAccount.getBranch().getAddress();
        timeZoneService.getZoneId(address.getCountry(), address.getCity()).ifPresentOrElse(
                zoneId -> summary.put(senderWord + SummaryField.TIME, LocalDateTime.now(zoneId).toString()),
                () -> summary.put(SummaryField.TIME, LocalDateTime.now(ZoneId.systemDefault()).toString())
        );

        address = recipientAccount.getBranch().getAddress();
        timeZoneService.getZoneId(address.getCountry(), address.getCity()).ifPresentOrElse(
                zoneId -> summary.put(recipientWord + SummaryField.TIME, LocalDateTime.now(zoneId).toString()),
                () -> summary.put(SummaryField.TIME, LocalDateTime.now(ZoneId.systemDefault()).toString())
        );

        AccountActivity accountActivity = createAccountActivity(activityType, request.amount(), summary, accounts, request.explanation(), channel);
        createAccountActivityForDeduction(transactionFee, summary, deducteeAccount);

        if (!areAccountsOwnedBySameCustomer) {
            String entity = Entity.ACCOUNT.getValue();
            String explanation = entity + " " + senderAccount.getId() + " sent " + amountInSummary + " " + senderAccount.getCurrency();
            cashFlowCalendarService.createCashFlow(senderAccount.getCustomer().getCashFlowCalendar(), accountActivity, explanation);
            explanation = entity + " " + recipientAccount.getId() + " received " + amountInSummary + recipientAccount.getCurrency();
            cashFlowCalendarService.createCashFlow(recipientAccount.getCustomer().getCashFlowCalendar(), accountActivity, explanation);
        }
    }

    @Override
    public void exchangeMoneyBetweenAccounts(MoneyExchangeRequest request, Account sellerAccount, Account buyerAccount, Account deducteeAccount, Channel channel) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        AccountActivityType activityType = AccountActivityType.MONEY_EXCHANGE;

        List<Account> accountsInMoneyExchange = List.of(sellerAccount, buyerAccount);
        double transactionFee = getTransactionFee(activityType, accountsInMoneyExchange);
        checkBalanceBeforeMoneyTransferAndExchange(deducteeAccount, accountsInMoneyExchange, request.amount(), transactionFee, activityType);

        Currency sellerAccountCurrency = sellerAccount.getCurrency();
        Currency buyerAccountCurrency = buyerAccount.getCurrency();
        Double rate = exchangeService.getBankExchangeRate(sellerAccountCurrency, buyerAccountCurrency);
        Double spentAmount = request.amount();
        Double earnedAmount = exchangeService.convertMoneyBetweenCurrencies(sellerAccountCurrency, buyerAccountCurrency, spentAmount);

        /* Balance update of seller account */
        double newBalance = sellerAccount.getBalance() - spentAmount;
        updateBalance(sellerAccount, newBalance);

        /* Balance update of deductee account */
        newBalance = deducteeAccount.getBalance() - transactionFee;
        updateBalance(deducteeAccount, newBalance);

        /* Balance update of recipient account */
        newBalance = buyerAccount.getBalance() + earnedAmount;
        updateBalance(buyerAccount, newBalance);

        accountRepository.saveAllAndFlush(List.of(sellerAccount, deducteeAccount, buyerAccount));

        String spentAmountInSummary = FormatterUtil.convertNumberToFormalExpression(spentAmount);
        log.info(LogMessage.PROCESSED_AMOUNT, spentAmountInSummary, "Spent");

        String earnedAmountInSummary = FormatterUtil.convertNumberToFormalExpression(earnedAmount);
        log.info(LogMessage.PROCESSED_AMOUNT, earnedAmountInSummary, "Earn");

        Account[] accounts = {sellerAccount, buyerAccount};

        Map<String, Object> summary = new HashMap<>();
        summary.put(Entity.ACCOUNT_ACTIVITY.getValue(), activityType.getValue());
        summary.put(SummaryField.FULL_NAME, sellerAccount.getCustomer().getFullName());
        summary.put(SummaryField.NATIONAL_IDENTITY, sellerAccount.getCustomer().getNationalId());

        String sellerWord = "Seller ";
        String buyerWord = "Buyer ";
        summary.put(sellerWord + SummaryField.ACCOUNT_IDENTITY, sellerAccount.getId());
        summary.put(buyerWord + SummaryField.ACCOUNT_IDENTITY, buyerAccount.getId());

        putDeducteeAccountInformationIntoSummary(sellerAccount, deducteeAccount, summary);

        summary.put("Spent " + SummaryField.AMOUNT, spentAmountInSummary + " " + sellerAccountCurrency);
        summary.put("Earned " + SummaryField.AMOUNT, earnedAmountInSummary + " " + buyerAccountCurrency);
        summary.put(SummaryField.RATE, FormatterUtil.convertNumberToFormalExpression(rate));

        ExchangeView exchangeView = exchangeService.getExchangeView(sellerAccountCurrency, buyerAccountCurrency);

        summary.put(SummaryField.TRANSACTION_FEE, transactionFee + " " + Currency.getDeductionCurrency());
        summary.put(SummaryField.CHANNEL, channel);

        Address addressOfTargetCurrency;
        String timeKey;

        if (exchangeView.getTargetCurrency() == sellerAccountCurrency) {
            addressOfTargetCurrency = sellerAccount.getBranch().getAddress();
            timeKey = sellerWord + SummaryField.TIME;
        } else {
            addressOfTargetCurrency = buyerAccount.getBranch().getAddress();
            timeKey = buyerWord + SummaryField.TIME;
        }

        timeZoneService.getZoneId(addressOfTargetCurrency.getCountry(), addressOfTargetCurrency.getCity()).ifPresentOrElse(
                zoneId -> summary.put(timeKey, LocalDateTime.now(zoneId).toString()),
                () -> summary.put(SummaryField.TIME, LocalDateTime.now(ZoneId.systemDefault()).toString())
        );

        createAccountActivity(activityType, earnedAmount, summary, accounts, null, channel);
        createAccountActivityForDeduction(transactionFee, summary, deducteeAccount);
    }

    @Override
    public void updateDepositAccountFields(Account account, double balance, int depositMaturity) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        double interestRate = getInterestRate(account.getCurrency(), balance, depositMaturity);
        double balanceAfterNextInterestIncome = AccountUtil.calculateBalanceAfterNextInterestIncome(balance, depositMaturity, interestRate);

        account.setInterestRate(interestRate);
        account.setDepositMaturity(depositMaturity);
        account.setBalanceAfterNextInterestIncome(balanceAfterNextInterestIncome);

        log.info("{} {} related fields are updated", AccountType.DEPOSIT.getValue(), Entity.ACCOUNT.getValue());
    }

    private void updateBalance(Account account, double balance) {
        if (AccountUtil.checkAccountTypeMatch.test(account.getType(), AccountType.DEPOSIT)) {
            log.info(LogMessage.DEPOSIT_ACCOUNT_FIELDS_SHOULD_UPDATE);
            updateDepositAccountFields(account, balance, account.getDepositMaturity());
        }

        account.setBalance(balance);
        log.info("{} balance is updated", Entity.ACCOUNT.getValue());
    }

    private double getInterestRate(Currency currency, double balance, int depositMaturity) {
        double interestRate = 0;

        /* Match interest rate for the given currency, balance and deposit maturity */
        try {
            interestRate = termDepositInterestRateService.getInterestRate(currency, depositMaturity, balance);
        } catch (ResourceNotFoundException _) {
            TermDepositInterestRateFilteringOption filteringOption = new TermDepositInterestRateFilteringOption();
            filteringOption.setCurrency(currency);
            filteringOption.setDepositMaturity(depositMaturity);

            String entity = Entity.TERM_DEPOSIT_INTEREST_RATE.getValue();
            String exceptionMessage = "No %s balance in the " + entity.toLowerCase();

            /* Less than minimum balance of Term Deposit Interest Rate */
            double minimumBalance = termDepositInterestRateService.getEntities(filteringOption)
                    .stream()
                    .mapToDouble(TermDepositInterestRateDto::getMinimumBalance)
                    .min()
                    .orElseThrow(() -> new ResourceNotFoundException(String.format(exceptionMessage, "minimum")));

            if (balance < minimumBalance) {
                log.info("Balance is less than the minimum {} balance for deposit maturity {}. Therefore, interest rate is {}", entity.toLowerCase(), depositMaturity, interestRate);
                return interestRate;
            }

            /* Greater than maximum balance of Term Deposit Interest Rate */
            double maximumBalance = termDepositInterestRateService.getEntities(filteringOption)
                    .stream()
                    .mapToDouble(TermDepositInterestRateDto::getMaximumBalance)
                    .max()
                    .orElseThrow(() -> new ResourceNotFoundException(String.format(exceptionMessage, "maximum")));

            if (balance > maximumBalance) {
                log.info("Balance is greater than or equal to the maximum balance for deposit maturity {}. Therefore, interest rate of the maximum {} balance", depositMaturity, entity.toLowerCase());
                return termDepositInterestRateService.getInterestRate(currency, depositMaturity, maximumBalance);
            }

            /* Unmatched Term Deposit Interest Rate Balance Case */
            log.error("Unexpected condition! There are unmatched {} balances. Balance: {} & Deposit Maturity: {}", entity.toLowerCase(), balance, depositMaturity);
            throw new ResourceNotFoundException(entity + " balance is not found for " + balance);
        }

        return interestRate;
    }

    private double getTransactionFee(AccountActivityType activityType, List<Account> accounts) {
        boolean sameCustomerTransferMoneyBetweenAccounts = activityType == AccountActivityType.MONEY_TRANSFER
                && accounts.getFirst().getCustomer().getNationalId().equals(accounts.getLast().getCustomer().getNationalId());

        if (sameCustomerTransferMoneyBetweenAccounts) {
            log.warn("There is no transaction fee when transferring money between accounts of the same customer");
            return 0;
        }

        return deductionService.getDeduction(activityType).amount();
    }

    private void createAccountActivityForDeduction(Double transactionFee, Map<String, Object> summary, Account deducteeAccount) {
        if (transactionFee == 0) {
            log.warn("There is no transaction fee");
            return;
        }

        Account[] accounts = new Account[2];
        accounts[0] = deducteeAccount;

        createAccountActivity(AccountActivityType.DEDUCTION, transactionFee, summary, accounts, null, Channel.AUTOMATIC);
    }

    private AccountActivity createAccountActivity(AccountActivityType activityType, Double amount, Map<String, Object> summary, Account[] accounts, String explanation, Channel channel) {
        AccountActivityRequest accountActivityRequest = new AccountActivityRequest(activityType, accounts[0], accounts[1], amount, summary, explanation, channel);
        return accountActivityService.createAccountActivity(accountActivityRequest);
    }

    private void checkBalanceBeforeMoneyTransferAndExchange(Account deducteeAccount, List<Account> relatedAccounts, Double amount, Double transactionFee, AccountActivityType activityType) {
        log.info(LogMessage.ACCOUNT_ACTIVITY_STATUS_ECHO, activityType.getValue(), amount, transactionFee);
        String entity = Entity.ACCOUNT.getValue().toLowerCase();

        if (Objects.equals(deducteeAccount.getId(), relatedAccounts.getFirst().getId())) {
            log.info("Extra deductee {} does not exist", entity);

            if (deducteeAccount.getBalance() < (amount + transactionFee)) {
                throw new ResourceExpectationFailedException(ResponseMessage.INSUFFICIENT_FUNDS);
            }
        } else {
            log.info("Extra deductee {} exists", entity);

            if (deducteeAccount.getBalance() < transactionFee) {
                throw new ResourceExpectationFailedException(ResponseMessage.TRANSACTION_FEE_CANNOT_BE_PAYED);
            }

            if (relatedAccounts.getFirst().getBalance() < amount) {
                throw new ResourceExpectationFailedException(ResponseMessage.INSUFFICIENT_FUNDS);
            }
        }

        log.info(LogMessage.ENOUGH_BALANCE, activityType.getValue());
    }

    private void putDeducteeAccountInformationIntoSummary(Account relatedAccount, Account deducteeAccount, Map<String, Object> summary) {
        String entity = Entity.ACCOUNT.getValue().toLowerCase();

        if (relatedAccount.getId().equals(deducteeAccount.getId())) {
            log.warn("Deduction and related {}s are same, so no need to add it into summary", entity);
            return;
        }

        log.info("There is a separate deductee {}, so add it into summary", entity);
        final String deducteeWord = "Deductee ";

        summary.put(deducteeWord + SummaryField.ACCOUNT_IDENTITY, deducteeAccount.getId());

        Address address = deducteeAccount.getBranch().getAddress();
        timeZoneService.getZoneId(address.getCountry(), address.getCity()).ifPresentOrElse(
                zoneId -> summary.put(deducteeWord + SummaryField.TIME, LocalDateTime.now(zoneId).toString()),
                () -> summary.put(SummaryField.TIME, LocalDateTime.now(ZoneId.systemDefault()).toString())
        );
    }
}
