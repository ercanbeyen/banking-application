package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.constant.enums.ERole;
import com.ercanbeyen.bankingapplication.entity.Role;

public interface RoleService {
    void createRole(ERole name);
    Role findByName(ERole name);
    boolean existsByName(ERole name);
}
