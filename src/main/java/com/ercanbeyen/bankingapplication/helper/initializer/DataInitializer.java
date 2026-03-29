package com.ercanbeyen.bankingapplication.helper.initializer;

import com.ercanbeyen.bankingapplication.constant.enums.ERole;
import com.ercanbeyen.bankingapplication.dto.request.CreateRoleRequest;
import com.ercanbeyen.bankingapplication.model.Permission;
import com.ercanbeyen.bankingapplication.service.PermissionService;
import com.ercanbeyen.bankingapplication.service.RoleService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {
    private final RoleService roleService;
    private final PermissionService permissionService;

    @PostConstruct
    public void seedRolesAndPermissions() {
        /* Avoid duplicate seeding */
        if (roleService.existsByName(ERole.ADMIN)) {
            log.warn("Roles and permissions have already been created!");
            return;
        }

        Permission manageEntityPermission = permissionService.createPermission("MANAGE_ENTITY");
        Permission readUserPermission = permissionService.createPermission("READ_DATA");

        roleService.createRole(new CreateRoleRequest(ERole.ADMIN, Set.of(manageEntityPermission, readUserPermission)));
        roleService.createRole(new CreateRoleRequest(ERole.TELLER, Set.of(readUserPermission)));
        roleService.createRole(new CreateRoleRequest(ERole.USER, Set.of()));
    }
}
