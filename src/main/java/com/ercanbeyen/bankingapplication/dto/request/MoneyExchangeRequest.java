package com.ercanbeyen.bankingapplication.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MoneyExchangeRequest(
        @NotNull(message = "Seller account id should not be null") Integer sellerAccountId,
        @NotNull(message = "Buyer account id should not be null") Integer buyerAccountId,
        Integer chargedAccountId,
        @Min(value = 1, message = "Minimum amount is {value}") Double amount) {

}
