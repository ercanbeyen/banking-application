package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.AccountActivityDto;
import com.ercanbeyen.bankingapplication.dto.request.AccountActivityRequest;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.entity.AccountActivity;
import com.ercanbeyen.bankingapplication.exception.InternalServerErrorException;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.util.AccountActivityUtil;
import com.ercanbeyen.bankingapplication.view.entity.AccountActivityView;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.AccountActivityMapper;
import com.ercanbeyen.bankingapplication.option.AccountActivityFilteringOption;
import com.ercanbeyen.bankingapplication.repository.AccountActivityRepository;
import com.ercanbeyen.bankingapplication.view.repository.AccountActivityViewRepository;
import com.ercanbeyen.bankingapplication.service.AccountActivityService;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import com.itextpdf.text.DocumentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountActivityServiceImpl implements AccountActivityService {
    private final AccountActivityRepository accountActivityRepository;
    private final AccountActivityViewRepository accountActivityViewRepository;
    private final AccountActivityMapper accountActivityMapper;

    @Override
    public List<AccountActivityDto> getAccountActivities(AccountActivityFilteringOption filteringOption) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Predicate<AccountActivity> accountActivityPredicate = accountActivity -> {
            boolean accountActivityCheck = checkAccountActivity(filteringOption, accountActivity);
            boolean senderAccountIdFilter = filteringOption.senderAccountId() == null
                    || (accountActivity.getSenderAccount() != null && filteringOption.senderAccountId().equals(accountActivity.getSenderAccount().getId()));
            boolean receiverAccountIdFilter = filteringOption.receiverAccountId() == null
                    || (accountActivity.getReceiverAccount() != null && filteringOption.receiverAccountId().equals(accountActivity.getReceiverAccount().getId()));
            boolean minimumAmountFilter = (filteringOption.minimumAmount() == null || filteringOption.minimumAmount() <= accountActivity.getAmount());
            boolean createdAtFilter = (filteringOption.createdAt() == null || (filteringOption.createdAt().isEqual(accountActivity.getCreatedAt().toLocalDate())));
            
            return accountActivityCheck && senderAccountIdFilter && receiverAccountIdFilter && minimumAmountFilter && createdAtFilter;
        };

        Comparator<AccountActivity> activityComparator = Comparator.comparing(AccountActivity::getCreatedAt).reversed();

        return accountActivityRepository.findAll()
                .stream()
                .filter(accountActivityPredicate)
                .sorted(activityComparator)
                .map(accountActivityMapper::entityToDto)
                .toList();
    }

    @Override
    public Set<AccountActivityDto> getAccountActivitiesOfParticularAccounts(AccountActivityFilteringOption filteringOption, Currency currency) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        List<AccountActivity> accountActivities = getAccountActivitiesOfSpecificEvent(filteringOption, currency);
        Set<AccountActivityDto> accountActivityDtos = new HashSet<>();

        for (AccountActivity accountActivity : accountActivities) {
            if (checkAccountActivity(filteringOption, accountActivity)) {
                accountActivityDtos.add(accountActivityMapper.entityToDto(accountActivity));
            }
        }

        return accountActivityDtos;
    }

    @Override
    public AccountActivityDto getAccountActivity(String id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        AccountActivity accountActivity = findById(id);
        return accountActivityMapper.entityToDto(accountActivity);
    }

    @Override
    public AccountActivity createAccountActivity(AccountActivityRequest request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        if (request == null) {
            throw new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, Entity.ACCOUNT_ACTIVITY.getValue() + " request"));
        }

        AccountActivity accountActivity = new AccountActivity(
                request.activityType(),
                request.senderAccount(),
                request.receiverAccount(),
                request.amount(),
                request.summary(),
                request.explanation()
        );

        AccountActivity savedAccountActivity = accountActivityRepository.save(accountActivity);
        log.info(LogMessage.RESOURCE_CREATE_SUCCESS, Entity.ACCOUNT_ACTIVITY.getValue(), savedAccountActivity.getId());

        return savedAccountActivity;
    }

    @Override
    public List<AccountActivityView> getAccountActivityViews(Integer senderAccountId, Integer receiverAccountId) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        return accountActivityViewRepository.findBySenderAccountIdAndReceiverAccountId(senderAccountId, receiverAccountId);
    }

    @Override
    public ByteArrayOutputStream createReceiptStream(String id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        AccountActivity accountActivity = findById(id);

        if (AccountActivityType.getAccountStatusUpdatingActivities().contains(accountActivity.getType())) {
            throw new ResourceConflictException(ResponseMessage.IMPROPER_ACCOUNT_ACTIVITY + ". Receipt cannot be generated for " + AccountActivityType.getAccountStatusUpdatingActivities());
        }

        log.info("{} is a proper account activity for receipt generation", accountActivity);
        ByteArrayOutputStream outputStream;

        try {
            outputStream = AccountActivityUtil.generatePdfStream(accountActivity.getSummary());
            log.info("Receipt is successfully generated");
        } catch (DocumentException exception) {
            log.error("Receipt cannot be created. Exception: {}", exception.getMessage());
            throw new InternalServerErrorException("Error occurred while creating receipt");
        } catch (Exception exception) {
            log.error("Unknown exception occurred. Exception: {}", exception.getMessage());
            throw new InternalServerErrorException("Unknown error occurred while creating receipt");
        }

        return outputStream;
    }

    @Override
    public boolean existsByIdAndCustomerNationalId(String id, String customerNationalId) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        AccountActivity accountActivity = findById(id);
        boolean accountActivityExists;

        Predicate<Account> accountPredicate = account -> account.getCustomer()
                .getNationalId()
                .equals(customerNationalId);

        if (accountActivity.getType() == AccountActivityType.MONEY_DEPOSIT || accountActivity.getType() == AccountActivityType.FEE) {
            accountActivityExists = accountPredicate.test(accountActivity.getReceiverAccount());
        } else if (accountActivity.getType() == AccountActivityType.WITHDRAWAL || accountActivity.getType() == AccountActivityType.CHARGE) {
            accountActivityExists = accountPredicate.test(accountActivity.getSenderAccount());
        } else if (accountActivity.getType() == AccountActivityType.MONEY_TRANSFER || accountActivity.getType() == AccountActivityType.MONEY_EXCHANGE) {
            boolean receiverAccountIsCustomer = accountPredicate.test(accountActivity.getReceiverAccount());
            boolean senderAccountIsCustomer = accountPredicate.test(accountActivity.getSenderAccount());
            accountActivityExists = receiverAccountIsCustomer || senderAccountIsCustomer;
        } else {
            log.error("{} {} is improper for this method", Entity.ACCOUNT_ACTIVITY.getValue(), accountActivity.getType());
            throw new ResourceConflictException(ResponseMessage.IMPROPER_ACCOUNT_ACTIVITY);
        }

        return accountActivityExists;
    }

    private AccountActivity findById(String id) {
        String entity = Entity.ACCOUNT_ACTIVITY.getValue();
        AccountActivity accountActivity = accountActivityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity)));

        log.info(LogMessage.RESOURCE_FOUND, entity);

        return accountActivity;
    }

    private List<AccountActivity> getAccountActivitiesOfSpecificEvent(AccountActivityFilteringOption filteringOption, Currency currency) {
        boolean senderAccountPresents = Optional.ofNullable(filteringOption.senderAccountId()).isPresent();
        boolean receiverAccountPresents = Optional.ofNullable(filteringOption.receiverAccountId()).isPresent();
        List<AccountActivity> accountActivities;
        Predicate<AccountActivity> accountActivityPredicate;

        if (senderAccountPresents && receiverAccountPresents) {
            accountActivities = accountActivityRepository.findBySenderAccountIdOrReceiverAccountId(filteringOption.senderAccountId(), filteringOption.receiverAccountId());
            accountActivityPredicate = accountActivity -> accountActivity.getSenderAccount().getCurrency() == currency;
        } else if (senderAccountPresents) {
            accountActivities = accountActivityRepository.findBySenderAccountId(filteringOption.senderAccountId());
            accountActivityPredicate = accountActivity -> accountActivity.getSenderAccount().getCurrency() == currency;
        } else if (receiverAccountPresents) {
            accountActivities = accountActivityRepository.findByReceiverAccountId(filteringOption.receiverAccountId());
            accountActivityPredicate = accountActivity -> accountActivity.getReceiverAccount().getCurrency() == currency;
        } else {
            throw new ResourceConflictException("Both accounts cannot be null");
        }

        return accountActivities.stream()
                .filter(accountActivityPredicate)
                .toList();
    }

    private static boolean checkAccountActivity(AccountActivityFilteringOption filteringOption, AccountActivity accountActivity) {
        boolean typeCheck = Optional.ofNullable(filteringOption.activityTypes()).isEmpty() || filteringOption.activityTypes()
                .stream()
                .anyMatch(activityType -> accountActivity.getType() == activityType);
        boolean amountCheck = Optional.ofNullable(filteringOption.minimumAmount()).isEmpty() || filteringOption.minimumAmount() <= accountActivity.getAmount();
        boolean createdAtCheck = Optional.ofNullable(filteringOption.createdAt()).isEmpty() || filteringOption.createdAt().isEqual(accountActivity.getCreatedAt().toLocalDate());

        return typeCheck && amountCheck && createdAtCheck;
    }
}
