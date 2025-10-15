package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.*;
import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.constant.query.SummaryField;
import com.ercanbeyen.bankingapplication.dto.AccountActivityDto;
import com.ercanbeyen.bankingapplication.dto.AccountDto;
import com.ercanbeyen.bankingapplication.dto.NotificationDto;
import com.ercanbeyen.bankingapplication.dto.request.AccountActivityRequest;
import com.ercanbeyen.bankingapplication.dto.request.AccountActivityFilteringRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyExchangeRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyTransferRequest;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.entity.Branch;
import com.ercanbeyen.bankingapplication.entity.Customer;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.AccountMapper;
import com.ercanbeyen.bankingapplication.option.AccountActivityFilteringOption;
import com.ercanbeyen.bankingapplication.option.AccountFilteringOption;
import com.ercanbeyen.bankingapplication.repository.AccountRepository;
import com.ercanbeyen.bankingapplication.dto.response.CustomerStatisticsResponse;
import com.ercanbeyen.bankingapplication.service.*;
import com.ercanbeyen.bankingapplication.util.AccountUtil;
import com.ercanbeyen.bankingapplication.util.AgreementUtil;
import com.ercanbeyen.bankingapplication.util.ExchangeUtil;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {
    private static final Currency CHARGE_CURRENCY = Currency.getChargeCurrency();
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final CustomerServiceImpl customerService;
    private final TransactionService transactionService;
    private final NotificationService notificationService;
    private final AccountActivityService accountActivityService;
    private final BranchService branchService;
    private final DailyActivityLimitService dailyActivityLimitService;
    private final AgreementService agreementService;

    @Override
    public List<AccountDto> getEntities(AccountFilteringOption filteringOption) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Predicate<Account> accountPredicate = account -> {
            boolean typeFilter = (Optional.ofNullable(filteringOption.getType()).isEmpty() || filteringOption.getType() == account.getType());
            boolean timeFilter = (Optional.ofNullable(filteringOption.getCreatedAt()).isEmpty() || filteringOption.getCreatedAt().isEqual(filteringOption.getCreatedAt()));
            boolean blockedFilter = (Optional.ofNullable(filteringOption.getIsBlocked()).isEmpty() || filteringOption.getIsBlocked() == account.isBlocked());
            boolean closedAtFilter = (Optional.ofNullable(filteringOption.getIsClosed()).isEmpty() || filteringOption.getIsClosed() == (Optional.ofNullable(account.getClosedAt()).isPresent()));
            return typeFilter && timeFilter && blockedFilter && closedAtFilter;
        };

        return accountRepository.findAll()
                .stream()
                .filter(accountPredicate)
                .map(accountMapper::entityToDto)
                .toList();
    }

    @Override
    public AccountDto getEntity(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        return accountMapper.entityToDto(findById(id));
    }

    @Transactional
    @Override
    public AccountDto createEntity(AccountDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account account = accountMapper.dtoToEntity(request);
        Customer customer = customerService.findByNationalId(request.getCustomerNationalId());
        Branch branch = branchService.findByName(request.getBranchName());

        AccountType accountType = account.getType();
        String agreementSubject = AgreementUtil.generateSubject(accountType.getValue(), Entity.ACCOUNT);
        agreementService.addCustomerToAgreement(agreementSubject, customer);

        String entity = Entity.ACCOUNT.getValue();

        if (AccountUtil.checkAccountTypeMatch.test(accountType, AccountType.DEPOSIT)) {
            log.info("{} is {}, so update interest ratio and balance after next {}", entity, accountType.getValue(), Entity.FEE.getValue());
            account.setInterestRatio(0D);
            account.setBalanceAfterNextFee(0D);
        }

        account.setCustomer(customer);
        account.setBranch(branch);

        Account savedAccount = accountRepository.save(account);
        log.info(LogMessage.RESOURCE_CREATE_SUCCESS, entity, savedAccount.getId());

        createAccountActivityForAccountStatusUpdate(account, AccountActivityType.ACCOUNT_OPENING);

        return accountMapper.entityToDto(savedAccount);
    }

    @Transactional
    @Override
    public AccountDto updateEntity(Integer id, AccountDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account account = findActiveAccountById(id);
        Branch branch = branchService.findByName(request.getBranchName());
        account.setBranch(branch);

        if (AccountUtil.checkAccountTypeMatch.test(account.getType(), AccountType.DEPOSIT) && !Objects.equals(account.getDepositPeriod(), request.getDepositPeriod())) {
            log.info(LogMessage.DEPOSIT_ACCOUNT_FIELDS_SHOULD_UPDATE);
            transactionService.updateDepositAccountFields(account, account.getBalance(), request.getDepositPeriod());
        }

        return accountMapper.entityToDto(accountRepository.save(account));
    }

    @Override
    public void deleteEntity(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        log.warn(LogMessage.UNUSABLE_METHOD);
    }

    @Override
    public String depositMoney(Integer id, Double amount) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account account = findActiveAccountById(id);
        AccountActivityType activityType = AccountActivityType.MONEY_DEPOSIT;
        AccountUtil.checkAccountActivityAndAccountTypeMatch(account.getType(), AccountType.CURRENT, activityType);

        checkDailyAccountActivityLimit(account, amount, activityType);

        String entity = Entity.ACCOUNT.getValue().toLowerCase();
        String cashFlowExplanation = entity + " " + account.getId() + " deposited " + amount + " " + account.getCurrency();
        transactionService.applyAccountActivityForSingleAccount(activityType, amount, account, cashFlowExplanation);

        String message = String.format("%s %s has been deposited into your %s %s", amount, account.getCurrency(), entity, account.getId());
        NotificationDto notificationDto = new NotificationDto(account.getCustomer().getNationalId(), String.format(message, amount, account.getCurrency(), entity, account.getId()));

        notificationService.createNotification(notificationDto);

        return String.format(ResponseMessage.SUCCESS, activityType.getValue());
    }

    @Override
    public String withdrawMoney(Integer id, Double amount) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account account = findActiveAccountById(id);
        AccountActivityType activityType = AccountActivityType.WITHDRAWAL;
        AccountUtil.checkAccountActivityAndAccountTypeMatch(account.getType(), AccountType.CURRENT, activityType);

        checkDailyAccountActivityLimit(account, amount, activityType);

        String entity = Entity.ACCOUNT.getValue();
        String cashFlowExplanation = entity + " " + account.getId() + " withdrew " + amount + " " + account.getCurrency();
        transactionService.applyAccountActivityForSingleAccount(activityType, amount, account, cashFlowExplanation);

        String message = String.format("%s %s has been withdrawn from your %s %s", amount, account.getCurrency(), entity.toLowerCase(), account.getId());
        NotificationDto notificationDto = new NotificationDto(account.getCustomer().getNationalId(), String.format(message, amount, account.getCurrency(), entity, account.getId()));
        notificationService.createNotification(notificationDto);

        return String.format(ResponseMessage.SUCCESS, activityType.getValue());
    }

    @Override
    public String payInterest(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account account = findActiveAccountById(id);

        if (!AccountUtil.checkAccountForPeriodicMoneyAdd(account.getType(), account.getUpdatedAt(), account.getDepositPeriod())) {
            log.warn("Deposit period is not completed");
            return "Today is not the completion of deposit period";
        }

        Double amount = AccountUtil.calculateInterest(account.getBalance(), account.getDepositPeriod(), account.getInterestRatio());
        AccountActivityType activityType = AccountActivityType.FEE;

        String entity = Entity.ACCOUNT.getValue().toLowerCase();
        String cashFlowExplanation = amount + " " + account.getCurrency() + " is transferred to " + entity + " " + account.getId();
        transactionService.applyAccountActivityForSingleAccount(activityType, amount, account, cashFlowExplanation);

        NotificationDto notificationDto = new NotificationDto(account.getCustomer().getNationalId(), String.format("Term of your %s is deposit %s has been renewed.", account.getCurrency(), entity));
        notificationService.createNotification(notificationDto);

        String response = activityType.getValue() + " transfer";

        return String.format(ResponseMessage.SUCCESS, response);
    }

    @Override
    public String transferMoney(MoneyTransferRequest request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account senderAccount = findActiveAccountById(request.senderAccountId());
        Account receiverAccount = findActiveAccountById(request.receiverAccountId());

        AccountActivityType activityType = AccountActivityType.MONEY_TRANSFER;
        Double amount = request.amount();
        Currency currency = senderAccount.getCurrency();

        checkAccountsBeforeMoneyTransfer(senderAccount, receiverAccount);

        Account chargedAccount = getChargedAccount(request.chargedAccountId(), List.of(senderAccount));

        checkDailyAccountActivityLimit(senderAccount, amount, activityType);

        transactionService.transferMoneyBetweenAccounts(request, amount, senderAccount, receiverAccount, chargedAccount);

        if (!senderAccount.getCustomer().getNationalId().equals(receiverAccount.getCustomer().getNationalId())) {
            String entity = Entity.ACCOUNT.getValue().toLowerCase();

            NotificationDto senderNotificationDto = new NotificationDto(senderAccount.getCustomer().getNationalId(), String.format("%s %s money transfer has been made from your %s.", amount, currency, entity));
            NotificationDto receiverNotificationDto = new NotificationDto(receiverAccount.getCustomer().getNationalId(), String.format("%s %s money transfer has been made to your %s.", amount, currency, entity));

            notificationService.createNotification(senderNotificationDto);
            notificationService.createNotification(receiverNotificationDto);
        }

        return String.format(ResponseMessage.SUCCESS, activityType.getValue());
    }

    @Override
    public String exchangeMoney(MoneyExchangeRequest request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account sellerAccount = findActiveAccountById(request.sellerAccountId());
        Account buyerAccount = findActiveAccountById(request.buyerAccountId());

        checkAccountsBeforeMoneyExchange(sellerAccount, buyerAccount);

        AccountActivityType activityType = AccountActivityType.MONEY_EXCHANGE;

        checkDailyAccountActivityLimit(sellerAccount, request.amount(), activityType);

        Account chargedAccount = getChargedAccount(request.chargedAccountId(), List.of(sellerAccount, buyerAccount));
        transactionService.exchangeMoneyBetweenAccounts(request, sellerAccount, buyerAccount, chargedAccount);

        return String.format(ResponseMessage.SUCCESS, activityType.getValue());
    }

    @Transactional
    @Override
    public String updateBlockStatus(Integer id, boolean status) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account account = findById(id);
        checkAccountClosed(account);

        account.setBlocked(status);
        accountRepository.save(account);

        String entity = Entity.ACCOUNT.getValue();

        String logMessage = status ? "{} {} is blocked" : "Blockage of {} {} is removed";
        logMessage += " at {}";
        log.info(logMessage, entity, id, LocalDateTime.now());

        AccountActivityType activityType = AccountActivityType.ACCOUNT_BLOCKING;
        createAccountActivityForAccountStatusUpdate(account, activityType);

        String message = status ? activityType.getValue() : entity + " blockage removal";

        return String.format(ResponseMessage.SUCCESS, message);
    }

    @Transactional
    @Override
    public String closeAccount(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account account = findActiveAccountById(id);
        double balance = account.getBalance();

        if (balance != 0) {
            String entity = Entity.ACCOUNT.getValue().toLowerCase();
            throw new ResourceConflictException(String.format("In order to close %s, balance of the %s must be zero. Currently balance is %s. Please Withdraw or transfer the remaining money.", entity, balance, entity));
        }

        account.setClosedAt(LocalDateTime.now());
        accountRepository.save(account);

        AccountActivityType activityType = AccountActivityType.ACCOUNT_CLOSING;
        createAccountActivityForAccountStatusUpdate(account, activityType);

        return String.format(ResponseMessage.SUCCESS, activityType.getValue());
    }

    @Override
    public String getTotalActiveAccounts(AccountType type, Currency currency, City city) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        int count = accountRepository.getTotalAccountsByCityAndTypeAndCurrency(city.name(), type.name(), currency.name());
        log.info("Total count: {}", count);

        return String.format("Total %s %s accounts is %d", type.getValue(), currency, count);
    }

    @Override
    public List<CustomerStatisticsResponse> getCustomersHaveMaximumBalance(AccountType type, Currency currency) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        return accountRepository.getCustomersHaveMaximumBalanceByTypeAndCurrency(type, currency);
    }

    @Override
    public Account getChargedAccount(Integer extraChargedAccountId, List<Account> relatedAccounts) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account chargedAccount;

        if (relatedAccounts.size() == 1) { // Money transfer case
            Account relatedAccount = relatedAccounts.getFirst();
            chargedAccount = getChargedAccountInMoneyTransfer(extraChargedAccountId, relatedAccount);
        } else { // Money exchange case
            chargedAccount = getChargedAccountInMoneyExchange(extraChargedAccountId, relatedAccounts);
        }

        return chargedAccount;
    }

    @Override
    public Account findChargedAccountById(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account account = findActiveAccountById(id);
        String entity = Entity.ACCOUNT.getValue().toLowerCase();

        if (account.getCurrency() != CHARGE_CURRENCY) {
            throw new ResourceConflictException(String.format("Currency of charged %s should be %s", entity, CHARGE_CURRENCY));
        }

        AccountType accountType = AccountType.CURRENT;

        if (account.getType() != accountType) {
            throw new ResourceConflictException(String.format("Charged %s type should be %s", entity, accountType));
        }


        log.info(LogMessage.RESOURCE_FOUND, "Charged " + entity);

        return account;
    }

    @Override
    public Account findActiveAccountById(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account account = findById(id);
        checkAccountBlocked(account);
        checkAccountClosed(account);

        log.info(LogMessage.RESOURCE_FOUND, "Active " + Entity.ACCOUNT.getValue().toLowerCase());

        return account;
    }

    @Override
    public void checkAccountsBeforeMoneyTransfer(Account senderAccount, Account receiverAccount) {
        AccountUtil.checkCurrenciesBeforeMoneyTransfer(senderAccount.getCurrency(), receiverAccount.getCurrency());

        if (senderAccount.getCustomer().getNationalId().equals(receiverAccount.getCustomer().getNationalId())) {
            String accountEntity = Entity.ACCOUNT.getValue().toLowerCase();
            String customerEntity = Entity.CUSTOMER.getValue().toLowerCase();

            log.warn("Same {} is transferring money between {}s", customerEntity, accountEntity);
            AccountType expectedAccountType = AccountType.CURRENT;

            if (!AccountUtil.checkAccountTypeMatch.test(senderAccount.getType(), expectedAccountType) && !AccountUtil.checkAccountTypeMatch.test(receiverAccount.getType(), expectedAccountType)) {
                log.error("There should be at least 1 {} {} in money transfer between {}s of the same {}", accountEntity, expectedAccountType.getValue().toLowerCase(), accountEntity, customerEntity);
                throw new ResourceConflictException(AccountType.DEPOSIT.getValue() + " " + accountEntity + "s cannot transfer money between themselves");
            }

            return;
        }

        AccountUtil.checkTypesOfAccountsBeforeMoneyTransferAndExchange(senderAccount.getType(), receiverAccount.getType(), AccountActivityType.MONEY_TRANSFER);
    }

    @Override
    public List<AccountActivityDto> getAccountActivities(Integer id, AccountActivityFilteringRequest request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account account = findActiveAccountById(id);
        Comparator<AccountActivityDto> accountActivityComparator = Comparator.comparing(AccountActivityDto::createdAt).reversed();
        BalanceActivity balanceActivity = request.balanceActivity();

        if (balanceActivity == null) {
            AccountActivityFilteringOption accountActivityFilteringOption = new AccountActivityFilteringOption(
                    request.activityTypes(), null, null, request.minimumAmount(), request.createdAt());

            return accountActivityService.getAccountActivities(accountActivityFilteringOption)
                    .stream()
                    .sorted(accountActivityComparator)
                    .toList();
        }

        Set<AccountActivityDto> accountActivityDtos = switch (balanceActivity) {
            case DECREASE -> {
                AccountActivityFilteringOption accountActivityFilteringOption = new AccountActivityFilteringOption(
                        request.activityTypes(), id, null, request.minimumAmount(), request.createdAt());
                yield accountActivityService.getAccountActivitiesOfParticularAccounts(accountActivityFilteringOption, account.getCurrency());
            }
            case INCREASE -> {
                AccountActivityFilteringOption accountActivityFilteringOption = new AccountActivityFilteringOption(
                        request.activityTypes(), null, id, request.minimumAmount(), request.createdAt());
                yield accountActivityService.getAccountActivitiesOfParticularAccounts(accountActivityFilteringOption, account.getCurrency());
            }
            case STABLE -> {
                AccountActivityFilteringOption accountActivityFilteringOption = new AccountActivityFilteringOption(
                        request.activityTypes(), null, null, request.minimumAmount(), request.createdAt());
                yield accountActivityService.getAccountActivities(accountActivityFilteringOption)
                        .stream()
                        .filter(accountActivityDto -> {
                            Map<String, Object> summary = accountActivityDto.summary();
                            String accountActivity = (String) summary.get(SummaryField.ACCOUNT_ACTIVITY);

                            boolean accountIdExists = summary.containsKey(SummaryField.ACCOUNT_ID)
                                    && summary.get(SummaryField.ACCOUNT_ID) == id;

                            boolean accountActivityMatches = AccountActivityType.getAccountStatusUpdatingActivities()
                                    .stream()
                                    .map(AccountActivityType::getValue)
                                    .anyMatch(accountActivityType -> accountActivityType.equals(accountActivity));

                            return accountIdExists && accountActivityMatches;
                        })
                        .collect(Collectors.toSet());
            }
        };

        return accountActivityDtos.stream()
                .sorted(accountActivityComparator)
                .toList();
    }

    private Account findById(Integer id) {
        String entity = Entity.ACCOUNT.getValue();
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity)));

        log.info(LogMessage.RESOURCE_FOUND, entity);

        return account;
    }

    private static void checkAccountsBeforeMoneyExchange(Account sellerAccount, Account buyerAccount) {
        ExchangeUtil.checkCurrenciesBeforeMoneyExchange(sellerAccount.getCurrency(), buyerAccount.getCurrency());

        if (!buyerAccount.getCustomer().getNationalId().equals(sellerAccount.getCustomer().getNationalId())) {
            throw new ResourceConflictException(String.format("Money %s between different customers is disallowed", Entity.EXCHANGE.getValue()));
        }

        AccountUtil.checkTypesOfAccountsBeforeMoneyTransferAndExchange(sellerAccount.getType(), buyerAccount.getType(), AccountActivityType.MONEY_EXCHANGE);
    }

    private Account getChargedAccountInMoneyExchange(Integer id, List<Account> accounts) {
        boolean accountWithChargeCurrencyExists = accounts.stream()
                .map(Account::getCurrency)
                .anyMatch(currency -> currency == CHARGE_CURRENCY);

        Account chargedAccount;
        String entity = Entity.ACCOUNT.getValue().toLowerCase();

        if (Optional.ofNullable(id).isPresent()) { // need an extra charged account
            if (accountWithChargeCurrencyExists) {
                throw new ResourceConflictException(String.format("Charged %s should not be indicated for %s money transfers", entity, CHARGE_CURRENCY));
            }

            chargedAccount = findChargedAccountById(id);
        } else { // no need an extra charged account
            if (!accountWithChargeCurrencyExists) {
                throw new ResourceConflictException(String.format("Charged %s's currency should be %s", entity, CHARGE_CURRENCY));
            }

            chargedAccount = accounts.getFirst().getCurrency() == CHARGE_CURRENCY ? accounts.getFirst() : accounts.getLast();
        }

        return chargedAccount;
    }

    private Account getChargedAccountInMoneyTransfer(Integer id, Account account) {
        Account chargedAccount;
        String entity = Entity.ACCOUNT.getValue().toLowerCase();

        if (Optional.ofNullable(id).isPresent()) { // need an extra charged account
            if (account.getCurrency() == CHARGE_CURRENCY) {
                throw new ResourceConflictException(String.format("Charged %s should not be indicated for %s money transfers", entity, CHARGE_CURRENCY));
            }

            chargedAccount = findChargedAccountById(id);
        } else { // no need an extra charged account
            if (account.getCurrency() != CHARGE_CURRENCY) {
                throw new ResourceConflictException(String.format("Charged %s's currency should be %s", entity, CHARGE_CURRENCY));
            }

            log.info("Charged {} is the related {} {}. So, no need the indicate a different {}", entity, entity, account.getId(), entity);
            chargedAccount = account;
        }

        return chargedAccount;
    }

    private void checkDailyAccountActivityLimit(Account account, Double amount, AccountActivityType activityType) {
        Set<AccountActivityDto> accountActivityDtos = new HashSet<>();

        for (Account currentAccount : account.getCustomer().getAccounts()) {
            AccountActivityFilteringOption filteringOption = constructAccountActivityFilteringOption(currentAccount.getId(), activityType);
            accountActivityDtos.addAll(accountActivityService.getAccountActivitiesOfParticularAccounts(filteringOption, account.getCurrency()));
        }

        double dailyActivityAmount = accountActivityDtos.stream()
                .map(AccountActivityDto::amount)
                .reduce(0D, Double::sum);

        log.info("Daily activity amount: {}", dailyActivityAmount);
        dailyActivityAmount += amount;
        log.info("Updated daily activity amount: {}", dailyActivityAmount);

        Double activityLimit = dailyActivityLimitService.getDailyActivityLimit(activityType).amount();

        if (dailyActivityAmount > activityLimit) {
            throw new ResourceConflictException(String.format("Daily limit of %s is going to be exceeded. Daily limit is %s", activityType.getValue(), activityLimit));
        }

        log.info("Daily limit of {} is not exceeded", activityType.getValue());
    }

    private static AccountActivityFilteringOption constructAccountActivityFilteringOption(Integer accountId, AccountActivityType activityType) {
        Integer[] accountIds = new Integer[2]; // first integer is sender id, second integer is receiver id

        switch (activityType) {
            case AccountActivityType.WITHDRAWAL, AccountActivityType.MONEY_TRANSFER,
                 AccountActivityType.MONEY_EXCHANGE -> accountIds[0] = accountId;
            case AccountActivityType.MONEY_DEPOSIT -> accountIds[1] = accountId;
            default -> throw new ResourceConflictException(ResponseMessage.IMPROPER_ACCOUNT_ACTIVITY);
        }

        return new AccountActivityFilteringOption(List.of(activityType), accountIds[0], accountIds[1], null, LocalDate.now());
    }

    private static void checkAccountBlocked(Account account) {
        String entity = Entity.ACCOUNT.getValue();
        int id = account.getId();

        if (account.isBlocked()) {
            log.error("{} {} has been blocked", entity, id);
            throw new ResourceConflictException(ResponseMessage.IMPROPER_ACCOUNT + ". It has been blocked");
        }

        log.info("{} {} has not been blocked", entity, id);
    }

    private static void checkAccountClosed(Account account) {
        String entity = Entity.ACCOUNT.getValue();
        int id = account.getId();
        LocalDateTime closedAt = account.getClosedAt();

        if (Optional.ofNullable(closedAt).isPresent()) {
            log.error("{} {} has already been closed at {}", entity, id, closedAt);
            throw new ResourceConflictException(String.format(ResponseMessage.IMPROPER_ACCOUNT + ". It has already been closed at %s", closedAt));
        }

        log.info("{} {} has not been closed", entity, id);
    }

    private void createAccountActivityForAccountStatusUpdate(Account account, AccountActivityType activityType) {
        Map<String, Object> summary = new HashMap<>();
        summary.put(SummaryField.ACCOUNT_ACTIVITY, activityType.getValue());
        summary.put(SummaryField.ACCOUNT_ID, account.getId());
        summary.put(SummaryField.FULL_NAME, account.getCustomer().getFullName());
        summary.put(SummaryField.NATIONAL_IDENTITY, account.getCustomer().getNationalId());
        summary.put(SummaryField.ACCOUNT_TYPE, account.getCurrency() + " " + account.getType());
        summary.put(SummaryField.BRANCH, account.getBranch().getName());
        summary.put(SummaryField.TIME, LocalDateTime.now().toString());

        AccountActivityRequest request = new AccountActivityRequest(activityType, null, null, 0D, summary, null);
        accountActivityService.createAccountActivity(request);
    }
}
