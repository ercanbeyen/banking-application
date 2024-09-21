package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.BalanceActivity;
import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessages;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessages;
import com.ercanbeyen.bankingapplication.dto.*;
import com.ercanbeyen.bankingapplication.dto.response.WorthResponse;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.entity.Customer;
import com.ercanbeyen.bankingapplication.entity.File;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.*;
import com.ercanbeyen.bankingapplication.option.AccountFilteringOptions;
import com.ercanbeyen.bankingapplication.option.CustomerFilteringOptions;
import com.ercanbeyen.bankingapplication.option.AccountActivityFilteringOptions;
import com.ercanbeyen.bankingapplication.repository.CustomerRepository;
import com.ercanbeyen.bankingapplication.service.BaseService;
import com.ercanbeyen.bankingapplication.service.FileStorageService;
import com.ercanbeyen.bankingapplication.service.AccountActivityService;
import com.ercanbeyen.bankingapplication.util.LoggingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService implements BaseService<CustomerDto, CustomerFilteringOptions> {
    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final AccountMapper accountMapper;
    private final RegularTransferOrderMapper regularTransferOrderMapper;
    private final AddressMapper addressMapper;
    private final NotificationMapper notificationMapper;
    private final FileStorageService fileStorageService;
    private final AccountActivityService accountActivityService;
    private final ExchangeService exchangeService;

    @Override
    public List<CustomerDto> getEntities(CustomerFilteringOptions options) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Predicate<Customer> customerPredicate = customer -> {
            LocalDate filteringDay = options.getBirthDate();
            LocalDate customerBirthday = customer.getBirthDate();
            Boolean birthDayCondition = (filteringDay == null)
                    || (filteringDay.getMonth() == customerBirthday.getMonth() && filteringDay.getDayOfMonth() == customerBirthday.getDayOfMonth());

            Boolean createTimeCondition = (options.getCreatedAt() == null || options.getCreatedAt().isEqual(options.getCreatedAt()));

            return (birthDayCondition && createTimeCondition);
        };

        List<CustomerDto> customerDtos = new ArrayList<>();

        customerRepository.findAll()
                .stream()
                .filter(customerPredicate)
                .forEach(customer -> customerDtos.add(customerMapper.entityToDto(customer)));

        return customerDtos;
    }

    @Override
    public CustomerDto getEntity(Integer id) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());
        Customer customer = findById(id);
        return customerMapper.entityToDto(customer);
    }

    @Override
    public CustomerDto createEntity(CustomerDto request) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        checkCustomerUniqueness(null, request);
        Customer customer = customerMapper.dtoToEntity(request);

        Customer savedCustomer = customerRepository.save(customer);
        log.info(LogMessages.RESOURCE_CREATE_SUCCESS, Entity.CUSTOMER.getValue(), savedCustomer.getId());

        return customerMapper.entityToDto(savedCustomer);
    }

    @Transactional
    @Override
    public CustomerDto updateEntity(Integer id, CustomerDto request) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Customer customer = findById(id);
        checkCustomerUniqueness(customer, request);

        Customer requestCustomer = customerMapper.dtoToEntity(request);
        customer.setName(requestCustomer.getName());
        customer.setSurname(requestCustomer.getSurname());
        customer.setPhoneNumber(requestCustomer.getPhoneNumber());
        customer.setEmail(requestCustomer.getEmail());
        customer.setGender(requestCustomer.getGender());
        customer.setBirthDate(requestCustomer.getBirthDate());

        return customerMapper.entityToDto(customerRepository.save(customer));
    }

    @Override
    public void deleteEntity(Integer id) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());
        Customer customer = findById(id);
        customerRepository.delete(customer);
    }

    public String uploadProfilePhoto(Integer id, MultipartFile file) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Customer customer = findById(id);
        CompletableFuture<File> photo = fileStorageService.storeFile(file);
        customer.setProfilePhoto(photo.join()); // Profile photo upload
        customerRepository.save(customer);

        return ResponseMessages.FILE_UPLOAD_SUCCESS;
    }

    public File downloadProfilePhoto(Integer id) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());
        Customer customer = findById(id);
        return customer.getProfilePhoto()
                .orElseThrow(() -> new ResourceNotFoundException(ResponseMessages.NOT_FOUND));
    }

    public String deleteProfilePhoto(Integer id) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Customer customer = findById(id);
        customer.setProfilePhoto(null); // Profile photo deletion
        customerRepository.save(customer);

        return ResponseMessages.FILE_DELETE_SUCCESS;
    }

    public WorthResponse calculateWorth(String nationalId) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        List<Account> accounts = findByNationalId(nationalId).getAccounts();
        double earning = 0;
        double spending = 0;

        for (Account account : accounts) {
            earning += calculateTotalAmount(account, BalanceActivity.INCREASE);
            spending += calculateTotalAmount(account, BalanceActivity.DECREASE);
            log.info("Earning and Spending for Account {}: {} & {}", earning, spending, account.getId());
        }

        Double netWorth = accounts.stream()
                .map(account -> exchangeService.convertMoney(account.getCurrency(), Currency.TL, account.getBalance()))
                .reduce(0D, Double::sum);

        return new WorthResponse(earning, spending, netWorth);
    }

    public List<AccountDto> getAccounts(Integer id, AccountFilteringOptions options) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Customer customer = findById(id);
        Predicate<Account> accountPredicate = account -> (account.getCustomer().getNationalId().equals(customer.getNationalId()))
                && (options.getType() == null || options.getType() == account.getType())
                && (options.getCreatedAt() == null || options.getCreatedAt().getYear() <= account.getCreatedAt().getYear());

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

    public List<AccountActivityDto> getAccountActivities(Integer id, AccountActivityFilteringOptions options) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Customer customer = findById(id);
        List<AccountActivityDto> accountActivityDtos = new ArrayList<>();

        List<Integer> accountIds = customer.getAccounts()
                .stream()
                .map(Account::getId)
                .toList();

        /* Get all transactions of each account */
        accountIds.forEach(accountId -> {
            getAccountActivities(accountId, true, options, accountActivityDtos);
            getAccountActivities(accountId, false, options, accountActivityDtos);
        });

        Comparator<AccountActivityDto> transactionDtoComparator = Comparator.comparing(AccountActivityDto::createdAt).reversed();

        return accountActivityDtos.stream()
                .sorted(transactionDtoComparator)
                .toList();
    }

    public List<NotificationDto> getNotifications(Integer id) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Customer customer = findById(id);
        List<NotificationDto> notificationDtos = new ArrayList<>();

        customer.getNotifications()
                .forEach(notification -> notificationDtos.add(notificationMapper.entityToDto(notification)));

        return notificationDtos;
    }

    public List<AddressDto> getAddresses(Integer id) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Customer customer = findById(id);
        List<AddressDto> addressDtos = new ArrayList<>();

        customer.getAddresses()
                .forEach(address -> addressDtos.add(addressMapper.entityToDto(address)));

        return addressDtos;
    }

    public List<RegularTransferOrderDto> getRegularTransferOrders(Integer customerId, Integer accountId) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Customer customer = findById(customerId);
        String entity = Entity.ACCOUNT.getValue();
        Account account = customer.getAccount(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessages.NOT_FOUND, entity)));

        log.info(LogMessages.RESOURCE_FOUND, entity);

        return account.getRegularTransferOrders()
                .stream()
                .map(regularTransferOrderMapper::entityToDto)
                .toList();
    }

    /**
     * @param nationalId is national identity which is unique for each customer
     * @return customer corresponds to that nationalId
     */
    public Customer findByNationalId(String nationalId) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        String entity = Entity.CUSTOMER.getValue();
        Customer customer = customerRepository.findByNationalId(nationalId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessages.NOT_FOUND, entity)));

        log.info(LogMessages.RESOURCE_FOUND, entity);

        return customer;
    }

    /***
     *
     * @param nationalId is national identity which is unique for each customer
     * @return status for customer existence corresponds to nationalId
     */
    public boolean existsByNationalId(String nationalId) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());
        return customerRepository.existsByNationalId(nationalId);
    }

    private Customer findById(Integer id) {
        String entity = Entity.CUSTOMER.getValue();
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessages.NOT_FOUND, entity)));

        log.info(LogMessages.RESOURCE_FOUND, entity);

        return customer;
    }

    private double calculateTotalAmount(Account account, BalanceActivity balanceActivity) {
        AccountActivityFilteringOptions options = balanceActivity == BalanceActivity.INCREASE ? new AccountActivityFilteringOptions(List.of(AccountActivityType.MONEY_DEPOSIT, AccountActivityType.MONEY_TRANSFER, AccountActivityType.MONEY_EXCHANGE, AccountActivityType.FEE), null, account.getId(), null, null)
                : new AccountActivityFilteringOptions(List.of(AccountActivityType.WITHDRAWAL, AccountActivityType.MONEY_TRANSFER, AccountActivityType.MONEY_EXCHANGE, AccountActivityType.CHARGE), account.getId(), null, null, null);

        return accountActivityService.getAccountActivitiesOfParticularAccounts(options, account.getCurrency())
                .stream()
                .map(accountActivityDto -> exchangeService.convertMoney(account.getCurrency(), Currency.TL, accountActivityDto.amount()))
                .reduce(0D, Double::sum);
    }

    private void getAccountActivities(Integer accountId, boolean isSender, AccountActivityFilteringOptions options, List<AccountActivityDto> accountActivityDtos) {
        AccountActivityFilteringOptions accountActivityFilteringOptions = isSender ?
                new AccountActivityFilteringOptions(options.activityTypes(), accountId, null, options.minimumAmount(), options.createdAt()) :
                new AccountActivityFilteringOptions(options.activityTypes(), null, accountId, options.minimumAmount(), options.createdAt());
        List<AccountActivityDto> currentAccountActivityDtos = accountActivityService.getAccountActivities(accountActivityFilteringOptions);
        accountActivityDtos.addAll(currentAccountActivityDtos);
    }

    private void checkCustomerUniqueness(Customer customerInDb, CustomerDto request) {
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

        if (customerExists) {
            throw new ResourceConflictException(String.format(ResponseMessages.ALREADY_EXISTS, Entity.CUSTOMER.getValue()));
        }

        log.info(LogMessages.RESOURCE_UNIQUE, Entity.CUSTOMER.getValue());
    }
}
