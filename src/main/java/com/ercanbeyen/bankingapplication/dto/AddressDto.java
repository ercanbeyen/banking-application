package com.ercanbeyen.bankingapplication.dto;

import com.ercanbeyen.bankingapplication.constant.enums.AddressType;
import com.ercanbeyen.bankingapplication.constant.enums.City;
import com.ercanbeyen.bankingapplication.constant.enums.Ownership;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.Range;

import java.util.Set;

public record AddressDto(
        String id,
        Set<String> customerNationalIds,
        @NotNull(message = "City should not be null")
        City city,
        @NotNull(message = "Address type should not be null")
        AddressType type,
        @NotNull(message = "Zip code should not be null")
        @Range(min = 10_000, max = 99_999, message = "Zip code is not between {min} and {max}")
        Integer zipCode,
        @NotBlank(message = "Details should not be blank")
        @Size(min = 5, max = 500, message = "Length of details is not between {min} and {max}")
        String details,
        String phoneNumber,
        @NotNull(message = "Ownership should not be null")
        Ownership ownership,
        String companyName) {

}
