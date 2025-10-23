package com.ercanbeyen.bankingapplication.dto;

import com.ercanbeyen.bankingapplication.constant.enums.Currency;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Range;

@EqualsAndHashCode(callSuper = true)
@Data
public non-sealed class FeeDto extends BaseDto {
    @NotNull(message = "Currency should not be null")
    private Currency currency;
    @NotNull(message = "Minimum amount should not be null")
    @Min(value = 0, message = "Minimum amount should be at least {value}")
    private Double minimumAmount;
    @NotNull(message = "Minimum amount should not be null")
    @Min(value = 0, message = "Maximum amount should be at least {value}")
    private Double maximumAmount;
    @Range(min = 0, max = 100, message = "Interest ratio is not between {min} and {max}")
    private Double interestRatio;
    private Integer depositPeriod;
}
