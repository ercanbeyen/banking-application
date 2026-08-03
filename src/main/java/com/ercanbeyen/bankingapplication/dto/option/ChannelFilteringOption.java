package com.ercanbeyen.bankingapplication.dto.option;

import com.ercanbeyen.bankingapplication.constant.enums.City;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public abstract sealed class ChannelFilteringOption extends BaseFilteringOption permits AtmFilteringOption, BranchFilteringOption {
    private City city;
    private String district;
}
