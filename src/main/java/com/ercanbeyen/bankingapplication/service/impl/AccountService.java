package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.*;
import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.constant.message.LogMessages;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessages;
import com.ercanbeyen.bankingapplication.dto.AccountActivityDto;
import com.ercanbeyen.bankingapplication.dto.AccountDto;
import com.ercanbeyen.bankingapplication.dto.NotificationDto;
import com.ercanbeyen.bankingapplication.dto.request.ExchangeRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyTransferRequest;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.entity.Branch;
import com.ercanbeyen.bankingapplication.entity.Customer;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.AccountMapper;
import com.ercanbeyen.bankingapplication.option.AccountActivityFilteringOptions;
import com.ercanbeyen.bankingapplication.option.AccountFilteringOptions;
import com.ercanbeyen.bankingapplication.repository.AccountRepository;
import com.ercanbeyen.bankingapplication.dto.response.CustomerStatisticsResponse;
import com.ercanbeyen.bankingapplication.service.AccountActivityService;
import com.ercanbeyen.bankingapplication.service.BaseService;
import com.ercanbeyen.bankingapplication.service.NotificationService;
import com.ercanbeyen.bankingapplication.util.AccountUtils;
import com.ercanbeyen.bankingapplication.util.LoggingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService implements BaseService<AccountDto, AccountFilteringOptions> {
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final CustomerService customerService;
    private final TransactionService transactionService;
    private final NotificationService notificationService;
    private final AccountActivityService accountActivityService;
    private final BranchService branchService;

    @Override
    public List<AccountDto> getEntities(AccountFilteringOptions options) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Predicate<Account> accountPredicate = account -> {
            boolean typeFilter = (options.getType() == null || options.getType() == account.getType());
            boolean timeFilter = (options.getCreatedAt() == null || options.getCreatedAt().toLocalDate().isEqual(options.getCreatedAt().toLocalDate()));
            boolean closedFilter = (options.getIsClosed() == null || options.getIsClosed() == (account.getClosedAt() != null));
            return typeFilter && timeFilter && closedFilter;
        };

        return accountRepository.findAll()
                .stream()
                .filter(accountPredicate)
                .map(accountMapper::entityToDto)
                .toList();
    }

    @Override
    public AccountDto getEntity(Integer id) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());
        Account account = findById(id);
        return accountMapper.entityToDto(account);
    }

    @Transactional
    @Override
    public AccountDto createEntity(AccountDto request) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Account account = accountMapper.dtoToEntity(request);
        Customer customer = customerService.findByNationalId(request.getCustomerNationalId());
        Branch branch = branchService.findByName(request.getBranchName());

        account.setCustomer(customer);
        account.setBranch(branch);

        Account savedAccount = accountRepository.save(account);
        log.info(LogMessages.RESOURCE_CREATE_SUCCESS, Entity.ACCOUNT.getValue(), savedAccount.getId());

        transactionService.createAccountActivityForAccountStatusUpdate(account, AccountActivityType.ACCOUNT_OPENING);

        return accountMapper.entityToDto(savedAccount);
    }

    @Override
    public AccountDto updateEntity(Integer id, AccountDto request) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Account account = findById(id);
        checkAccountStatus(account);
        AccountUtils.checkCurrencies(account.getCurrency(), request.getCurrency());

        Branch branch = branchService.findByName(request.getBranchName());

        account.setBranch(branch);
        account.setInterestRatio(request.getInterestRatio());
        account.setDepositPeriod(request.getDepositPeriod());

        return accountMapper.entityToDto(accountRepository.save(account));
    }

    @Override
    public void deleteEntity(Integer id) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        if (!accountRepository.existsById(id)) {
            throw new ResourceNotFoundException(String.format(ResponseMessages.NOT_FOUND, Entity.ACCOUNT.getValue()));
        }

        log.info(LogMessages.RESOURCE_FOUND, Entity.ACCOUNT.getValue());

        accountRepository.deleteById(id);
        log.info(LogMessages.RESOURCE_DELETE_SUCCESS, Entity.ACCOUNT.getValue(), id);
    }

    public String updateBalanceOfCurrentAccount(Integer id, AccountActivityType activityType, Double amount) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Account account = findById(id);
        checkAccountStatus(account);
        AccountUtils.checkCurrentAccountBeforeUpdateBalance(account.getBalance(), amount, activityType);
        checkDailyAccountActivityLimit(account, amount, activityType);

        transactionService.updateBalanceOfSingleAccount(activityType, amount, account);

        return String.format(ResponseMessages.SUCCESS, activityType.getValue());
    }

    public String updateBalanceOfDepositAccount(Integer id) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Account account = findById(id);
        checkAccountStatus(account);

        if (!AccountUtils.checkAccountForPeriodicMoneyAdd(account.getType(), account.getUpdatedAt(), account.getDepositPeriod())) {
            log.warn("Deposit period is not completed");
            return "Today is not the completion of deposit period";
        }

        Double amount = AccountUtils.calculateInterest(account.getBalance(), account.getInterestRatio());
        AccountActivityType activityType = AccountActivityType.FEE;

        transactionService.updateBalanceOfSingleAccount(activityType, amount, account);

        NotificationDto notificationDto = new NotificationDto(account.getCustomer().getNationalId(), String.format("Term of your %s is deposit account has been renewed.", account.getCurrency()));
        notificationService.createNotification(notificationDto);

        String response = activityType.getValue() + " transfer";

        return String.format(ResponseMessages.SUCCESS, response);
    }

    public String transferMoney(MoneyTransferRequest request) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Integer senderAccountId = request.senderAccountId();
        Integer receiverAccountId = request.receiverAccountId();

        Account senderAccount = findById(senderAccountId);
        checkAccountStatus(senderAccount);

        Account receiverAccount = findById(receiverAccountId);
        checkAccountStatus(receiverAccount);

        Double amount = request.amount();
        Currency currency = senderAccount.getCurrency();
        AccountActivityType activityType = AccountActivityType.MONEY_TRANSFER;

        checkAccountsBeforeMoneyTransfer(senderAccount, receiverAccount, amount);
        checkDailyAccountActivityLimit(senderAccount, amount, activityType);

        transactionService.transferMoneyBetweenAccounts(request, senderAccountId, amount, receiverAccountId, senderAccount, receiverAccount);

        NotificationDto senderNotificationDto = new NotificationDto(senderAccount.getCustomer().getNationalId(), String.format("%s %s money transaction has been made from your account.", amount, currency));
        NotificationDto receiverNotificationDto = new NotificationDto(receiverAccount.getCustomer().getNationalId(), String.format("%s %s money transaction has been made to your account.", amount, currency));

        notificationService.createNotification(senderNotificationDto);
        notificationService.createNotification(receiverNotificationDto);

        return String.format(ResponseMessages.SUCCESS, activityType.getValue());
    }

    public String exchangeMoney(ExchangeRequest request) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Account sellerAccount = findById(request.sellerId());
        checkAccountStatus(sellerAccount);

        Account buyerAccount = findById(request.buyerId());
        checkAccountStatus(buyerAccount);

        AccountActivityType activityType = AccountActivityType.MONEY_EXCHANGE;
        Double requestedAmount = request.amount();

        AccountUtils.checkBalance(sellerAccount.getBalance(), requestedAmount);
        checkDailyAccountActivityLimit(sellerAccount, requestedAmount, activityType);

        transactionService.exchangeMoneyBetweenAccounts(request, sellerAccount, buyerAccount);

        return String.format(ResponseMessages.SUCCESS, activityType.getValue());
    }

    @Transactional
    public String updateBlockStatus(Integer id, boolean status) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Account account = findById(id);
        checkIsAccountClosed(account);

        account.setIsBlocked(status);
        accountRepository.save(account);

        String logMessage = status ? "Account {} is blocked"
                : "Blockage of account {} is removed";

        logMessage += " at {}";
        log.info(logMessage, id, LocalDateTime.now());

        AccountActivityType activityType = AccountActivityType.ACCOUNT_BLOCKING;
        transactionService.createAccountActivityForAccountStatusUpdate(account, activityType);

        String message = status ? activityType.getValue()
                : "Account blockage removal";

        return String.format(ResponseMessages.SUCCESS, message);
    }

    @Transactional
    public String closeAccount(Integer id) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Account account = findById(id);
        checkAccountStatus(account);

        double balance = account.getBalance();

        if (balance != 0) {
            throw new ResourceConflictException(String.format("In order to close account, balance of the account must be zero. Currently balance is %s. Please Withdraw or transfer the remaining money.", balance));
        }

        account.setClosedAt(LocalDateTime.now());
        accountRepository.save(account);

        AccountActivityType activityType = AccountActivityType.ACCOUNT_CLOSING;
        transactionService.createAccountActivityForAccountStatusUpdate(account, activityType);

        return String.format(ResponseMessages.SUCCESS, activityType.getValue());
    }

    public String getTotalActiveAccounts(AccountType type, Currency currency, City city) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        int count = accountRepository.getTotalAccountsByCityAndTypeAndCurrency(
                city.name(),
                type.name(),
                currency.name()
        );

        log.info("Total count: {}", count);

        return String.format("Total %s %s accounts is %d", type, currency, count);
    }

    public List<CustomerStatisticsResponse> getCustomersHaveMaximumBalance(AccountType type, Currency currency) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());
        return accountRepository.getCustomersHaveMaximumBalanceByTypeAndCurrency(type, currency);
    }

    public Account findById(Integer id) {
        String entity = Entity.ACCOUNT.getValue();
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessages.NOT_FOUND, entity)));

        log.info(LogMessages.RESOURCE_FOUND, entity);

        return account;
    }

    private void checkDailyAccountActivityLimit(Account account, Double amount, AccountActivityType activityType) {
        Set<AccountActivityDto> accountActivityDtos = new HashSet<>();

        for (Account currentAccount : account.getCustomer().getAccounts()) {
            AccountActivityFilteringOptions filteringOptions = constructAccountActivityFilteringOptions(currentAccount.getId(), activityType);
            accountActivityDtos.addAll(accountActivityService.getAccountActivitiesOfParticularAccounts(filteringOptions, account.getCurrency()));
        }

        double dailyActivityAmount = accountActivityDtos.stream()
                .map(AccountActivityDto::amount)
                .reduce(0D, Double::sum);

        log.info("Daily activity amount: {}", dailyActivityAmount);
        dailyActivityAmount += amount;
        log.info("Updated daily activity amount: {}", dailyActivityAmount);

        Double activityLimit = AccountActivityType.getActivityToLimits()
                .get(activityType);

        if (dailyActivityAmount > activityLimit) {
            throw new ResourceConflictException(String.format("Daily limit of %s is going to be exceeded. Daily limit is %s", activityType, activityLimit));
        }

        log.info("Daily limit of {} is not exceeded", activityType);
    }

    private static AccountActivityFilteringOptions constructAccountActivityFilteringOptions(Integer accountId, AccountActivityType activityType) {
        Integer[] accountIds = new Integer[2]; // first integer is sender id, second integer is receiver id

        switch (activityType) {
            case AccountActivityType.WITHDRAWAL, AccountActivityType.MONEY_TRANSFER,
                 AccountActivityType.MONEY_EXCHANGE -> accountIds[0] = accountId;
            case AccountActivityType.MONEY_DEPOSIT -> accountIds[1] = accountId;
            default -> throw new ResourceConflictException(ResponseMessages.IMPROPER_ACCOUNT_ACTIVITY);
        }

        return new AccountActivityFilteringOptions(
                List.of(activityType),
                accountIds[0],
                accountIds[1],
                null,
                LocalDate.now()
        );
    }

    private static void checkAccountsBeforeMoneyTransfer(Account senderAccount, Account receiverAccount, Double amount) {
        AccountUtils.checkCurrencies(senderAccount.getCurrency(), receiverAccount.getCurrency());
        AccountUtils.checkBalance(senderAccount.getBalance(), amount);
    }

    private static void checkAccountStatus(Account account) {
        checkIsAccountBlocked(account);
        checkIsAccountClosed(account);
    }

    private static void checkIsAccountBlocked(Account account) {
        int id = account.getId();

        if (account.getIsBlocked()) {
            log.error("Account {} is blocked", id);
            throw new ResourceConflictException(ResponseMessages.IMPROPER_ACCOUNT + ". It has been blocked");
        }

        log.info(LogMessages.NOT_IMPROVER_ACCOUNT_ACTIVITY, id, "blocked");
    }

    private static void checkIsAccountClosed(Account account) {
        int id = account.getId();
        LocalDateTime closedAt = account.getClosedAt();

        if (Optional.ofNullable(closedAt).isPresent()) {
            log.error("Account {} has already been closed at {}", id, closedAt);
            throw new ResourceConflictException(String.format(ResponseMessages.IMPROPER_ACCOUNT + ". It has already been closed at %s", closedAt));
        }

        log.info(LogMessages.NOT_IMPROVER_ACCOUNT_ACTIVITY, id, "closed");
    }
}
