package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.RegularTransferDto;
import com.ercanbeyen.bankingapplication.dto.TransferOrderDto;
import com.ercanbeyen.bankingapplication.embeddable.RegularTransfer;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.entity.TransferOrder;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.TransferOrderMapper;
import com.ercanbeyen.bankingapplication.option.TransferOrderOption;
import com.ercanbeyen.bankingapplication.repository.TransferOrderRepository;
import com.ercanbeyen.bankingapplication.service.AccountService;
import com.ercanbeyen.bankingapplication.service.TransferOrderService;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferOrderServiceImpl implements TransferOrderService {
    private final TransferOrderRepository transferOrderRepository;
    private final TransferOrderMapper transferOrderMapper;
    private final AccountService accountService;

    @Override
    public List<TransferOrderDto> getEntities(TransferOrderOption filteringOption) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Predicate<TransferOrder> transferOrderPredicate = transferOrder -> {
            RegularTransfer regularTransfer = transferOrder.getRegularTransfer();
            boolean senderAccountIdFilter = (filteringOption.getSenderAccountId() == null || filteringOption.getSenderAccountId().equals(transferOrder.getSenderAccount().getId()));
            boolean receiverAccountIdFilter = (filteringOption.getReceiverAccountId() == null || filteringOption.getReceiverAccountId().equals(regularTransfer.getReceiverAccount().getId()));
            boolean transferDateFilter = (filteringOption.getTransferDate() == null || filteringOption.getTransferDate().isEqual(transferOrder.getTransferDate()));
            boolean paymentTypeFilter = (filteringOption.getPaymentType() == null || filteringOption.getPaymentType() == regularTransfer.getPaymentType());

            return senderAccountIdFilter && receiverAccountIdFilter && transferDateFilter && paymentTypeFilter;
        };

        return transferOrderRepository.findAll()
                .stream()
                .filter(transferOrderPredicate)
                .map(transferOrderMapper::entityToDto)
                .toList();
    }

    @Override
    public TransferOrderDto getEntity(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        TransferOrder transferOrder = findById(id);
        return transferOrderMapper.entityToDto(transferOrder);
    }

    @Override
    public TransferOrderDto createEntity(TransferOrderDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        TransferOrder transferOrder = constructTransferOrder(request);
        TransferOrder savedTransferOrder = transferOrderRepository.save(transferOrder);
        log.info(LogMessage.RESOURCE_CREATE_SUCCESS, Entity.TRANSFER_ORDER.getValue(), savedTransferOrder.getId());

        return transferOrderMapper.entityToDto(savedTransferOrder);
    }

    @Override
    public TransferOrderDto updateEntity(Integer id, TransferOrderDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        TransferOrder transferOrder = findById(id);

        List<Account> accounts = getAccountsFromRegularTransferDto(request);
        transferOrder.setSenderAccount(accounts.getFirst());
        transferOrder.setChargedAccount(accounts.getLast());

        RegularTransfer regularTransfer = transferOrder.getRegularTransfer();
        regularTransfer.setReceiverAccount(accounts.get(1));
        regularTransfer.setAmount(request.getRegularTransferDto().amount());
        regularTransfer.setPaymentPeriod(request.getRegularTransferDto().paymentPeriod());
        regularTransfer.setPaymentType(request.getRegularTransferDto().paymentType());
        regularTransfer.setExplanation(request.getRegularTransferDto().explanation());

        transferOrder.setTransferDate(request.getTransferDate());

        return transferOrderMapper.entityToDto(transferOrderRepository.save(transferOrder));
    }

    @Override
    public void deleteEntity(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        String entity = Entity.TRANSFER_ORDER.getValue();

        transferOrderRepository.findById(id)
                .ifPresentOrElse(_ -> {
                    log.info(LogMessage.RESOURCE_FOUND, entity);
                    transferOrderRepository.deleteById(id);
                }, () -> {
                    log.error(LogMessage.RESOURCE_NOT_FOUND, entity);
                    throw new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity));
                });

        log.info(LogMessage.RESOURCE_DELETE_SUCCESS, Entity.TRANSFER_ORDER.getValue(), id);
    }

    private TransferOrder constructTransferOrder(TransferOrderDto request) {
        List<Account> accounts = getAccountsFromRegularTransferDto(request);
        RegularTransfer regularTransfer = constructRegularTransfer(request, accounts);
        TransferOrder transferOrder = new TransferOrder();

        transferOrder.setSenderAccount(accounts.getFirst());
        transferOrder.setChargedAccount(accounts.getLast());
        transferOrder.setRegularTransfer(regularTransfer);
        transferOrder.setTransferDate(request.getTransferDate());

        return transferOrder;
    }

    private static RegularTransfer constructRegularTransfer(TransferOrderDto request, List<Account> accounts) {
        RegularTransferDto regularTransferDto = request.getRegularTransferDto();
        return new RegularTransfer(
                accounts.get(1),
                regularTransferDto.paymentPeriod(),
                regularTransferDto.amount(),
                regularTransferDto.paymentType(),
                regularTransferDto.explanation()
        );
    }

    /***
     *
     * @param request is TransferOrderDto object
     * @return list contains sender, receiver and charged accounts respectively
     */
    private List<Account> getAccountsFromRegularTransferDto(TransferOrderDto request) {
        Account senderAccount = accountService.findActiveAccountById(request.getSenderAccountId());
        Account receiverAccount = accountService.findActiveAccountById(request.getRegularTransferDto().receiverAccountId());

        accountService.checkAccountsBeforeMoneyTransfer(senderAccount, receiverAccount);

        Account chargedAccount = accountService.getChargedAccount(request.getChargedAccountId(), List.of(senderAccount));

        return List.of(senderAccount, receiverAccount, chargedAccount);
    }

    private TransferOrder findById(Integer id) {
        String entity = Entity.TRANSFER_ORDER.getValue();
        TransferOrder transferOrder = transferOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity)));

        log.info(LogMessage.RESOURCE_FOUND, entity);

        return transferOrder;
    }
}
