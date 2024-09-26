package com.ercanbeyen.bankingapplication.option;

import com.ercanbeyen.bankingapplication.constant.enums.City;
import lombok.Data;

@Data
public final class BranchFilteringOptions extends BaseFilteringOptions {
    private City city;
    private String district;
}
