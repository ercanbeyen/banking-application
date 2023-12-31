package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.UnidirectionalAccountOperation;
import com.ercanbeyen.bankingapplication.constant.message.LogMessages;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessages;
import com.ercanbeyen.bankingapplication.dto.AccountDto;
import com.ercanbeyen.bankingapplication.dto.request.MoneyTransferRequest;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.entity.Customer;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.AccountMapper;
import com.ercanbeyen.bankingapplication.repository.AccountRepository;
import com.ercanbeyen.bankingapplication.service.BaseService;
import com.ercanbeyen.bankingapplication.util.AccountUtils;
import com.ercanbeyen.bankingapplication.util.LoggingUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService implements BaseService<AccountDto> {
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final CustomerService customerService;

    @Override
    public List<AccountDto> getEntities() {
        log.info(LogMessages.ECHO,
                LoggingUtils.getClassName(this),
                LoggingUtils.getMethodName(new Object() {}.getClass().getEnclosingMethod())
        );

        List<AccountDto> accountDtoList = new ArrayList<>();

        accountRepository.findAll()
                .forEach(account -> accountDtoList.add(accountMapper.accountToDto(account)));

        return accountDtoList;
    }

    @Override
    public Optional<AccountDto> getEntity(Integer id) {
        log.info(LogMessages.ECHO,
                LoggingUtils.getClassName(this),
                LoggingUtils.getMethodName(new Object() {}.getClass().getEnclosingMethod())
        );

        Optional<Account> accountOptional = accountRepository.findById(id);

        return accountOptional.map(accountMapper::accountToDto);
    }

    @Override
    public AccountDto createEntity(AccountDto request) {
        log.info(LogMessages.ECHO,
                LoggingUtils.getClassName(this),
                LoggingUtils.getMethodName(new Object() {}.getClass().getEnclosingMethod())
        );

        AccountUtils.checkAccountConstruction(request);
        Account account = accountMapper.dtoToAccount(request);

        Customer customer = customerService.findCustomerByNationalId(request.getCustomerNationalId());
        log.info(LogMessages.RESOURCE_FOUND, LogMessages.ResourceNames.CUSTOMER);

        account.setCustomer(customer);

        return accountMapper.accountToDto(accountRepository.save(account));
    }

    @Override
    public AccountDto updateEntity(Integer id, AccountDto request) {
        log.info(LogMessages.ECHO,
                LoggingUtils.getClassName(this),
                LoggingUtils.getMethodName(new Object() {}.getClass().getEnclosingMethod())
        );

        Account account = findAccountById(id);
        log.info(LogMessages.RESOURCE_FOUND, LogMessages.ResourceNames.ACCOUNT);

        account.setBranchLocation(request.getBranchLocation());

        return accountMapper.accountToDto(accountRepository.save(account));
    }

    @Override
    public void deleteEntity(Integer id) {
        log.info(LogMessages.ECHO,
                LoggingUtils.getClassName(this),
                LoggingUtils.getMethodName(new Object() {}.getClass().getEnclosingMethod())
        );

        Account account = findAccountById(id);
        log.info(LogMessages.RESOURCE_FOUND, LogMessages.ResourceNames.ACCOUNT);

        accountRepository.delete(account);
    }

    public String applyUnidirectionalAccountOperation(Integer id, UnidirectionalAccountOperation operation, Double amount) {
        log.info(LogMessages.ECHO,
                LoggingUtils.getClassName(this),
                LoggingUtils.getMethodName(new Object() {}.getClass().getEnclosingMethod())
        );

        Account account = findAccountById(id);
        log.info(LogMessages.RESOURCE_FOUND, LogMessages.ResourceNames.ACCOUNT);

        return switch (operation) {
            case UnidirectionalAccountOperation.ADD -> addMoney(account, amount);
            case UnidirectionalAccountOperation.WITHDRAW -> withdrawMoney(account, amount);
        };
    }

    public String transferMoney(MoneyTransferRequest request) {
        log.info(LogMessages.ECHO,
                LoggingUtils.getClassName(this),
                LoggingUtils.getMethodName(new Object() {}.getClass().getEnclosingMethod())
        );

        Account senderAccount = findAccountById(request.senderId());
        log.info(LogMessages.RESOURCE_FOUND, LogMessages.ResourceNames.ACCOUNT);

        Account receiverAccount = findAccountById(request.receiverId());
        log.info(LogMessages.RESOURCE_FOUND, LogMessages.ResourceNames.ACCOUNT);

        AccountUtils.checkCurrenciesForMoneyTransfer(senderAccount, receiverAccount);

        Double amount = request.amount();

        /*
            Money transfer flow:
            1) Withdraw requested money amount from the sender account
            2) Add requested money amount to the receiver account
            3) Rollback the withdrawal of sender account, if any error occurred during money add to receiver account
         */

        String message = withdrawMoney(senderAccount, amount);
        log.info(LogMessages.TRANSACTION_MESSAGE, message);

        try {
            message = addMoney(receiverAccount, amount);
            log.info(LogMessages.TRANSACTION_MESSAGE, message);
        } catch (Exception exception) {
            log.error("Unable to transfer the money");
            message = addMoney(senderAccount, amount); // rollback
            log.info(LogMessages.TRANSACTION_MESSAGE, message);
            throw new RuntimeException("Money transfer operation is unsuccessfully completed");
        }

        message = amount + " " + senderAccount.getCurrency() + " is successfully transferred from account "
                + senderAccount.getId() + " to account " + receiverAccount.getId();

        return message;
    }

    @Transactional
    private String addMoney(Account account, Double amount) {
        log.info(LogMessages.ECHO,
                LoggingUtils.getClassName(this),
                LoggingUtils.getMethodName(new Object() {}.getClass().getEnclosingMethod())
        );

        Double previousBalance = account.getBalance();
        Double nextBalance = previousBalance + amount;
        log.info(LogMessages.BALANCE_UPDATE, previousBalance, nextBalance);

        account.setBalance(nextBalance);
        accountRepository.save(account);

        return AccountUtils.constructResponseMessageForUnidirectionalAccountOperations(UnidirectionalAccountOperation.ADD, amount, account);
    }

    @Transactional
    private String withdrawMoney(Account account, Double amount) {
        log.info(LogMessages.ECHO,
                LoggingUtils.getClassName(this),
                LoggingUtils.getMethodName(new Object() {}.getClass().getEnclosingMethod())
        );

        AccountUtils.checkBalance(account, amount);

        Double previousBalance = account.getBalance();
        Double nextBalance = previousBalance - amount;
        log.info(LogMessages.BALANCE_UPDATE, previousBalance, nextBalance);

        account.setBalance(nextBalance);
        accountRepository.save(account);

        return AccountUtils.constructResponseMessageForUnidirectionalAccountOperations(UnidirectionalAccountOperation.WITHDRAW, amount, account);
    }

    private Account findAccountById(Integer id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ResponseMessages.NOT_FOUND));
    }

}
