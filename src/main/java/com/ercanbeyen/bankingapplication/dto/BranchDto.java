package com.ercanbeyen.bankingapplication.dto;

import com.ercanbeyen.bankingapplication.embeddable.Address;
import lombok.Data;

import java.util.List;

@Data
public final class BranchDto extends BaseDto {
    private String name;
    private Address address;
    private List<Integer> accountIds;
}
