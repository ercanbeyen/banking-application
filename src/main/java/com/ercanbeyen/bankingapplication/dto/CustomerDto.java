package com.ercanbeyen.bankingapplication.dto;

import com.ercanbeyen.bankingapplication.annotation.PhoneNumberRequest;
import com.ercanbeyen.bankingapplication.constant.enums.Gender;
import com.ercanbeyen.bankingapplication.embeddable.Address;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public non-sealed class CustomerDto extends BaseDto {
    @NotBlank(message = "Name should not be blank")
    @Size(min = 2, max = 100, message = "Length of name is not between {min} and {max}")
    String name;
    @NotBlank(message = "Surname should not be blank")
    @Size(min = 2, max = 100, message = "Length of surname is not between {min} and {max}")
    String surname;
    @NotBlank(message = "National identity should not be blank")
    @Pattern(regexp = "\\d{11}", message = "Length of national identity must be 11 characters")
    private String nationalId;
    @PhoneNumberRequest
    private String phoneNumber;
    @NotBlank(message = "Email should not be blank")
    @Email(message = "Invalid email")
    private String email;
    @NotNull(message = "Gender should not be null")
    private Gender gender;
    @NotNull(message = "Birth date should not be null")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;
    private List<@Valid Address> addresses;
}
