package com.ercanbeyen.bankingapplication.dto.request;

import com.ercanbeyen.bankingapplication.constant.enums.AddressType;
import com.ercanbeyen.bankingapplication.constant.enums.City;
import com.ercanbeyen.bankingapplication.constant.enums.Ownership;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.Range;

public record CreateAddressRequest(
        @NotBlank(message = "National identity should not be blank")
        @Pattern(regexp = "\\d{11}", message = "Length of national identity is not 11")
        String customerNationalId,
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
