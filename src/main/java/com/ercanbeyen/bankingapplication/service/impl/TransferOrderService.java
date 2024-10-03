package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessages;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessages;
import com.ercanbeyen.bankingapplication.dto.RegularTransferDto;
import com.ercanbeyen.bankingapplication.dto.TransferOrderDto;
import com.ercanbeyen.bankingapplication.embeddable.RegularTransfer;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.entity.TransferOrder;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.TransferOrderMapper;
import com.ercanbeyen.bankingapplication.option.TransferOrderOptions;
import com.ercanbeyen.bankingapplication.repository.TransferOrderRepository;
import com.ercanbeyen.bankingapplication.service.BaseService;
import com.ercanbeyen.bankingapplication.util.LoggingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferOrderService implements BaseService<TransferOrderDto, TransferOrderOptions> {
    private final TransferOrderRepository transferOrderRepository;
    private final TransferOrderMapper transferOrderMapper;
    private final AccountService accountService;

    @Override
    public List<TransferOrderDto> getEntities(TransferOrderOptions options) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Predicate<TransferOrder> transferOrderPredicate = transferOrder -> (options.getSenderAccountId() == null || options.getSenderAccountId().equals(transferOrder.getSenderAccount().getId()))
                && (options.getReceiverAccountId() == null || options.getReceiverAccountId().equals(transferOrder.getRegularTransfer().getReceiverAccount().getId())
                && (options.getCreatedAt() == null || options.getCreatedAt().toLocalDate().isEqual(options.getCreatedAt().toLocalDate())));

        return transferOrderRepository.findAll()
                .stream()
                .filter(transferOrderPredicate)
                .map(transferOrderMapper::entityToDto)
                .toList();
    }

    @Override
    public TransferOrderDto getEntity(Integer id) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());
        TransferOrder transferOrder = findById(id);
        return transferOrderMapper.entityToDto(transferOrder);
    }

    @Override
    public TransferOrderDto createEntity(TransferOrderDto request) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        TransferOrder transferOrder = createTransferOrder(request);
        TransferOrder savedTransferOrder = transferOrderRepository.save(transferOrder);
        log.info(LogMessages.RESOURCE_CREATE_SUCCESS, Entity.TRANSFER_ORDER.getValue(), savedTransferOrder.getId());

        return transferOrderMapper.entityToDto(savedTransferOrder);
    }

    @Override
    public TransferOrderDto updateEntity(Integer id, TransferOrderDto request) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        TransferOrder transferOrder = findById(id);

        List<Account> accounts = getAccountsFromRegularTransferDto(request);
        transferOrder.setSenderAccount(accounts.getFirst());

        RegularTransfer regularTransfer = transferOrder.getRegularTransfer();
        regularTransfer.setReceiverAccount(accounts.get(1));
        regularTransfer.setAmount(request.getRegularTransferDto().amount());
        regularTransfer.setExplanation(request.getRegularTransferDto().explanation());

        transferOrder.setTransferDate(transferOrder.getTransferDate());

        return transferOrderMapper.entityToDto(transferOrderRepository.save(transferOrder));
    }

    @Override
    public void deleteEntity(Integer id) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());
        TransferOrder transferOrder = findById(id);
        transferOrderRepository.delete(transferOrder);
        log.info(LogMessages.RESOURCE_DELETE_SUCCESS, Entity.TRANSFER_ORDER.getValue(), id);
    }

    private TransferOrder createTransferOrder(TransferOrderDto request) {
        List<Account> accounts = getAccountsFromRegularTransferDto(request);
        RegularTransfer regularTransfer = createRegularTransfer(request, accounts);
        TransferOrder transferOrder = new TransferOrder();

        transferOrder.setSenderAccount(accounts.getFirst());
        transferOrder.setRegularTransfer(regularTransfer);
        transferOrder.setTransferDate(request.getTransferDate());

        return transferOrder;
    }

    private static RegularTransfer createRegularTransfer(TransferOrderDto request, List<Account> accounts) {
        RegularTransferDto regularTransferDto = request.getRegularTransferDto();
        return new RegularTransfer(
                accounts.get(1),
                regularTransferDto.orderPeriod(),
                regularTransferDto.amount(),
                regularTransferDto.explanation()
        );
    }

    /***
     *
     * @param request TransferOrderDto object
     * @return list which contains sender and receiver accounts respectively
     */
    private List<Account> getAccountsFromRegularTransferDto(TransferOrderDto request) {
        Account senderAccount = accountService.findById(request.getSenderAccountId());
        log.info(LogMessages.RESOURCE_FOUND, Entity.ACCOUNT.getValue());

        Account receiverAccount = accountService.findById(request.getRegularTransferDto().receiverAccountId());
        log.info(LogMessages.RESOURCE_FOUND, Entity.ACCOUNT.getValue());

        return List.of(senderAccount, receiverAccount);
    }

    private TransferOrder findById(Integer id) {
        String entity = Entity.TRANSFER_ORDER.getValue();
        TransferOrder transferOrder = transferOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessages.NOT_FOUND, entity)));

        log.info(LogMessages.RESOURCE_FOUND, entity);

        return transferOrder;
    }
}
