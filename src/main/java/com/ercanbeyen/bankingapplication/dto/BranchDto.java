package com.ercanbeyen.bankingapplication.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class BranchDto extends ChannelDto {
    private List<Integer> accountIds;
}
