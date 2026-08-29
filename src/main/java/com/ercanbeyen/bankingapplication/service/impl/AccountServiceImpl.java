package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.*;
import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.constant.query.HeaderField;
import com.ercanbeyen.bankingapplication.constant.query.SummaryField;
import com.ercanbeyen.bankingapplication.dto.*;
import com.ercanbeyen.bankingapplication.dto.request.AccountActivityFilteringRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyExchangeRequest;
import com.ercanbeyen.bankingapplication.dto.request.MoneyTransferRequest;
import com.ercanbeyen.bankingapplication.dto.response.AccountActivityPreview;
import com.ercanbeyen.bankingapplication.embeddable.Address;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.entity.Branch;
import com.ercanbeyen.bankingapplication.entity.Customer;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import com.ercanbeyen.bankingapplication.exception.InternalServerErrorException;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.AccountMapper;
import com.ercanbeyen.bankingapplication.dto.option.AccountActivityFilteringOption;
import com.ercanbeyen.bankingapplication.dto.option.AccountFilteringOption;
import com.ercanbeyen.bankingapplication.repository.AccountRepository;
import com.ercanbeyen.bankingapplication.dto.response.CustomerStatisticsResponse;
import com.ercanbeyen.bankingapplication.security.util.UserDetailsUtil;
import com.ercanbeyen.bankingapplication.service.*;
import com.ercanbeyen.bankingapplication.util.AccountUtil;
import com.ercanbeyen.bankingapplication.util.ExchangeUtil;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import com.ercanbeyen.bankingapplication.view.entity.ExchangeView;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {
    private static final Currency DEDUCTION_CURRENCY = Currency.getDeductionCurrency();
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final CustomerService customerService;
    private final TransactionService transactionService;
    private final NotificationService notificationService;
    private final AccountActivityService accountActivityService;
    private final BranchService branchService;
    private final AtmService atmService;
    private final DailyAccountActivityLimitService dailyAccountActivityLimitService;
    private final AgreementService agreementService;
    private final ExchangeService exchangeService;
    private final TimeZoneService timeZoneService;

    @Override
    public List<AccountDto> getEntities(AccountFilteringOption filteringOption) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Predicate<Account> accountPredicate = account -> {
            boolean typeFilter = (Optional.ofNullable(filteringOption.getType()).isEmpty() || filteringOption.getType() == account.getType());
            boolean timeFilter = (Optional.ofNullable(filteringOption.getCreatedAt()).isEmpty() || filteringOption.getCreatedAt().isEqual(account.getCreatedAt().toLocalDate()));
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

        String entity = Entity.ACCOUNT.getValue();

        if (AccountUtil.checkAccountTypeMatch.test(accountType, AccountType.DEPOSIT)) {
            log.info("Account type is {}. So, agreements and interest rate will be assigned", accountType.getValue());
            agreementService.approveAgreements(AgreementSubject.DEPOSIT_ACCOUNT, customer);

            log.info("{} is {}, so update interest rate and balance after next {}", entity, accountType.getValue(), Entity.TERM_DEPOSIT_INTEREST_RATE.getValue());
            account.setInterestRate(0D);
            account.setBalanceAfterNextInterestIncome(0D);
        }

        account.setCustomer(customer);
        account.setBranch(branch);

        Account savedAccount = accountRepository.save(account);
        log.info(LogMessage.RESOURCE_CREATE_SUCCESS, entity, savedAccount.getId());

        TransactionInformation transactionInformation = getTransactionPlaceForStatusUpdate(account.getBranch().getAddress());
        transactionService.createAccountActivityForAccountStatusUpdate(account, AccountActivityType.ACCOUNT_OPENING, transactionInformation);

        return accountMapper.entityToDto(savedAccount);
    }

    @Transactional
    @Override
    public AccountDto updateEntity(Integer id, AccountDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account account = findActiveAccountById(id);
        Branch branch = branchService.findByName(request.getBranchName());

        Address addressOfOldBranch = account.getBranch().getAddress();
        Address addressOfNewBranch = branch.getAddress();

        ZoneId zoneIdOfOldBranch = timeZoneService.getZoneId(addressOfOldBranch.getCountry(), addressOfOldBranch.getCity())
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, "Time Zone of old branch")));

        ZoneId zoneIdOfNewBranch = timeZoneService.getZoneId(addressOfNewBranch.getCountry(), addressOfNewBranch.getCity())
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, "Time Zone of new branch")));

        if (!zoneIdOfOldBranch.equals(zoneIdOfNewBranch)) {
            throw new ResourceConflictException("Time Zone of account cannot be updated!");
        }

        account.setBranch(branch);

        if (AccountUtil.checkAccountTypeMatch.test(account.getType(), AccountType.DEPOSIT) && !Objects.equals(account.getDepositMaturity(), request.getDepositMaturity())) {
            log.info(LogMessage.DEPOSIT_ACCOUNT_FIELDS_SHOULD_UPDATE);
            transactionService.updateDepositAccountFields(account, account.getBalance(), request.getDepositMaturity());
        }

        return accountMapper.entityToDto(accountRepository.save(account));
    }

    @Override
    public void deleteEntity(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        log.warn(LogMessage.UNUSABLE_METHOD);
    }

    @Override
    public void depositMoney(Integer id, Double amount, HttpServletRequest httpServletRequest) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account account = findActiveAccountById(id);
        AccountActivityType activityType = AccountActivityType.MONEY_DEPOSIT;
        AccountUtil.checkAccountActivityAndAccountTypeMatch(account.getType(), AccountType.CURRENT, activityType);

        checkDailyAccountActivityLimit(account, amount, activityType);

        TransactionInformation transactionInformation = getTransactionPlaceMoneyDepositAndWithdrawal(httpServletRequest);

        String entity = Entity.ACCOUNT.getValue().toLowerCase();
        String cashFlowExplanation = entity + " " + account.getId() + " deposited " + amount + " " + account.getCurrency();
        transactionService.applyAccountActivityForSingleAccount(activityType, amount, account, cashFlowExplanation, transactionInformation);

        String message = String.format("%s %s has been deposited into your %s %s", amount, account.getCurrency(), entity, account.getId());
        NotificationDto notificationDto = new NotificationDto(account.getCustomer().getNationalId(), String.format(message, amount, account.getCurrency(), entity, account.getId()));

        notificationService.sendNotification(notificationDto);
    }

    @Override
    public void withdrawMoney(Integer id, Double amount, HttpServletRequest httpServletRequest) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account account = findActiveAccountById(id);
        AccountActivityType activityType = AccountActivityType.WITHDRAWAL;
        AccountUtil.checkAccountActivityAndAccountTypeMatch(account.getType(), AccountType.CURRENT, activityType);

        checkDailyAccountActivityLimit(account, amount, activityType);

        TransactionInformation transactionInformation = getTransactionPlaceMoneyDepositAndWithdrawal(httpServletRequest);

        String entity = Entity.ACCOUNT.getValue();
        String cashFlowExplanation = entity + " " + account.getId() + " withdrew " + amount + " " + account.getCurrency();
        transactionService.applyAccountActivityForSingleAccount(activityType, amount, account, cashFlowExplanation, transactionInformation);

        String message = String.format("%s %s has been withdrawn from your %s %s", amount, account.getCurrency(), entity.toLowerCase(), account.getId());
        NotificationDto notificationDto = new NotificationDto(account.getCustomer().getNationalId(), String.format(message, amount, account.getCurrency(), entity, account.getId()));
        notificationService.sendNotification(notificationDto);
    }

    @Override
    public String payInterestIncome(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account account = findActiveAccountById(id);

        if (!AccountUtil.checkAccountForPeriodicMoneyAdd(account.getType(), LocalDateTime.now(ZoneId.systemDefault()), account.getDepositMaturity())) {
            return "Maturity date has not arrived yet";
        }

        Double amount = AccountUtil.calculateInterestIncome(account.getBalance(), account.getDepositMaturity(), account.getInterestRate());
        AccountActivityType activityType = AccountActivityType.INTEREST_INCOME;

        Address address = account.getBranch().getAddress();
        ZoneId zoneId = timeZoneService.getZoneId(address.getCountry(), address.getCity())
                .orElse(ZoneId.systemDefault());

        TransactionInformation transactionInformation = new TransactionInformation(
                ChannelType.getPlaceNameForSystemChannel(),
                ChannelType.SYSTEM,
                zoneId
        );

        String entity = Entity.ACCOUNT.getValue().toLowerCase();
        String cashFlowExplanation = amount + " " + account.getCurrency() + " is transferred to " + entity + " " + account.getId();
        transactionService.applyAccountActivityForSingleAccount(activityType, amount, account, cashFlowExplanation, transactionInformation);

        NotificationDto notificationDto = new NotificationDto(account.getCustomer().getNationalId(), String.format("Term of your %s is deposit %s has been renewed.", account.getCurrency(), entity));
        notificationService.sendNotification(notificationDto);

        String response = activityType.getValue() + " transfer";

        return String.format(ResponseMessage.SUCCESS, response);
    }

    @Override
    public void transferMoney(MoneyTransferRequest moneyTransferRequest, HttpServletRequest httpServletRequest) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account senderAccount = findActiveAccountById(moneyTransferRequest.senderAccountId());
        Account recipientAccount = findActiveAccountById(moneyTransferRequest.recipientAccountId());

        AccountActivityType accountActivityType = AccountActivityType.MONEY_TRANSFER;
        Double amount = moneyTransferRequest.amount();
        Currency currency = senderAccount.getCurrency();

        checkAccountsBeforeMoneyTransfer(senderAccount, recipientAccount);

        Account deducteeAccount = getDeducteeAccount(accountActivityType, moneyTransferRequest.deducteeAccountId(), List.of(senderAccount, recipientAccount));
        boolean areAccountsOwnedBySameCustomer = Objects.equals(senderAccount.getCustomer().getId(), recipientAccount.getCustomer().getId());

        if (!areAccountsOwnedBySameCustomer) {
            checkDailyAccountActivityLimit(senderAccount, amount, accountActivityType);
        }

        TransactionInformation transactionInformation = getTransactionPlaceForMoneyTransfer(httpServletRequest, senderAccount.getBranch().getAddress());

        transactionService.transferMoneyBetweenAccounts(moneyTransferRequest, amount, senderAccount, recipientAccount, deducteeAccount, transactionInformation);

        if (!areAccountsOwnedBySameCustomer) {
            String entity = Entity.ACCOUNT.getValue().toLowerCase();

            NotificationDto senderNotificationDto = new NotificationDto(senderAccount.getCustomer().getNationalId(), String.format("%s %s money transfer has been made from your %s.", amount, currency, entity));
            NotificationDto recipientNotificationDto = new NotificationDto(recipientAccount.getCustomer().getNationalId(), String.format("%s %s money transfer has been made to your %s.", amount, currency, entity));

            notificationService.sendNotification(senderNotificationDto);
            notificationService.sendNotification(recipientNotificationDto);
        }
    }

    @Override
    public void exchangeMoney(MoneyExchangeRequest moneyExchangeRequest, HttpServletRequest httpServletRequest) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account sellerAccount = findActiveAccountById(moneyExchangeRequest.sellerAccountId());
        Account buyerAccount = findActiveAccountById(moneyExchangeRequest.buyerAccountId());

        checkAccountsBeforeMoneyExchange(sellerAccount, buyerAccount);

        AccountActivityType accountActivityType = AccountActivityType.MONEY_EXCHANGE;
        checkDailyAccountActivityLimit(sellerAccount, moneyExchangeRequest.amount(), accountActivityType);

        Account deducteeAccount = getDeducteeAccount(accountActivityType, moneyExchangeRequest.deducteeAccountId(), List.of(sellerAccount, buyerAccount));
        TransactionInformation transactionInformation = getTransactionPlaceForMoneyExchange(httpServletRequest, sellerAccount, buyerAccount);
        transactionService.exchangeMoneyBetweenAccounts(moneyExchangeRequest, sellerAccount, buyerAccount, deducteeAccount, transactionInformation);
    }

    @Transactional
    @Override
    public String updateBlockStatus(Integer id, boolean status) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account account = findById(id);
        checkAccountClosed(account);

        String entity = Entity.ACCOUNT.getValue();

        if (account.isBlocked() == status) {
            log.warn("Same blocking status was applied to the {} {}", entity, id);
        } else {
            account.setBlocked(status);
            accountRepository.save(account);

            TransactionInformation transactionInformation = getTransactionPlaceForStatusUpdate(account.getBranch().getAddress());
            transactionService.createAccountActivityForAccountStatusUpdate(account, AccountActivityType.ACCOUNT_BLOCKING, transactionInformation);
        }

        return status ? entity + " is successfully blocked" : entity + " blockage is successfully removed";
    }

    @Transactional
    @Override
    public void closeAccount(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account account = findActiveAccountById(id);
        double balance = account.getBalance();

        if (balance != 0) {
            String entity = Entity.ACCOUNT.getValue().toLowerCase();
            throw new ResourceConflictException(String.format("In order to close %s, balance of the %s must be zero. Currently balance is %s. Please Withdraw or transfer the remaining money.", entity, balance, entity));
        }

        account.setClosedAt(Instant.now());
        accountRepository.save(account);

        TransactionInformation transactionInformation = getTransactionPlaceForStatusUpdate(account.getBranch().getAddress());

        transactionService.createAccountActivityForAccountStatusUpdate(account, AccountActivityType.ACCOUNT_CLOSING, transactionInformation);
    }

    @Override
    public Integer getTotalActiveAccounts(AccountType type, Currency currency, String city) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        return accountRepository.getTotalAccountsByCityAndTypeAndCurrency(city, type.name(), currency.name());
    }

    @Override
    public List<CustomerStatisticsResponse> getCustomersHaveMaximumBalance(AccountType type, Currency currency) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        return accountRepository.getCustomersHaveMaximumBalanceByTypeAndCurrency(type, currency);
    }

    @Override
    public List<AccountActivityPreview> getAccountActivityPreviews(Integer id, AccountActivityFilteringRequest request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account account = findById(id);

        UserDetails userDetails = UserDetailsUtil.getUserDetailsOfLoggedInUser();

        if (account.getClosedAt() != null && !UserDetailsUtil.getPermissions(userDetails).contains(EPermission.READ_DATA.toString())) {
            throw new AccessDeniedException(String.format(ResponseMessage.ACCESS_DENIED, "User cannot access to this account!"));
        }

        log.info("User can access to the account!");

        List<AccountActivityDto> accountActivityDtos = getAccountActivitiesOfAccount(account, request);
        List<AccountActivityPreview> accountActivityPreviews = new ArrayList<>();

        accountActivityDtos.forEach(accountActivityDto -> {
            Address address = account.getBranch().getAddress();
            ZoneId zoneId = timeZoneService.getZoneId(address.getCountry(), address.getCity())
                    .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, "Time Zone of branch")));

            BalanceActivity balanceActivity = switch (accountActivityDto.type()) {
                case ACCOUNT_OPENING, ACCOUNT_BLOCKING, ACCOUNT_CLOSING -> BalanceActivity.STABLE;
                case MONEY_DEPOSIT, INTEREST_INCOME -> BalanceActivity.INCREASE;
                case WITHDRAWAL, DEDUCTION -> BalanceActivity.DECREASE;
                default -> // MONEY_TRANSFER and MONEY_EXCHANGE cases
                        accountActivityDto.senderAccountId().equals(id) ? BalanceActivity.DECREASE : BalanceActivity.INCREASE;
            };

            AccountActivityPreview accountActivityPreview = new AccountActivityPreview(
                    accountActivityDto.id(),
                    accountActivityDto.type(),
                    balanceActivity,
                    accountActivityDto.amount(),
                    accountActivityDto.channelType(),
                    LocalDateTime.ofInstant(accountActivityDto.createdAt(), zoneId)
            );

            accountActivityPreviews.add(accountActivityPreview);
        });

        return accountActivityPreviews;
    }

    @Override
    public Account getDeducteeAccount(AccountActivityType accountActivityType, Integer extraDeducteeAccountId, List<Account> relatedAccounts) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        return switch (accountActivityType) {
            case AccountActivityType.MONEY_TRANSFER -> {
                Account senderAccount = relatedAccounts.getFirst();
                Account recipientAccount = relatedAccounts.getLast();

                if (Objects.equals(senderAccount.getCustomer().getId(), recipientAccount.getCustomer().getId())) { // Customer transfers money between his/her accounts
                    yield senderAccount;
                }

                yield getDeducteeAccountInMoneyTransfer(extraDeducteeAccountId, senderAccount);
            }
            case AccountActivityType.MONEY_EXCHANGE ->
                    getDeducteeAccountInMoneyExchange(extraDeducteeAccountId, relatedAccounts);
            default ->
                    throw new InternalServerErrorException("Unknown account activity type for getting deductee account");
        };
    }

    @Override
    public Account findDeducteeAccountById(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Account account = findActiveAccountById(id);
        String entity = Entity.ACCOUNT.getValue().toLowerCase();

        if (account.getCurrency() != DEDUCTION_CURRENCY) {
            throw new ResourceConflictException(String.format(ResponseMessage.INVALID_DEDUCTEE_ACCOUNT_CURRENCY, entity, DEDUCTION_CURRENCY));
        }

        AccountType accountType = AccountType.CURRENT;

        if (account.getType() != accountType) {
            throw new ResourceConflictException(String.format("Deductee %s type should be %s", entity, accountType));
        }


        log.info(LogMessage.RESOURCE_FOUND, "Deductee " + entity);

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
    public Account findById(Integer id) {
        String entity = Entity.ACCOUNT.getValue();
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity)));

        log.info(LogMessage.RESOURCE_FOUND, entity);

        return account;
    }

    @Override
    public void checkAccountsBeforeMoneyTransfer(Account senderAccount, Account recipientAccount) {
        AccountUtil.checkCurrenciesBeforeMoneyTransfer(senderAccount.getCurrency(), recipientAccount.getCurrency());

        if (senderAccount.getCustomer().getNationalId().equals(recipientAccount.getCustomer().getNationalId())) {
            String accountEntity = Entity.ACCOUNT.getValue().toLowerCase();
            String customerEntity = Entity.CUSTOMER.getValue().toLowerCase();

            log.warn("Same {} is transferring money between {}s", customerEntity, accountEntity);
            AccountType expectedAccountType = AccountType.CURRENT;

            if (!AccountUtil.checkAccountTypeMatch.test(senderAccount.getType(), expectedAccountType) && !AccountUtil.checkAccountTypeMatch.test(recipientAccount.getType(), expectedAccountType)) {
                log.error("There should be at least 1 {} {} in money transfer between {}s of the same {}", accountEntity, expectedAccountType.getValue().toLowerCase(), accountEntity, customerEntity);
                throw new ResourceConflictException(AccountType.DEPOSIT.getValue() + " " + accountEntity + "s cannot transfer money between themselves");
            }

            return;
        }

        AccountUtil.checkTypesOfAccountsBeforeMoneyTransferAndExchange(senderAccount.getType(), recipientAccount.getType(), AccountActivityType.MONEY_TRANSFER);
    }

    private List<AccountActivityDto> getAccountActivitiesOfAccount(Account account, AccountActivityFilteringRequest request) {
        Comparator<AccountActivityDto> accountActivityComparator = Comparator.comparing(AccountActivityDto::createdAt).reversed();
        BalanceActivity balanceActivity = request.balanceActivity();
        int id = account.getId();
        Set<AccountActivityDto> accountActivityDtos = new HashSet<>();

        if (Optional.ofNullable(balanceActivity).isPresent()) {
            accountActivityDtos = getFilteredAccountActivities(id, request, balanceActivity, account);
        } else {
            for (BalanceActivity currentBalanceActivity : BalanceActivity.values()) {
                accountActivityDtos.addAll(getFilteredAccountActivities(id, request, currentBalanceActivity, account));
            }
        }

        return accountActivityDtos.stream()
                .sorted(accountActivityComparator)
                .toList();
    }

    private Set<AccountActivityDto> getFilteredAccountActivities(Integer id, AccountActivityFilteringRequest request, BalanceActivity balanceActivity, Account account) {
        return switch (balanceActivity) {
            case DECREASE -> {
                AccountActivityFilteringOption filteringOption = new AccountActivityFilteringOption(
                        request.activityTypes(), id, null, request.minimumAmount(), request.fromDate(), request.toDate(), request.channelTypes());
                yield accountActivityService.getAccountActivitiesOfParticularAccounts(filteringOption, account.getCurrency());
            }
            case INCREASE -> {
                AccountActivityFilteringOption filteringOption = new AccountActivityFilteringOption(
                        request.activityTypes(), null, id, request.minimumAmount(), request.fromDate(), request.toDate(), request.channelTypes());
                yield accountActivityService.getAccountActivitiesOfParticularAccounts(filteringOption, account.getCurrency());
            }
            case STABLE -> {
                AccountActivityFilteringOption filteringOption = new AccountActivityFilteringOption(
                        request.activityTypes(), null, null, null, request.fromDate(), request.toDate(), request.channelTypes());
                yield accountActivityService.getAccountActivities(filteringOption)
                        .stream()
                        .filter(accountActivityDto -> {
                            Map<String, Object> summary = accountActivityDto.summary();
                            String accountActivity = (String) summary.get(SummaryField.ACCOUNT_ACTIVITY);

                            boolean accountIdExists = summary.containsKey(SummaryField.ACCOUNT_IDENTITY)
                                    && summary.get(SummaryField.ACCOUNT_IDENTITY) == id;

                            boolean accountActivityMatches = AccountActivityType.getAccountStatusUpdatingActivities()
                                    .stream()
                                    .map(AccountActivityType::getValue)
                                    .anyMatch(accountActivityType -> accountActivityType.equals(accountActivity));

                            return accountIdExists && accountActivityMatches;
                        })
                        .collect(Collectors.toSet());
            }
        };
    }

    private static void checkAccountsBeforeMoneyExchange(Account sellerAccount, Account buyerAccount) {
        ExchangeUtil.checkCurrenciesBeforeMoneyExchange(sellerAccount.getCurrency(), buyerAccount.getCurrency());

        if (!buyerAccount.getCustomer().getNationalId().equals(sellerAccount.getCustomer().getNationalId())) {
            throw new ResourceConflictException(String.format("Money %s between different customers is disallowed", Entity.EXCHANGE.getValue()));
        }

        AccountUtil.checkTypesOfAccountsBeforeMoneyTransferAndExchange(sellerAccount.getType(), buyerAccount.getType(), AccountActivityType.MONEY_EXCHANGE);
    }

    private Account getDeducteeAccountInMoneyExchange(Integer id, List<Account> accounts) {
        boolean accountWithDeductionCurrencyExists = accounts.stream()
                .map(Account::getCurrency)
                .anyMatch(currency -> currency == DEDUCTION_CURRENCY);

        Account deducteeAccount;
        String entity = Entity.ACCOUNT.getValue().toLowerCase();

        if (Optional.ofNullable(id).isPresent()) { // need an extra deductee account
            if (accountWithDeductionCurrencyExists) {
                throw new ResourceConflictException(String.format(ResponseMessage.IMPROPER_DEDUCTEE_ACCOUNT, entity, DEDUCTION_CURRENCY));
            }

            deducteeAccount = findDeducteeAccountById(id);
        } else { // no need an extra deductee account
            if (!accountWithDeductionCurrencyExists) {
                throw new ResourceConflictException(String.format(ResponseMessage.INVALID_DEDUCTEE_ACCOUNT_CURRENCY, entity, DEDUCTION_CURRENCY));
            }

            deducteeAccount = accounts.getFirst().getCurrency() == DEDUCTION_CURRENCY ? accounts.getFirst() : accounts.getLast();
        }

        return deducteeAccount;
    }

    private Account getDeducteeAccountInMoneyTransfer(Integer id, Account account) {
        Account deducteeAccount;
        String entity = Entity.ACCOUNT.getValue().toLowerCase();

        if (Optional.ofNullable(id).isPresent()) { // need an extra deductee account
            if (account.getCurrency() == DEDUCTION_CURRENCY) {
                throw new ResourceConflictException(String.format(ResponseMessage.IMPROPER_DEDUCTEE_ACCOUNT, entity, DEDUCTION_CURRENCY));
            }

            deducteeAccount = findDeducteeAccountById(id);
        } else { // no need an extra deductee account
            if (account.getCurrency() != DEDUCTION_CURRENCY) {
                throw new ResourceConflictException(String.format(ResponseMessage.INVALID_DEDUCTEE_ACCOUNT_CURRENCY, entity, DEDUCTION_CURRENCY));
            }

            log.info("Deductee {} is the related {} {}. So, no need the indicate a different {}", entity, entity, account.getId(), entity);
            deducteeAccount = account;
        }

        return deducteeAccount;
    }

    private void checkDailyAccountActivityLimit(Account account, Double amount, AccountActivityType activityType) {
        Set<AccountActivityDto> accountActivityDtos = new HashSet<>();

        for (Account currentAccount : account.getCustomer().getAccounts()) {
            AccountActivityFilteringOption filteringOption = constructAccountActivityFilteringOptionForDailyAccountActivityCheck(currentAccount.getId(), activityType);
            accountActivityDtos.addAll(accountActivityService.getAccountActivitiesOfParticularAccounts(filteringOption, account.getCurrency()));
        }

        double dailyAccountActivityAmount = accountActivityDtos.stream()
                .map(AccountActivityDto::amount)
                .reduce(0D, Double::sum);

        log.info("Daily account activity amount: {}", dailyAccountActivityAmount);
        dailyAccountActivityAmount += amount;
        log.info("Updated daily account activity amount: {}", dailyAccountActivityAmount);

        DailyAccountActivityLimitDto dailyAccountActivityLimit = dailyAccountActivityLimitService.getDailyAccountActivityLimit(activityType);
        Double lowerLimit = dailyAccountActivityLimit.lowerLimit();
        Double upperLimit = dailyAccountActivityLimit.upperLimit();
        log.info("Remaining daily account activity limit: {}", upperLimit - dailyAccountActivityAmount);

        if (dailyAccountActivityAmount < lowerLimit || dailyAccountActivityAmount > upperLimit) {
            throw new ResourceConflictException(String.format("Daily limits of %s are going to be exceeded. Daily limits are %s - %s", activityType.getValue(), lowerLimit, upperLimit));
        }

        log.info("Daily limits of {} are not exceeded", activityType.getValue());
    }

    private TransactionInformation getTransactionPlaceForStatusUpdate(Address address) {
        ZoneId zoneId = getZoneId(address);

        return new TransactionInformation(
                ChannelType.getPlaceNameForSystemChannel(),
                ChannelType.SYSTEM,
                zoneId
        );
    }

    private TransactionInformation getTransactionPlaceMoneyDepositAndWithdrawal(HttpServletRequest request) {
        ChannelType channelType = ChannelType.valueOf(request.getHeader(HeaderField.CHANNEL_TYPE));
        Integer channelId = request.getIntHeader(HeaderField.CHANNEL_ID);

        ChannelDto requestedChannel = switch (channelType) {
            case BRANCH -> branchService.getEntity(channelId);
            case ATM -> atmService.getEntity(channelId);
            default -> throw new BadRequestException(ResponseMessage.UNACCEPTABLE_CHANNEL);
        };

        ZoneId zoneId = getZoneId(requestedChannel.getAddress());

        return new TransactionInformation(
                requestedChannel.getName(),
                channelType,
                zoneId
        );
    }

    private TransactionInformation getTransactionPlaceForMoneyTransfer(HttpServletRequest httpServletRequest, Address senderAccountAddress) {
        ChannelType channelType = ChannelType.valueOf(httpServletRequest.getHeader(HeaderField.CHANNEL_TYPE));
        Integer channelId = httpServletRequest.getIntHeader(HeaderField.CHANNEL_ID);

        ChannelDto channelDto = switch (channelType) {
            case ATM -> atmService.getEntity(channelId);
            case BRANCH -> branchService.getEntity(channelId);
            case MOBILE_BANKING, INTERNET_BANKING -> {
                ChannelDto requestedChannel = new ChannelDto();
                requestedChannel.setName(ChannelType.getPlaceNameForSystemChannel());
                requestedChannel.setAddress(senderAccountAddress);
                yield requestedChannel;
            }
            default -> throw new BadRequestException(ResponseMessage.UNACCEPTABLE_CHANNEL);
        };

        Address address = channelDto.getAddress();
        ZoneId zoneId = getZoneId(address);

        return new TransactionInformation(
                channelDto.getName(),
                channelType,
                zoneId
        );
    }

    private TransactionInformation getTransactionPlaceForMoneyExchange(HttpServletRequest httpServletRequest, Account sellerAccount, Account buyerAccount) {
        ChannelType channelType = ChannelType.valueOf(httpServletRequest.getHeader(HeaderField.CHANNEL_TYPE));
        Integer channelId = httpServletRequest.getIntHeader(HeaderField.CHANNEL_ID);

        ChannelDto channelDto = switch (channelType) {
            case ATM -> atmService.getEntity(channelId);
            case BRANCH -> branchService.getEntity(channelId);
            case MOBILE_BANKING, INTERNET_BANKING -> {
                ChannelDto requestedChannel = new ChannelDto();
                requestedChannel.setName(ChannelType.getPlaceNameForSystemChannel());

                Currency sellerAccountCurrency = sellerAccount.getCurrency();
                Currency buyerAccountCurrency = sellerAccount.getCurrency();

                ExchangeView exchangeView = exchangeService.getExchangeView(sellerAccountCurrency, buyerAccountCurrency);
                Address addressOfTargetCurrency = exchangeView.getTargetCurrency() == sellerAccountCurrency
                        ? sellerAccount.getBranch().getAddress()
                        : buyerAccount.getBranch().getAddress();

                requestedChannel.setAddress(addressOfTargetCurrency);

                yield requestedChannel;
            }
            default -> throw new BadRequestException(ResponseMessage.UNACCEPTABLE_CHANNEL);
        };

        ZoneId zoneId = getZoneId(channelDto.getAddress());

        return new TransactionInformation(
                channelDto.getName(),
                channelType,
                zoneId
        );
    }

    private static AccountActivityFilteringOption constructAccountActivityFilteringOptionForDailyAccountActivityCheck(Integer accountId, AccountActivityType activityType) {
        Integer[] accountIds = new Integer[2]; // first integer is sender id, second integer is recipient id

        switch (activityType) {
            case AccountActivityType.WITHDRAWAL, AccountActivityType.MONEY_TRANSFER,
                 AccountActivityType.MONEY_EXCHANGE -> accountIds[0] = accountId;
            case AccountActivityType.MONEY_DEPOSIT -> accountIds[1] = accountId;
            default -> throw new ResourceConflictException(ResponseMessage.IMPROPER_ACCOUNT_ACTIVITY);
        }

        LocalDate today = LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault());

        return new AccountActivityFilteringOption(
                List.of(activityType),
                accountIds[0],
                accountIds[1],
                null,
                today,
                today,
                List.of(ChannelType.MOBILE_BANKING, ChannelType.INTERNET_BANKING, ChannelType.ATM, ChannelType.BRANCH)
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
        Instant closedAt = account.getClosedAt();

        if (closedAt != null) {
            log.error("{} {} has already been closed at {}", entity, id, closedAt);
            throw new ResourceConflictException(String.format(ResponseMessage.IMPROPER_ACCOUNT + ". It has already been closed at %s", closedAt));
        }

        log.info("{} {} has not been closed", entity, id);
    }

    private ZoneId getZoneId(Address address) {
        return timeZoneService.getZoneId(address.getCountry(), address.getCity()).orElseGet(ZoneId::systemDefault);
    }
}
