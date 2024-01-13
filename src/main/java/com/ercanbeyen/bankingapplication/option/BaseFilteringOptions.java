package com.ercanbeyen.bankingapplication.option;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public sealed class BaseFilteringOptions permits CustomerFilteringOptions, AddressFilteringOptions, AccountFilteringOptions {
    private LocalDateTime createTime;
}
