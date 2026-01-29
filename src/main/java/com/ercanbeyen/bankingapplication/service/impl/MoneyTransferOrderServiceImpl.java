package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.MoneyTransferOrderDto;
import com.ercanbeyen.bankingapplication.dto.RegularMoneyTransferDto;
import com.ercanbeyen.bankingapplication.embeddable.RegularMoneyTransfer;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.entity.MoneyTransferOrder;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.MoneyTransferOrderMapper;
import com.ercanbeyen.bankingapplication.option.MoneyTransferOrderOption;
import com.ercanbeyen.bankingapplication.repository.MoneyTransferOrderRepository;
import com.ercanbeyen.bankingapplication.service.AccountService;
import com.ercanbeyen.bankingapplication.service.MoneyTransferOrderService;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoneyTransferOrderServiceImpl implements MoneyTransferOrderService {
    private final MoneyTransferOrderRepository moneyTransferOrderRepository;
    private final MoneyTransferOrderMapper moneyTransferOrderMapper;
    private final AccountService accountService;

    @Override
    public List<MoneyTransferOrderDto> getEntities(MoneyTransferOrderOption filteringOption) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Predicate<MoneyTransferOrder> transferOrderPredicate = moneyTransferOrder -> {
            RegularMoneyTransfer regularMoneyTransfer = moneyTransferOrder.getRegularMoneyTransfer();
            boolean senderAccountIdFilter = (filteringOption.getSenderAccountId() == null || filteringOption.getSenderAccountId().equals(moneyTransferOrder.getSenderAccount().getId()));
            boolean recipientAccountIdFilter = (filteringOption.getRecipientAccountId() == null || filteringOption.getRecipientAccountId().equals(regularMoneyTransfer.getRecipientAccount().getId()));
            boolean transferDateFilter = (filteringOption.getTransferDate() == null || filteringOption.getTransferDate().isEqual(moneyTransferOrder.getTransferDate()));
            boolean paymentTypeFilter = (filteringOption.getPaymentType() == null || filteringOption.getPaymentType() == regularMoneyTransfer.getPaymentType());

            return senderAccountIdFilter && recipientAccountIdFilter && transferDateFilter && paymentTypeFilter;
        };

        return moneyTransferOrderRepository.findAll()
                .stream()
                .filter(transferOrderPredicate)
                .map(moneyTransferOrderMapper::entityToDto)
                .toList();
    }

    @Override
    public MoneyTransferOrderDto getEntity(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        MoneyTransferOrder moneyTransferOrder = findById(id);
        return moneyTransferOrderMapper.entityToDto(moneyTransferOrder);
    }

    @Override
    public MoneyTransferOrderDto createEntity(MoneyTransferOrderDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        MoneyTransferOrder moneyTransferOrder = constructMoneyTransferOrder(request);
        MoneyTransferOrder savedMoneyTransferOrder = moneyTransferOrderRepository.save(moneyTransferOrder);
        log.info(LogMessage.RESOURCE_CREATE_SUCCESS, Entity.MONEY_TRANSFER_ORDER.getValue(), savedMoneyTransferOrder.getId());

        return moneyTransferOrderMapper.entityToDto(savedMoneyTransferOrder);
    }

    @Override
    public MoneyTransferOrderDto updateEntity(Integer id, MoneyTransferOrderDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        MoneyTransferOrder moneyTransferOrder = findById(id);

        List<Account> accounts = getAccountsFromRegularTransferDto(request);
        moneyTransferOrder.setSenderAccount(accounts.getFirst());
        moneyTransferOrder.setChargedAccount(accounts.getLast());

        RegularMoneyTransfer regularMoneyTransfer = moneyTransferOrder.getRegularMoneyTransfer();
        regularMoneyTransfer.setRecipientAccount(accounts.get(1));
        regularMoneyTransfer.setAmount(request.getRegularMoneyTransferDto().amount());
        regularMoneyTransfer.setPaymentPeriod(request.getRegularMoneyTransferDto().paymentPeriod());
        regularMoneyTransfer.setPaymentType(request.getRegularMoneyTransferDto().paymentType());
        regularMoneyTransfer.setExplanation(request.getRegularMoneyTransferDto().explanation());

        moneyTransferOrder.setTransferDate(request.getTransferDate());

        return moneyTransferOrderMapper.entityToDto(moneyTransferOrderRepository.save(moneyTransferOrder));
    }

    @Override
    public void deleteEntity(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        String entity = Entity.MONEY_TRANSFER_ORDER.getValue();

        moneyTransferOrderRepository.findById(id)
                .ifPresentOrElse(_ -> {
                    log.info(LogMessage.RESOURCE_FOUND, entity);
                    moneyTransferOrderRepository.deleteById(id);
                }, () -> {
                    log.error(LogMessage.RESOURCE_NOT_FOUND, entity);
                    throw new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity));
                });

        log.info(LogMessage.RESOURCE_DELETE_SUCCESS, Entity.MONEY_TRANSFER_ORDER.getValue(), id);
    }

    private MoneyTransferOrder constructMoneyTransferOrder(MoneyTransferOrderDto request) {
        List<Account> accounts = getAccountsFromRegularTransferDto(request);
        RegularMoneyTransfer regularMoneyTransfer = constructRegularTransfer(request, accounts);
        MoneyTransferOrder moneyTransferOrder = new MoneyTransferOrder();

        moneyTransferOrder.setSenderAccount(accounts.getFirst());
        moneyTransferOrder.setChargedAccount(accounts.getLast());
        moneyTransferOrder.setRegularMoneyTransfer(regularMoneyTransfer);
        moneyTransferOrder.setTransferDate(request.getTransferDate());

        return moneyTransferOrder;
    }

    private static RegularMoneyTransfer constructRegularTransfer(MoneyTransferOrderDto request, List<Account> accounts) {
        RegularMoneyTransferDto regularMoneyTransferDto = request.getRegularMoneyTransferDto();
        return new RegularMoneyTransfer(
                accounts.get(1),
                regularMoneyTransferDto.paymentPeriod(),
                regularMoneyTransferDto.amount(),
                regularMoneyTransferDto.paymentType(),
                regularMoneyTransferDto.explanation()
        );
    }

    /***
     *
     * @param request is MoneyTransferOrderDto object
     * @return list contains sender, recipient and charged accounts respectively
     */
    private List<Account> getAccountsFromRegularTransferDto(MoneyTransferOrderDto request) {
        Account senderAccount = accountService.findActiveAccountById(request.getSenderAccountId());
        Account recipientAccount = accountService.findActiveAccountById(request.getRegularMoneyTransferDto().recipientAccountId());

        accountService.checkAccountsBeforeMoneyTransfer(senderAccount, recipientAccount);

        Account chargedAccount = accountService.getChargedAccount(AccountActivityType.MONEY_TRANSFER, request.getChargedAccountId(), List.of(senderAccount, recipientAccount));

        return List.of(senderAccount, recipientAccount, chargedAccount);
    }

    private MoneyTransferOrder findById(Integer id) {
        String entity = Entity.MONEY_TRANSFER_ORDER.getValue();
        MoneyTransferOrder moneyTransferOrder = moneyTransferOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity)));

        log.info(LogMessage.RESOURCE_FOUND, entity);

        return moneyTransferOrder;
    }
}
