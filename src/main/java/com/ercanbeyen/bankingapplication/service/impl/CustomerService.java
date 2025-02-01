package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.*;
import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.*;
import com.ercanbeyen.bankingapplication.dto.response.CustomerStatusResponse;
import com.ercanbeyen.bankingapplication.embeddable.CashFlow;
import com.ercanbeyen.bankingapplication.embeddable.ExpectedTransaction;
import com.ercanbeyen.bankingapplication.entity.*;
import com.ercanbeyen.bankingapplication.exception.InternalServerErrorException;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.*;
import com.ercanbeyen.bankingapplication.option.AccountFilteringOption;
import com.ercanbeyen.bankingapplication.option.CustomerFilteringOption;
import com.ercanbeyen.bankingapplication.option.AccountActivityFilteringOption;
import com.ercanbeyen.bankingapplication.repository.CustomerRepository;
import com.ercanbeyen.bankingapplication.service.*;
import com.ercanbeyen.bankingapplication.util.AccountUtil;
import com.ercanbeyen.bankingapplication.util.CashFlowCalendarUtil;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService implements BaseService<CustomerDto, CustomerFilteringOption> {
    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final AccountMapper accountMapper;
    private final TransferOrderMapper transferOrderMapper;
    private final NotificationMapper notificationMapper;
    private final CashFlowCalendarMapper cashFlowCalendarMapper;
    private final FileService fileService;
    private final AccountActivityService accountActivityService;
    private final ExchangeService exchangeService;
    private final CashFlowCalendarService cashFlowCalendarService;
    private final ContractService contractService;

    @Override
    public List<CustomerDto> getEntities(CustomerFilteringOption filteringOption) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Predicate<Customer> customerPredicate = customer -> {
            LocalDate birthDateOption = filteringOption.getBirthDate();
            LocalDate customerBirthday = customer.getBirthDate();

            Boolean birthDayFilter = (birthDateOption == null
                    || birthDateOption.getMonth() == customerBirthday.getMonth() && birthDateOption.getDayOfMonth() == customerBirthday.getDayOfMonth());
            Boolean createdAtFilter = (filteringOption.getCreatedAt() == null || filteringOption.getCreatedAt().isEqual(filteringOption.getCreatedAt()));

            return (birthDayFilter && createdAtFilter);
        };

        return customerRepository.findAll()
                .stream()
                .filter(customerPredicate)
                .map(customerMapper::entityToDto)
                .toList();
    }

    @Override
    public CustomerDto getEntity(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        Customer customer = findById(id);
        return customerMapper.entityToDto(customer);
    }

    @Transactional
    @Override
    public CustomerDto createEntity(CustomerDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        checkUniqueness(null, request);

        Customer customer = customerMapper.dtoToEntity(request);
        CashFlowCalendar cashFlowCalendar = cashFlowCalendarService.createCashFlowCalendar();
        customer.setCashFlowCalendar(cashFlowCalendar);

        Customer savedCustomer = customerRepository.save(customer);
        log.info(LogMessage.RESOURCE_CREATE_SUCCESS, Entity.CUSTOMER.getValue(), savedCustomer.getId());

        contractService.addCustomerToContract("CUSTOMER_REGISTRATION_CONTRACT", customer);

        return customerMapper.entityToDto(savedCustomer);
    }

    @Transactional
    @Override
    public CustomerDto updateEntity(Integer id, CustomerDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Customer customer = findById(id);
        checkUniqueness(customer, request);

        customer.setName(request.getName());
        customer.setSurname(request.getSurname());
        customer.setPhoneNumber(request.getPhoneNumber());
        customer.setEmail(request.getEmail());
        customer.setGender(request.getGender());
        customer.setBirthDate(request.getBirthDate());
        customer.setAddresses(request.getAddresses());

        return customerMapper.entityToDto(customerRepository.save(customer));
    }

    @Override
    public void deleteEntity(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        Customer customer = findById(id);
        customerRepository.delete(customer);
    }

    public String uploadProfilePhoto(Integer id, MultipartFile file) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Customer customer = findById(id);
        CompletableFuture<File> photo = fileService.storeFile(file);
        customer.setProfilePhoto(photo.join()); // Profile photo upload
        customerRepository.save(customer);

        return ResponseMessage.FILE_UPLOAD_SUCCESS;
    }

    public File downloadProfilePhoto(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        Customer customer = findById(id);
        return customer.getProfilePhoto()
                .orElseThrow(() -> new ResourceNotFoundException(ResponseMessage.NOT_FOUND));
    }

    public String deleteProfilePhoto(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Customer customer = findById(id);
        customer.setProfilePhoto(null); // Profile photo deletion
        customerRepository.save(customer);

        return ResponseMessage.FILE_DELETE_SUCCESS;
    }

    public CustomerStatusResponse calculateStatus(String nationalId, Currency toCurrency) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        List<Account> accounts = findByNationalId(nationalId).getAccounts();
        double earning = 0;
        double spending = 0;

        for (Account account : accounts) {
            earning += calculateTotalAmount(account, BalanceActivity.INCREASE, toCurrency);
            spending += calculateTotalAmount(account, BalanceActivity.DECREASE, toCurrency);
            log.info("Earning and Spending for Account {}: {} & {}", earning, spending, account.getId());
        }

        Double netStatus = accounts.stream()
                .map(account -> exchangeService.convertMoneyBetweenCurrencies(
                        account.getCurrency(),
                        toCurrency,
                        account.getBalance()))
                .reduce(0D, Double::sum);

        return new CustomerStatusResponse(earning, spending, netStatus);
    }

    public List<AccountDto> getAccounts(Integer id, AccountFilteringOption filteringOption) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Customer customer = findById(id);
        Predicate<Account> accountPredicate = account -> (account.getCustomer().getNationalId().equals(customer.getNationalId()))
                && (filteringOption.getType() == null || filteringOption.getType() == account.getType())
                && (filteringOption.getCreatedAt() == null || filteringOption.getCreatedAt().getYear() <= account.getCreatedAt().getYear());

        Comparator<Account> accountComparator = Comparator.comparing(Account::getCreatedAt)
                .reversed();

        List<Account> accounts = customer.getAccounts()
                .stream()
                .filter(accountPredicate)
                .sorted(accountComparator)
                .toList();

        List<AccountDto> accountDtos = new ArrayList<>();
        accounts.forEach(account -> accountDtos.add(accountMapper.entityToDto(account)));

        return accountDtos;
    }

    public List<AccountActivityDto> getAccountActivities(Integer id, AccountActivityFilteringOption filteringOption) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Customer customer = findById(id);
        List<AccountActivityDto> accountActivityDtos = new ArrayList<>();

        List<Integer> accountIds = customer.getAccounts()
                .stream()
                .map(Account::getId)
                .toList();

        /* Get all account activities of each account */
        accountIds.forEach(accountId -> {
            getAccountActivities(accountId, BalanceActivity.DECREASE, filteringOption, accountActivityDtos);
            getAccountActivities(accountId, BalanceActivity.INCREASE, filteringOption, accountActivityDtos);
        });

        Comparator<AccountActivityDto> transactionDtoComparator = Comparator.comparing(AccountActivityDto::createdAt).reversed();

        return accountActivityDtos.stream()
                .sorted(transactionDtoComparator)
                .toList();
    }

    public List<NotificationDto> getNotifications(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Customer customer = findById(id);
        List<NotificationDto> notificationDtos = new ArrayList<>();

        customer.getNotifications()
                .forEach(notification -> notificationDtos.add(notificationMapper.entityToDto(notification)));

        return notificationDtos;
    }

    public List<TransferOrderDto> getTransferOrders(Integer customerId, LocalDate fromDate, LocalDate toDate, Currency currency, PaymentType paymentType) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Customer customer = findById(customerId);
        List<TransferOrderDto> transferOrderDtos = new ArrayList<>();

        Predicate<TransferOrder> transferOrderPredicate = transferOrder -> {
            LocalDate transferDate = transferOrder.getTransferDate();
            boolean checkTransferDate = transferDate.isAfter(fromDate.minusDays(1))
                    && transferDate.isBefore(toDate.plusDays(1));
            boolean checkCurrency = (Optional.ofNullable(currency).isEmpty()
                    || currency == transferOrder.getSenderAccount().getCurrency());
            boolean checkPaymentType = (Optional.ofNullable(paymentType).isEmpty()
                    || paymentType == transferOrder.getRegularTransfer().getPaymentType());

            return checkTransferDate && checkCurrency && checkPaymentType;
        };

        for (Account account : customer.getAccounts()) {
            List<TransferOrderDto> transferOrderDtosOfAccount = account.getTransferOrders()
                    .stream()
                    .filter(transferOrderPredicate)
                    .map(transferOrderMapper::entityToDto)
                    .toList();

            transferOrderDtos.addAll(transferOrderDtosOfAccount);
        }

        return transferOrderDtos;
    }

    public CashFlowCalendarDto getCashFlowCalendar(Integer id, Integer year, Integer month) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Customer customer = findById(id);
        CashFlowCalendar cashFlowCalendar = customer.getCashFlowCalendar();
        List<CashFlow> cashFlows = new ArrayList<>();
        LocalDate today = LocalDate.now();

        if (CashFlowCalendarUtil.isDateFuture(today, year, month)) {
            log.info("Past cash flows are requested");
            getPastCashFlows(year, month, cashFlowCalendar, cashFlows);
        } else if (CashFlowCalendarUtil.isDatePast(today, year, month)) {
            log.info("Future cash flows are requested");
            getFutureCashFlows(year, month, customer, cashFlows);
        } else if (CashFlowCalendarUtil.isDateThisMonth(today, year, month)) {
            log.info("This month's cash flows are requested");
            getPastCashFlows(year, month, cashFlowCalendar, cashFlows);
            getFutureCashFlows(year, month, customer, cashFlows);
        } else {
            log.error("Unhandled time case. Year {} & Month: {}", year, month);
            throw new InternalServerErrorException("Error occurred while processing the cash flow calendar");
        }

        cashFlowCalendar.setCashFlows(cashFlows);

        return cashFlowCalendarMapper.entityToDto(cashFlowCalendar);
    }

    public List<ExpectedTransaction> getExpectedTransactions(Integer id, Integer month) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Customer customer = findById(id);
        List<ExpectedTransaction> expectedTransactions = new ArrayList<>();
        LocalDate finalDate = LocalDate.now().plusMonths(month);

        for (Account account : customer.getAccounts()) {
            AccountType accountType = account.getType();
            String entity = Entity.ACCOUNT.getValue();

            if (accountType == AccountType.DEPOSIT) {
                log.info("Only expected fees are going to be processed for {} {}", accountType.getValue(), entity);
                LocalDate nextPaymentDate = account.getUpdatedAt().plusMonths(account.getDepositPeriod()).toLocalDate();

                while (!nextPaymentDate.isAfter(finalDate)) {
                    ExpectedTransaction expectedTransaction = new ExpectedTransaction(AccountActivityType.FEE, account.getInterestRatio(), nextPaymentDate);
                    expectedTransactions.add(expectedTransaction);
                    nextPaymentDate = nextPaymentDate.plusMonths(account.getDepositPeriod());
                }

                continue;
            }

            for (TransferOrder transferOrder : account.getTransferOrders()) {
                log.info("Only expected money transfers are going to be processed for {} {}", accountType, entity);
                LocalDate nextPaymentDate = transferOrder.getTransferDate();

                while (!nextPaymentDate.isAfter(finalDate)) {
                    ExpectedTransaction expectedTransaction = new ExpectedTransaction(AccountActivityType.MONEY_TRANSFER, transferOrder.getRegularTransfer().getAmount(), nextPaymentDate);
                    expectedTransactions.add(expectedTransaction);

                    PaymentPeriod paymentPeriod = transferOrder.getRegularTransfer().getPaymentPeriod();
                    nextPaymentDate = switch (paymentPeriod) {
                        case ONE_TIME -> nextPaymentDate;
                        case DAILY -> nextPaymentDate.plusDays(1);
                        case WEEKLY -> nextPaymentDate.plusWeeks(1);
                        case MONTHLY -> nextPaymentDate.plusMonths(1);
                    };

                    if (paymentPeriod == PaymentPeriod.ONE_TIME) {  // One Time Transfer Order Case
                        log.info("One Time transfer order. So the expected transaction has already been added");
                        break;
                    }
                }
            }
        }

        return expectedTransactions.stream()
                .sorted(Comparator.comparing(ExpectedTransaction::date))
                .toList();
    }

    /**
     * @param nationalId is national identity which is unique for each customer
     * @return customer corresponds to the given nationalId
     */
    public Customer findByNationalId(String nationalId) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        String entity = Entity.CUSTOMER.getValue();
        Customer customer = customerRepository.findByNationalId(nationalId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity)));

        log.info(LogMessage.RESOURCE_FOUND, entity);

        return customer;
    }

    /***
     *
     * @param nationalId is national identity which is unique for each customer
     * @return status for customer existence corresponds to nationalId
     */
    public boolean existsByNationalId(String nationalId) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        return customerRepository.existsByNationalId(nationalId);
    }

    private Customer findById(Integer id) {
        String entity = Entity.CUSTOMER.getValue();
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity)));

        log.info(LogMessage.RESOURCE_FOUND, entity);

        return customer;
    }

    private static void getFutureCashFlows(Integer year, Integer month, Customer customer, List<CashFlow> cashFlows) {
        for (Account account : customer.getAccounts()) {
            AccountType accountType = account.getType();
            String entity = Entity.ACCOUNT.getValue();

            if (accountType == AccountType.DEPOSIT) {
                log.info(LogMessage.ONLY_ENTITIES_ARE_GOING_TO_BE_PROCESSED, Entity.FEE.getValue(), accountType.getValue(), entity);
                addFutureCashFlowsForFees(cashFlows, account, year, month);
            } else { // Account type is current
                log.info(LogMessage.ONLY_ENTITIES_ARE_GOING_TO_BE_PROCESSED, Entity.TRANSFER_ORDER.getValue(), accountType.getValue(), entity);
                addFutureCashFlowsForTransferOrders(cashFlows, account, year, month);
            }
        }

        cashFlows.sort(Comparator.comparing(CashFlow::getDate));
    }

    private static void addFutureCashFlowsForTransferOrders(List<CashFlow> cashFlows, Account account, Integer year, Integer month) {
        for (TransferOrder transferOrder : account.getTransferOrders()) {
            LocalDate paymentDate = transferOrder.getTransferDate();
            LocalDate counterDate = LocalDate.now();
            PaymentPeriod paymentPeriod = transferOrder.getRegularTransfer().getPaymentPeriod();
            AccountActivityType activityType = AccountActivityType.MONEY_TRANSFER;
            Double amount = transferOrder.getRegularTransfer().getAmount();
            String entity = Entity.ACCOUNT.getValue();

            if (paymentPeriod == PaymentPeriod.ONE_TIME && doesDateMatchesWithYearAndMonth(paymentDate, year, month)) { // One Time Transfer Order Case
                String explanation = entity + " " + account.getId() + " will send " + amount + " " + account.getCurrency();
                addCashFlow(cashFlows, paymentDate, activityType, year, month, explanation);
                log.info("One Time Transfer Order, so related cash flow was 1 and it has already been added");
                continue;
            }

            while (!CashFlowCalendarUtil.isDateFuture(counterDate, year, month)) {
                if (doesDateMatchesWithYearAndMonth(paymentDate, counterDate.getYear(), counterDate.getMonthValue())) {
                    log.info(LogMessage.PAYMENT_DATE_HAS_ARRIVED, activityType.getValue());

                    String explanation = entity + " " + account.getId() + " will send " + amount + " " + account.getCurrency();
                    addCashFlow(cashFlows, paymentDate, activityType, year, month, explanation);

                    paymentDate = switch (paymentPeriod) {
                        case ONE_TIME -> paymentDate;
                        case DAILY -> paymentDate.plusDays(1);
                        case WEEKLY -> paymentDate.plusWeeks(1);
                        case MONTHLY -> paymentDate.plusMonths(1);
                    };
                }

                counterDate = switch (paymentPeriod) {
                    case ONE_TIME, DAILY -> counterDate.plusDays(1);
                    case WEEKLY -> counterDate.plusWeeks(1);
                    case MONTHLY -> counterDate.plusMonths(1);
                };
            }
        }
    }

    private static void addFutureCashFlowsForFees(List<CashFlow> cashFlows, Account account, Integer year, Integer month) {
        LocalDate paymentDate = account.getUpdatedAt().toLocalDate();
        LocalDate counterDate = LocalDate.now();

        while (!CashFlowCalendarUtil.isDateFuture(counterDate, year, month)) {
            AccountActivityType activityType = AccountActivityType.FEE;
            if (doesDateMatchesWithYearAndMonth(paymentDate, counterDate.getYear(), counterDate.getMonthValue())) {
                log.info(LogMessage.PAYMENT_DATE_HAS_ARRIVED, activityType.getValue());
                String entity = Entity.ACCOUNT.getValue();

                if (doesDateMatchesWithYearAndMonth(account.getCreatedAt().toLocalDate(), counterDate.getYear(), counterDate.getMonthValue())) {
                    log.info("Calendar shows for {} {} creating time, so no fee", AccountType.DEPOSIT.getValue(), entity);
                } else {
                    log.info("Add the {} to the balance", Entity.FEE.getValue());

                    account.setBalance(account.getBalanceAfterNextFee());
                    double interest = AccountUtil.calculateInterest(account.getBalance(), account.getDepositPeriod(), account.getInterestRatio());
                    double balanceAfterNextFee = account.getBalance() + interest;
                    account.setBalanceAfterNextFee(balanceAfterNextFee);

                    String explanation = interest + " " + account.getCurrency() + " will be transferred to " + entity + " " + account.getId();

                    addCashFlow(cashFlows, paymentDate, activityType, year, month, explanation);
                }

                paymentDate = paymentDate.plusMonths(account.getDepositPeriod());
            }

            counterDate = counterDate.plusMonths(1);
        }
    }

    private static void addCashFlow(List<CashFlow> cashFlows, LocalDate date, AccountActivityType activityType, Integer year, Integer month, String explanation) {
        if (doesDateMatchesWithYearAndMonth(date, year, month)) {
            log.info("{} matches with a cash flow", activityType.getValue());
            CashFlow cashFlow = new CashFlow();
            cashFlow.setDate(date);
            cashFlow.setExplanation(explanation);
            cashFlows.add(cashFlow);
        }
    }

    private static boolean doesDateMatchesWithYearAndMonth(LocalDate date, Integer year, Integer month) {
        return date.getYear() == year && date.getMonthValue() == month;
    }

    private static void getPastCashFlows(Integer year, Integer month, CashFlowCalendar cashFlowCalendar, List<CashFlow> cashFlows) {
        cashFlowCalendar.getCashFlows()
                .stream()
                .filter(cashFlow -> cashFlow.getDate().getYear() == year && cashFlow.getDate().getMonthValue() == month)
                .forEach(cashFlows::add);
    }

    private double calculateTotalAmount(Account account, BalanceActivity balanceActivity, Currency toCurrency) {
        AccountActivityFilteringOption filteringOption = balanceActivity == BalanceActivity.INCREASE
                ? new AccountActivityFilteringOption(List.of(AccountActivityType.MONEY_DEPOSIT, AccountActivityType.MONEY_TRANSFER, AccountActivityType.MONEY_EXCHANGE, AccountActivityType.FEE), null, account.getId(), null, null)
                : new AccountActivityFilteringOption(List.of(AccountActivityType.WITHDRAWAL, AccountActivityType.MONEY_TRANSFER, AccountActivityType.MONEY_EXCHANGE, AccountActivityType.CHARGE), account.getId(), null, null, null);

        return accountActivityService.getAccountActivitiesOfParticularAccounts(filteringOption, account.getCurrency())
                .stream()
                .map(accountActivityDto -> exchangeService.convertMoneyBetweenCurrencies(account.getCurrency(), toCurrency, accountActivityDto.amount()))
                .reduce(0D, Double::sum);
    }

    private void getAccountActivities(Integer accountId, BalanceActivity balanceActivity, AccountActivityFilteringOption filteringOption, List<AccountActivityDto> accountActivityDtos) {
        Integer[] accountIds = new Integer[2]; // first value is sender account id, second value is receiver account id

        if (balanceActivity == BalanceActivity.DECREASE) {
            accountIds[0] = accountId;
        } else {
            accountIds[1] = accountId;
        }

        AccountActivityFilteringOption accountActivityFilteringOption = new AccountActivityFilteringOption(
                filteringOption.activityTypes(),
                accountIds[0],
                accountIds[1],
                filteringOption.minimumAmount(),
                filteringOption.createdAt()
        );

        List<AccountActivityDto> currentAccountActivityDtos = accountActivityService.getAccountActivities(accountActivityFilteringOption);
        accountActivityDtos.addAll(currentAccountActivityDtos);
    }

    private void checkUniqueness(Customer customerInDb, CustomerDto request) {
        String nationalId = request.getNationalId();
        String phoneNumber = request.getPhoneNumber();
        String email = request.getEmail();

        Predicate<Customer> nationalIdPredicate = customer -> customer.getNationalId().equals(nationalId);
        Predicate<Customer> phoneNumberPredicate = customer -> customer.getPhoneNumber().equals(phoneNumber);
        Predicate<Customer> emailPredicate = customer -> customer.getEmail().equals(email);

        if (Optional.ofNullable(customerInDb).isPresent()) { // Add related predicates for updateEntity case
            Predicate<Customer> customerInDbPredicate = customer -> !customerInDb.getNationalId().equals(nationalId);
            nationalIdPredicate = customerInDbPredicate.and(nationalIdPredicate);

            customerInDbPredicate = customer -> !customerInDb.getPhoneNumber().equals(phoneNumber);
            phoneNumberPredicate = customerInDbPredicate.and(phoneNumberPredicate);

            customerInDbPredicate = customer -> !customerInDb.getEmail().equals(email);
            emailPredicate = customerInDbPredicate.and(emailPredicate);
        }

        Predicate<Customer> customerPredicate = nationalIdPredicate
                .or(phoneNumberPredicate)
                .or(emailPredicate);

        boolean customerExists = customerRepository.findAll()
                .stream()
                .anyMatch(customerPredicate);

        String entity = Entity.CUSTOMER.getValue();

        if (customerExists) {
            log.error(LogMessage.RESOURCE_NOT_UNIQUE, entity);
            throw new ResourceConflictException(String.format(ResponseMessage.ALREADY_EXISTS, entity));
        }

        log.info(LogMessage.RESOURCE_UNIQUE, entity);
    }
}
