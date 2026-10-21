package com.ercanbeyen.bankingapplication.helper;

import com.ercanbeyen.bankingapplication.constant.enums.AccountType;
import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.dto.TermDepositInterestRateDto;
import com.ercanbeyen.bankingapplication.dto.option.TermDepositInterestRateFilteringOption;
import com.ercanbeyen.bankingapplication.entity.Account;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.service.TermDepositInterestRateService;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
@Slf4j
@RequiredArgsConstructor
public class DepositAccountHelper {
    private final TermDepositInterestRateService termDepositInterestRateService;

    public void updateDepositAccountFields(Account account, int depositMaturity, double transactionFee) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        double currentNetBalance = account.getBalance();
        double interestRate = getInterestRate(account.getCurrency(), currentNetBalance, depositMaturity);
        double interestIncome = calculateInterestIncome(currentNetBalance, depositMaturity, interestRate);
        double balanceAfterNextInterestIncome = currentNetBalance + interestIncome - transactionFee;

        account.setInterestRate(interestRate);
        account.setDepositMaturity(depositMaturity);
        account.setDepositMaturityDate(LocalDate.now(ZoneId.systemDefault()).plusMonths(depositMaturity));
        account.setBalanceAfterNextInterestIncome(balanceAfterNextInterestIncome);

        log.info("{} {} fields are updated", AccountType.DEPOSIT.getValue(), Entity.ACCOUNT.getValue());
    }

    public static double calculateInterestIncome(Double balance, Integer depositMaturity, Double interestRate) {
        if (balance < 0 || interestRate < 0) {
            throw new BadRequestException(String.format("Balance and interest rate must be greater than or equal to %s", 0));
        }

        double interestIncome = (balance / 100) * (interestRate / 12) * depositMaturity;
        log.info("Interest after calculation with balance ({}), interest rate ({}) and deposit maturity ({}): {}", balance, interestRate, depositMaturity, interestIncome);

        return interestIncome;
    }

    private double getInterestRate(Currency currency, double balance, int depositMaturity) {
        double interestRate = 0;

        /* Match interest rate for the given currency, balance and deposit maturity */
        try {
            interestRate = termDepositInterestRateService.getInterestRate(currency, depositMaturity, balance);
        } catch (ResourceNotFoundException _) {
            TermDepositInterestRateFilteringOption filteringOption = new TermDepositInterestRateFilteringOption();
            filteringOption.setCurrency(currency);
            filteringOption.setDepositMaturity(depositMaturity);

            String entity = Entity.TERM_DEPOSIT_INTEREST_RATE.getValue();
            String exceptionMessage = "No %s balance in the " + entity.toLowerCase();

            /* Less than minimum balance of Term Deposit Interest Rate */
            double minimumBalance = termDepositInterestRateService.getEntities(filteringOption)
                    .stream()
                    .mapToDouble(TermDepositInterestRateDto::getMinimumBalance)
                    .min()
                    .orElseThrow(() -> new ResourceNotFoundException(String.format(exceptionMessage, "minimum")));

            if (balance < minimumBalance) {
                log.info("Balance is less than the minimum {} balance for deposit maturity {}. Therefore, interest rate is {}", entity.toLowerCase(), depositMaturity, interestRate);
                return interestRate;
            }

            /* Greater than maximum balance of Term Deposit Interest Rate */
            double maximumBalance = termDepositInterestRateService.getEntities(filteringOption)
                    .stream()
                    .mapToDouble(TermDepositInterestRateDto::getMaximumBalance)
                    .max()
                    .orElseThrow(() -> new ResourceNotFoundException(String.format(exceptionMessage, "maximum")));

            if (balance > maximumBalance) {
                log.info("Balance is greater than or equal to the maximum balance for deposit maturity {}. Therefore, interest rate of the maximum {} balance", depositMaturity, entity.toLowerCase());
                return termDepositInterestRateService.getInterestRate(currency, depositMaturity, maximumBalance);
            }

            /* Unmatched Term Deposit Interest Rate Balance Case */
            log.error("Unexpected condition! There are unmatched {} balances. Balance: {} & Deposit Maturity: {}", entity.toLowerCase(), balance, depositMaturity);
            throw new ResourceNotFoundException(entity + " balance is not found for " + balance);
        }

        return interestRate;
    }
}
