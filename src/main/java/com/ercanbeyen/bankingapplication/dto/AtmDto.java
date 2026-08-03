package com.ercanbeyen.bankingapplication.dto;

import com.ercanbeyen.bankingapplication.embeddable.Address;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public non-sealed class AtmDto extends BaseDto {
    @NotNull(message = "Address should not be null")
    @Valid
    private Address address;
}
