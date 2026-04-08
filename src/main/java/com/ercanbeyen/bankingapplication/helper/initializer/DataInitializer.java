package com.ercanbeyen.bankingapplication.helper.initializer;

import com.ercanbeyen.bankingapplication.constant.enums.*;
import com.ercanbeyen.bankingapplication.dto.UserCredentialDto;
import com.ercanbeyen.bankingapplication.dto.request.CreateRoleRequest;
import com.ercanbeyen.bankingapplication.model.Permission;
import com.ercanbeyen.bankingapplication.service.PermissionService;
import com.ercanbeyen.bankingapplication.service.RoleService;
import com.ercanbeyen.bankingapplication.service.UserCredentialService;
import com.ercanbeyen.bankingapplication.util.UserCredentialUtil;
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
    private final UserCredentialService userCredentialService;

    @PostConstruct
    public void initialize() {
        /* Avoid repeated creation processes */
        if (roleService.existsByName(ERole.ADMIN)) {
            log.warn("Roles and permissions have already been created!");
            return;
        }

        createRolesAndPermissions();
        createSystemAdmin();
    }

    private void createSystemAdmin() {
        UserCredentialDto request = new UserCredentialDto(UserCredentialUtil.getSystemAdminUsername(), 0, "password", Set.of(ERole.ADMIN.toString()));
        userCredentialService.createUserCredential(request);
        log.warn("System Admin is created!");
    }

    public void createRolesAndPermissions() {
        Permission manageEntityPermission = permissionService.createPermission("MANAGE_ENTITY");
        Permission readUserPermission = permissionService.createPermission("READ_DATA");

        roleService.createRole(new CreateRoleRequest(ERole.ADMIN, Set.of(manageEntityPermission, readUserPermission)));
        roleService.createRole(new CreateRoleRequest(ERole.TELLER, Set.of(readUserPermission)));
        roleService.createRole(new CreateRoleRequest(ERole.USER, Set.of()));

        log.warn("Roles and permissions are created!");
    }
}
