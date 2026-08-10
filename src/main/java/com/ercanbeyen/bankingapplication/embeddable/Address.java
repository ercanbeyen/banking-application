package com.ercanbeyen.bankingapplication.embeddable;


import com.ercanbeyen.bankingapplication.annotation.PhoneNumberRequest;
import com.ercanbeyen.bankingapplication.constant.enums.AddressType;
import com.ercanbeyen.bankingapplication.constant.enums.Ownership;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

@Data
@Embeddable
public class Address {
    @NotBlank(message = "Country cannot be blank")
    private String country;
    @NotBlank(message = "City should not be blank")
    private String city;
    private String district;
    @NotNull(message = "Address type should not be null")
    @Enumerated(EnumType.STRING)
    private AddressType type;
    @NotNull(message = "Zip code should not be null")
    @Range(min = 10_000, max = 99_999, message = "Zip code is not between {min} and {max}")
    private Integer zipCode;
    @NotBlank(message = "Details should not be blank")
    @Size(min = 5, max = 500, message = "Length of details is not between {min} and {max}")
    private String details;
    @PhoneNumberRequest
    private String phoneNumber;
    @NotNull(message = "Ownership should not be null")
    @Enumerated(EnumType.STRING)
    private Ownership ownership;
    private String companyName;
}
