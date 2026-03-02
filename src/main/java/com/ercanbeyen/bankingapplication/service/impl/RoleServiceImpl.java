package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.ERole;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.entity.Role;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.repository.RoleRepository;
import com.ercanbeyen.bankingapplication.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;

    @Override
    public void createRole(ERole name) {
        Role admin = new Role();
        admin.setName(name);
        roleRepository.save(admin);
    }

    @Override
    public Role findByName(ERole name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, Entity.ROLE.getValue())));
    }

    @Override
    public boolean existsByName(ERole name) {
        return roleRepository.existsByName(name);
    }
}
