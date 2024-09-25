package com.ercanbeyen.bankingapplication.dto;

import com.ercanbeyen.bankingapplication.constant.enums.City;
import lombok.Data;

import java.util.List;

@Data
public final class BranchDto extends BaseDto {
    private City city;
    private String district;
    private String name;
    private List<Integer> accountIds;
}
