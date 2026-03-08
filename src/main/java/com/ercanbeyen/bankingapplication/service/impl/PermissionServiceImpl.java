package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.model.Permission;
import com.ercanbeyen.bankingapplication.repository.PermissionRepository;
import com.ercanbeyen.bankingapplication.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {
    private final PermissionRepository permissionRepository;

    @Override
    public Permission createPermission(String name) {
        Permission permission = new Permission(name);
        return permissionRepository.save(permission);
    }
}
