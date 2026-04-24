package com.ercanbeyen.bankingapplication.dto;

import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Range;

@EqualsAndHashCode(callSuper = true)
@Data
public non-sealed class TermDepositInterestRateDto extends BaseDto {
    @NotNull(message = "Currency should not be null")
    private Currency currency;
    @NotNull(message = "Minimum balance should not be null")
    @Min(value = 0, message = "Minimum balance should be at least {value}")
    private Double minimumBalance;
    @NotNull(message = "Maximum balance should not be null")
    @Min(value = 0, message = "Maximum balance should be at least {value}")
    private Double maximumBalance;
    private Integer depositMaturity;
    @Range(min = 0, max = 100, message = "Interest rate is not between {min} and {max}")
    private Double interestRate;
}
