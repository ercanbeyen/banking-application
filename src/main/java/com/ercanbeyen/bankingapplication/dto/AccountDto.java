package com.ercanbeyen.bankingapplication.dto;

import com.ercanbeyen.bankingapplication.constant.enums.AccountType;
import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Range;

import java.time.Instant;

@EqualsAndHashCode(callSuper = true)
@Data
public non-sealed class AccountDto extends BaseDto {
    @NotBlank(message = "National identity should not be blank")
    @Pattern(regexp = "\\d{11}", message = "Length of national identity is not 11")
    private String customerNationalId;
    @NotNull(message = "Currency should not be null")
    private Currency currency;
    private Double balance;
    @NotNull(message = "Account type should not be null")
    private AccountType type;
    private Boolean isBlocked;
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "UTC"
    )
    private Instant closedAt;
    @NotNull(message = "Branch name should not be null")
    @NotBlank(message = "Branch name should not be blank")
    private String branchName;
    /* Deposit Account fields */
    @Range(min = 0, max = 100, message = "Interest rate is not between {min} and {max}")
    private Double interestRate;
    private Integer depositMaturity;
    private Double balanceAfterNextInterestIncome;
}
