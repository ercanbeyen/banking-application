package com.ercanbeyen.bankingapplication.dto;


import com.ercanbeyen.bankingapplication.constant.enums.AccountActivityType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
public class ChargeDto implements Serializable {
    private String id;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @NotNull(message = "Activity type should not be null")
    private AccountActivityType activityType;
    @NotNull(message = "Minimum amount should not be null")
    @Min(value = 0, message = "Amount should be at least {value}")
    private Double amount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime modifiedAt;
}
