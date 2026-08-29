package com.ercanbeyen.bankingapplication.dto;

import com.ercanbeyen.bankingapplication.embeddable.Address;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public non-sealed class ChannelDto extends BaseDto {
    @NotBlank(message = "Name should not be blank")
    private String name;
    @NotNull(message = "Address should not be null")
    @Valid
    private Address address;
}
