package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessages;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessages;
import com.ercanbeyen.bankingapplication.dto.AccountActivityDto;
import com.ercanbeyen.bankingapplication.dto.request.AccountActivityRequest;
import com.ercanbeyen.bankingapplication.entity.AccountActivity;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.util.AccountActivityUtils;
import com.ercanbeyen.bankingapplication.view.entity.AccountActivityView;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.AccountActivityMapper;
import com.ercanbeyen.bankingapplication.option.AccountActivityFilteringOptions;
import com.ercanbeyen.bankingapplication.repository.AccountActivityRepository;
import com.ercanbeyen.bankingapplication.view.repository.AccountActivityViewRepository;
import com.ercanbeyen.bankingapplication.service.AccountActivityService;
import com.ercanbeyen.bankingapplication.util.LoggingUtils;
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
    public List<AccountActivityDto> getAccountActivities(AccountActivityFilteringOptions options) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Predicate<AccountActivity> accountActivityPredicate = accountActivity -> (checkAccountActivity(options, accountActivity))
                && (options.senderAccountId() == null || options.senderAccountId().equals(accountActivity.getSenderAccount().getId()))
                && (options.receiverAccountId() == null || options.receiverAccountId().equals(accountActivity.getReceiverAccount().getId()))
                && (options.minimumAmount() == null || options.minimumAmount() <= accountActivity.getAmount())
                && (options.createdAt() == null || (options.createdAt().isEqual(accountActivity.getCreatedAt().toLocalDate())));

        Comparator<AccountActivity> activityComparator = Comparator.comparing(AccountActivity::getCreatedAt).reversed();

        return accountActivityRepository.findAll()
                .stream()
                .filter(accountActivityPredicate)
                .sorted(activityComparator)
                .map(accountActivityMapper::entityToDto)
                .toList();
    }

    @Override
    public Set<AccountActivityDto> getAccountActivitiesOfParticularAccounts(AccountActivityFilteringOptions options, Currency currency) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        List<AccountActivity> accountActivities = getAccountActivitiesOfSpecificEvent(options, currency);
        Set<AccountActivityDto> accountActivityDtos = new HashSet<>();

        for (AccountActivity accountActivity : accountActivities) {
            if (checkAccountActivity(options, accountActivity)) {
                accountActivityDtos.add(accountActivityMapper.entityToDto(accountActivity));
            }
        }

        return accountActivityDtos;
    }

    @Override
    public AccountActivityDto getAccountActivity(String id) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());
        AccountActivity accountActivity = findById(id);
        return accountActivityMapper.entityToDto(accountActivity);
    }

    @Override
    public void createAccountActivity(AccountActivityRequest request) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        if (request == null) {
            throw new ResourceNotFoundException(String.format(ResponseMessages.NOT_FOUND, Entity.ACCOUNT_ACTIVITY.getValue() + " request"));
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
        log.info(LogMessages.RESOURCE_CREATE_SUCCESS, Entity.ACCOUNT_ACTIVITY.getValue(), savedAccountActivity.getId());
    }

    @Override
    public List<AccountActivityView> getAccountActivityViews(Integer senderAccountId, Integer receiverAccountId) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());
        return accountActivityViewRepository.findBySenderAccountIdAndReceiverAccountId(senderAccountId, receiverAccountId);
    }

    @Override
    public ByteArrayOutputStream generateReceiptPdfStream(String id) {
        AccountActivity accountActivity = findById(id);
        ByteArrayOutputStream outputStream;

        try {
            outputStream = AccountActivityUtils.generatePdfStream(accountActivity.getSummary());
            log.info("Receipt is successfully generated");
        } catch (DocumentException exception) {
            log.error("Receipt cannot be created. Exception: {}", exception.getMessage());
            throw new RuntimeException("Error occurred while creating receipt");
        } catch (Exception exception) {
            log.error("Unknown error occurred while creating receipt");
            throw new RuntimeException("Error occurred while creating receipt");
        }

        return outputStream;
    }

    private AccountActivity findById(String id) {
        String entity = Entity.ACCOUNT_ACTIVITY.getValue();
        AccountActivity accountActivity = accountActivityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessages.NOT_FOUND, entity)));

        log.info(LogMessages.RESOURCE_FOUND, entity);

        return accountActivity;
    }

    private List<AccountActivity> getAccountActivitiesOfSpecificEvent(AccountActivityFilteringOptions options, Currency currency) {
        boolean senderAccountPresents = Optional.ofNullable(options.senderAccountId()).isPresent();
        boolean receiverAccountPresents = Optional.ofNullable(options.receiverAccountId()).isPresent();
        List<AccountActivity> accountActivities;
        Predicate<AccountActivity> accountActivityPredicate;

        if (senderAccountPresents && receiverAccountPresents) {
            accountActivities = accountActivityRepository.findBySenderAccountIdOrReceiverAccountId(options.senderAccountId(), options.receiverAccountId());
            accountActivityPredicate = accountActivity -> accountActivity.getSenderAccount().getCurrency() == currency;
        } else if (senderAccountPresents) {
            accountActivities = accountActivityRepository.findBySenderAccountId(options.senderAccountId());
            accountActivityPredicate = accountActivity -> accountActivity.getSenderAccount().getCurrency() == currency;
        } else if (receiverAccountPresents) {
            accountActivities = accountActivityRepository.findByReceiverAccountId(options.receiverAccountId());
            accountActivityPredicate = accountActivity -> accountActivity.getReceiverAccount().getCurrency() == currency;
        } else {
            throw new ResourceConflictException("Both accounts cannot be null");
        }

        return accountActivities.stream()
                .filter(accountActivityPredicate)
                .toList();
    }

    private static boolean checkAccountActivity(AccountActivityFilteringOptions options, AccountActivity accountActivity) {
        boolean typeCheck = Optional.ofNullable(options.activityTypes()).isEmpty() || options.activityTypes()
                .stream()
                .anyMatch(activityType -> accountActivity.getType() == activityType);
        boolean amountCheck = Optional.ofNullable(options.minimumAmount()).isEmpty() || options.minimumAmount() <= accountActivity.getAmount();
        boolean createdAtCheck = Optional.ofNullable(options.createdAt()).isEmpty() || options.createdAt().isEqual(accountActivity.getCreatedAt().toLocalDate());

        return typeCheck && amountCheck && createdAtCheck;
    }
}
