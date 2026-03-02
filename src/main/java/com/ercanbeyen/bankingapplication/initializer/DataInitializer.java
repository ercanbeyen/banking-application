package com.ercanbeyen.bankingapplication.initializer;

import com.ercanbeyen.bankingapplication.constant.enums.ERole;
import com.ercanbeyen.bankingapplication.service.RoleService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DataInitializer {
    private final RoleService roleService;

    @PostConstruct
    public void seedRoles() {
        /* Avoid duplicate seeding */
        if (roleService.existsByName(ERole.ADMIN)) {
            log.warn("Roles have already been created!");
            return;
        }

        roleService.createRole(ERole.ADMIN);
        roleService.createRole(ERole.TELLER);
        roleService.createRole(ERole.USER);
    }
}
