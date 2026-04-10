package com.ercanbeyen.bankingapplication.dto.request;

import com.ercanbeyen.bankingapplication.constant.enums.ERole;
import com.ercanbeyen.bankingapplication.entity.Permission;

import java.util.Set;

public record CreateRoleRequest(ERole name, Set<Permission> permissions) {

}
