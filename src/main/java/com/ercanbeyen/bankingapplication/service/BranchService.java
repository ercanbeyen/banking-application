package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.dto.BranchDto;
import com.ercanbeyen.bankingapplication.entity.Branch;
import com.ercanbeyen.bankingapplication.option.BranchFilteringOption;

public interface BranchService extends BaseService<BranchDto, BranchFilteringOption> {
    Branch findByName(String name);
}
