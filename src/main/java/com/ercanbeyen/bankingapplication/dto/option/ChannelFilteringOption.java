package com.ercanbeyen.bankingapplication.dto.option;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public abstract sealed class ChannelFilteringOption extends BaseFilteringOption permits AtmFilteringOption, BranchFilteringOption {
    private String city;
    private String district;
}
