package com.ercanbeyen.bankingapplication.helper.initializer;

import com.ercanbeyen.bankingapplication.constant.enums.*;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.dto.CustomerDto;
import com.ercanbeyen.bankingapplication.dto.request.CreateRoleRequest;
import com.ercanbeyen.bankingapplication.dto.request.RegistrationRequest;
import com.ercanbeyen.bankingapplication.entity.Permission;
import com.ercanbeyen.bankingapplication.security.config.SystemAdminProperties;
import com.ercanbeyen.bankingapplication.service.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {
    private final SystemAdminProperties systemAdminProperties;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final AuthService authService;

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
        CustomerDto customerDto = new CustomerDto();
        customerDto.setName("Admin");
        customerDto.setSurname("User");
        customerDto.setNationalId(systemAdminProperties.getUsername());
        customerDto.setEmail("admin@bank.com");
        customerDto.setPhoneNumber("+905809452510");
        customerDto.setGender(Gender.MALE);
        customerDto.setBirthDate(LocalDate.of(1980, Month.NOVEMBER, 10));
        customerDto.setAddresses(new ArrayList<>());

        RegistrationRequest request = new RegistrationRequest(
                customerDto,
                systemAdminProperties.getPassword(),
                Set.of(ERole.ADMIN.toString())
        );

        authService.registerUser(request);
        log.warn(LogMessage.RESOURCE_CREATE_SUCCESS, Entity.CUSTOMER.getValue(), customerDto.getName());
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
