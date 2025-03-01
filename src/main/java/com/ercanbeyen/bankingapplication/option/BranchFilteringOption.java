package com.ercanbeyen.bankingapplication.option;

import com.ercanbeyen.bankingapplication.constant.enums.City;
import lombok.Data;

@Data
public final class BranchFilteringOption extends BaseFilteringOption {
    private City city;
    private String district;
}
