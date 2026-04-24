package com.ercanbeyen.bankingapplication.helper.initializer;

import com.ercanbeyen.bankingapplication.constant.enums.*;
import com.ercanbeyen.bankingapplication.dto.UserCredentialsDto;
import com.ercanbeyen.bankingapplication.dto.request.CreateRoleRequest;
import com.ercanbeyen.bankingapplication.entity.Permission;
import com.ercanbeyen.bankingapplication.security.config.SystemAdminProperties;
import com.ercanbeyen.bankingapplication.service.PermissionService;
import com.ercanbeyen.bankingapplication.service.RoleService;
import com.ercanbeyen.bankingapplication.service.UserCredentialsService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {
    private final SystemAdminProperties systemAdminProperties;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final UserCredentialsService userCredentialsService;

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
        UserCredentialsDto request = new UserCredentialsDto(
                systemAdminProperties.getUsername(),
                systemAdminProperties.getCustomerId(),
                systemAdminProperties.getPassword(),
                Set.of(ERole.ADMIN.toString())
        );
        userCredentialsService.createUserCredentials(request);
        log.warn("System Admin is created!");
    }

    public void createRolesAndPermissions() {
        Permission manageEntityPermission = permissionService.createPermission(EPermission.MANAGE_ENTITY.toString());
        Permission readUserPermission = permissionService.createPermission(EPermission.READ_DATA.toString());

        roleService.createRole(new CreateRoleRequest(ERole.ADMIN, Set.of(manageEntityPermission, readUserPermission)));
        roleService.createRole(new CreateRoleRequest(ERole.TELLER, Set.of(readUserPermission)));
        roleService.createRole(new CreateRoleRequest(ERole.USER, Set.of()));

        log.warn("Roles and permissions are created!");
    }
}
