package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.*;
import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.constant.query.SummaryFields;
import com.ercanbeyen.bankingapplication.dto.AccountActivityDto;
import com.ercanbeyen.bankingapplication.dto.AccountDto;
import com.ercanbeyen.bankingapplication.dto.NotificationDto;
import com.ercanbeyen.bankingapplication.dto.request.AccountActivityRequest;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService implements BaseService<AccountDto, AccountFilteringOption> {
    private static final Currency CHARGE_CURRENCY = Currency.getChargeCurrency();
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final CustomerService customerService;
    private final TransactionService transactionService;
    private final NotificationService notificationService;
    private final AccountActivityService accountActivityService;
    private final BranchService branchService;
    private final DailyActivityLimitService dailyActivityLimitService;
    private final FeeService feeService;
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

        if (accountType == AccountType.DEPOSIT) {
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

        if (account.getType() == AccountType.DEPOSIT && !Objects.equals(account.getDepositPeriod(), request.getDepositPeriod())) {
            double interestRatio = feeService.getInterestRatio(account.getCurrency(), request.getDepositPeriod(), account.getBalance());
            double balanceAfterNextFee = AccountUtil.calculateBalanceAfterNextFee(account.getBalance(), request.getDepositPeriod(), interestRatio);

            account.setDepositPeriod(request.getDepositPeriod());
            account.setInterestRatio(interestRatio);
            account.setBalanceAfterNextFee(balanceAfterNextFee);
        }

        return accountMapper.entityToDto(accountRepository.save(account));
    }

    @Override
    public void deleteEntity(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        if (!accountRepository.existsById(id)) {
            throw new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, Entity.ACCOUNT.getValue()));
        }

        log.info(LogMessage.RESOURCE_FOUND, Entity.ACCOUNT.getValue());

        accountRepository.deleteById(id);
        log.info(LogMessage.RESOURCE_DELETE_SUCCESS, Entity.ACCOUNT.getValue(), id);
    }

    public String depositMoney(Integer id, Double amount) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account account = findActiveAccountById(id);
        AccountActivityType activityType = AccountActivityType.MONEY_DEPOSIT;

        checkDailyAccountActivityLimit(account, amount, activityType);

        String cashFlowExplanation = Entity.ACCOUNT.getValue() + " " + account.getId() + " deposited " + amount + " " + account.getCurrency();
        transactionService.updateBalanceOfSingleAccount(activityType, amount, account, cashFlowExplanation);

        String message = String.format("%s %s has been deposited into your %s %s",
                amount, account.getCurrency(), Entity.ACCOUNT.getValue(), account.getId());

        NotificationDto notificationDto = new NotificationDto(
                account.getCustomer().getNationalId(),
                String.format(message, amount, account.getCurrency(), Entity.ACCOUNT.getValue(), account.getId())
        );

        notificationService.createNotification(notificationDto);

        return String.format(ResponseMessage.SUCCESS, activityType.getValue());
    }

    public String withdrawMoney(Integer id, Double amount) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account account = findActiveAccountById(id);
        AccountActivityType activityType = AccountActivityType.WITHDRAWAL;

        checkDailyAccountActivityLimit(account, amount, activityType);

        String cashFlowExplanation = Entity.ACCOUNT.getValue() + " " + account.getId() + " withdrew " + amount + " " + account.getCurrency();
        transactionService.updateBalanceOfSingleAccount(activityType, amount, account, cashFlowExplanation);

        String message = String.format("%s %s has been withdrawn from your %s %s",
                amount, account.getCurrency(), Entity.ACCOUNT.getValue(), account.getId());

        NotificationDto notificationDto = new NotificationDto(
                account.getCustomer().getNationalId(),
                String.format(message, amount, account.getCurrency(), Entity.ACCOUNT.getValue(), account.getId())
        );

        notificationService.createNotification(notificationDto);

        return String.format(ResponseMessage.SUCCESS, activityType.getValue());
    }

    public String payInterest(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account account = findActiveAccountById(id);

        if (!AccountUtil.checkAccountForPeriodicMoneyAdd(account.getType(), account.getUpdatedAt(), account.getDepositPeriod())) {
            log.warn("Deposit period is not completed");
            return "Today is not the completion of deposit period";
        }

        Double amount = AccountUtil.calculateInterest(account.getBalance(), account.getDepositPeriod(), account.getInterestRatio());
        AccountActivityType activityType = AccountActivityType.FEE;

        String cashFlowExplanation = amount + " " + account.getCurrency() + " is transferred to " + Entity.ACCOUNT.getValue() + " " + account.getId();
        transactionService.updateBalanceOfSingleAccount(activityType, amount, account, cashFlowExplanation);

        NotificationDto notificationDto = new NotificationDto(account.getCustomer().getNationalId(), String.format("Term of your %s is deposit account has been renewed.", account.getCurrency()));
        notificationService.createNotification(notificationDto);

        String response = activityType.getValue() + " transfer";

        return String.format(ResponseMessage.SUCCESS, response);
    }

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

        NotificationDto senderNotificationDto = new NotificationDto(senderAccount.getCustomer().getNationalId(), String.format("%s %s money transfer has been made from your account.", amount, currency));
        NotificationDto receiverNotificationDto = new NotificationDto(receiverAccount.getCustomer().getNationalId(), String.format("%s %s money transfer has been made to your account.", amount, currency));

        notificationService.createNotification(senderNotificationDto);
        notificationService.createNotification(receiverNotificationDto);

        return String.format(ResponseMessage.SUCCESS, activityType.getValue());
    }

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
    public String updateBlockStatus(Integer id, boolean status) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account account = findById(id);
        checkAccountClosed(account);

        account.setBlocked(status);
        accountRepository.save(account);

        String logMessage = status ? "{} {} is blocked"
                : "Blockage of {} {} is removed";

        logMessage += " at {}";
        log.info(logMessage, Entity.ACCOUNT.getValue(), id, LocalDateTime.now());

        AccountActivityType activityType = AccountActivityType.ACCOUNT_BLOCKING;
        createAccountActivityForAccountStatusUpdate(account, activityType);

        String message = status ? activityType.getValue()
                : "Account blockage removal";

        return String.format(ResponseMessage.SUCCESS, message);
    }

    @Transactional
    public String closeAccount(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account account = findActiveAccountById(id);
        double balance = account.getBalance();

        if (balance != 0) {
            throw new ResourceConflictException(String.format("In order to close account, balance of the account must be zero. Currently balance is %s. Please Withdraw or transfer the remaining money.", balance));
        }

        account.setClosedAt(LocalDateTime.now());
        accountRepository.save(account);

        AccountActivityType activityType = AccountActivityType.ACCOUNT_CLOSING;
        createAccountActivityForAccountStatusUpdate(account, activityType);

        return String.format(ResponseMessage.SUCCESS, activityType.getValue());
    }

    public String getTotalActiveAccounts(AccountType type, Currency currency, City city) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        int count = accountRepository.getTotalAccountsByCityAndTypeAndCurrency(
                city.name(),
                type.name(),
                currency.name()
        );

        log.info("Total count: {}", count);

        return String.format("Total %s %s accounts is %d", type, currency, count);
    }

    public List<CustomerStatisticsResponse> getCustomersHaveMaximumBalance(AccountType type, Currency currency) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        return accountRepository.getCustomersHaveMaximumBalanceByTypeAndCurrency(type, currency);
    }

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

    public Account findChargedAccountById(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account account = findActiveAccountById(id);

        if (account.getCurrency() != CHARGE_CURRENCY) {
            throw new ResourceConflictException(String.format("Currency of charged account should be %s", CHARGE_CURRENCY));
        }

        AccountType accountType = AccountType.CURRENT;

        if (account.getType() != accountType) {
            throw new ResourceConflictException(String.format("Charged account type should be %s", accountType));
        }

        log.info(LogMessage.RESOURCE_FOUND, "Charged " + Entity.ACCOUNT.getValue());

        return account;
    }

    public Account findActiveAccountById(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account account = findById(id);
        checkAccountBlocked(account);
        checkAccountClosed(account);

        log.info(LogMessage.RESOURCE_FOUND, "Active " + Entity.ACCOUNT.getValue());

        return account;
    }

    private Account findById(Integer id) {
        String entity = Entity.ACCOUNT.getValue();
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity)));

        log.info(LogMessage.RESOURCE_FOUND, entity);

        return account;
    }

    private static void checkAccountsBeforeMoneyTransfer(Account senderAccount, Account receiverAccount) {
        AccountUtil.checkCurrenciesBeforeMoneyTransfer(senderAccount.getCurrency(), receiverAccount.getCurrency());

        if (senderAccount.getCustomer().getNationalId().equals(receiverAccount.getCustomer().getNationalId())) {
            log.warn("Same customer is transferring money between accounts");
            return;
        }

        AccountUtil.checkTypesOfAccountsBeforeMoneyTransferAndExchange(senderAccount.getType(), receiverAccount.getType());
    }

    private static void checkAccountsBeforeMoneyExchange(Account sellerAccount, Account buyerAccount) {
        ExchangeUtil.checkCurrenciesBeforeMoneyExchange(sellerAccount.getCurrency(), buyerAccount.getCurrency());

        if (!buyerAccount.getCustomer().getNationalId().equals(sellerAccount.getCustomer().getNationalId())) {
            throw new ResourceConflictException(String.format("Money %s between different customers is disallowed", Entity.EXCHANGE.getValue()));
        }

        AccountUtil.checkTypesOfAccountsBeforeMoneyTransferAndExchange(sellerAccount.getType(), buyerAccount.getType());
    }

    private Account getChargedAccountInMoneyExchange(Integer id, List<Account> accounts) {
        boolean accountWithChargeCurrencyExists = accounts.stream()
                .map(Account::getCurrency)
                .anyMatch(currency -> currency == CHARGE_CURRENCY);

        Account chargedAccount;

        if (Optional.ofNullable(id).isPresent()) { // need an extra charged account
            if (accountWithChargeCurrencyExists) {
                throw new ResourceConflictException(String.format("Charged account should not be indicated for %s money transfers", CHARGE_CURRENCY));
            }

            chargedAccount = findChargedAccountById(id);
        } else { // no need an extra charged account
            if (!accountWithChargeCurrencyExists) {
                throw new ResourceConflictException(String.format("Charged account's currency should be %s", CHARGE_CURRENCY));
            }

            chargedAccount = accounts.getFirst().getCurrency() == CHARGE_CURRENCY
                    ? accounts.getFirst()
                    : accounts.getLast();
        }

        return chargedAccount;
    }

    private Account getChargedAccountInMoneyTransfer(Integer id, Account account) {
        Account chargedAccount;

        if (Optional.ofNullable(id).isPresent()) { // need an extra charged account
            if (account.getCurrency() == CHARGE_CURRENCY) {
                throw new ResourceConflictException(String.format("Charged account should not be indicated for %s money transfers", CHARGE_CURRENCY));
            }

            chargedAccount = findChargedAccountById(id);
        } else { // no need an extra charged account
            if (account.getCurrency() != CHARGE_CURRENCY) {
                throw new ResourceConflictException(String.format("Charged account's currency should be %s", CHARGE_CURRENCY));
            }

            log.info("Charged account is the related account {}. So, no need the indicate a different account", account.getId());
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
            throw new ResourceConflictException(String.format("Daily limit of %s is going to be exceeded. Daily limit is %s", activityType, activityLimit));
        }

        log.info("Daily limit of {} is not exceeded", activityType);
    }

    private static AccountActivityFilteringOption constructAccountActivityFilteringOption(Integer accountId, AccountActivityType activityType) {
        Integer[] accountIds = new Integer[2]; // first integer is sender id, second integer is receiver id

        switch (activityType) {
            case AccountActivityType.WITHDRAWAL, AccountActivityType.MONEY_TRANSFER,
                 AccountActivityType.MONEY_EXCHANGE -> accountIds[0] = accountId;
            case AccountActivityType.MONEY_DEPOSIT -> accountIds[1] = accountId;
            default -> throw new ResourceConflictException(ResponseMessage.IMPROPER_ACCOUNT_ACTIVITY);
        }

        return new AccountActivityFilteringOption(
                List.of(activityType),
                accountIds[0],
                accountIds[1],
                null,
                LocalDate.now()
        );
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
        summary.put(SummaryFields.ACCOUNT_ACTIVITY, activityType.getValue());
        summary.put(SummaryFields.FULL_NAME, account.getCustomer().getFullName());
        summary.put(SummaryFields.NATIONAL_IDENTITY, account.getCustomer().getNationalId());
        summary.put(SummaryFields.ACCOUNT_TYPE, account.getCurrency() + " " + account.getType());
        summary.put(SummaryFields.BRANCH, account.getBranch().getName());
        summary.put(SummaryFields.TIME, LocalDateTime.now().toString());

        AccountActivityRequest request = new AccountActivityRequest(
                activityType,
                null,
                null,
                0D,
                summary,
                null
        );

        accountActivityService.createAccountActivity(request);
    }
}
