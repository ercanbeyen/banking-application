package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessages;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessages;
import com.ercanbeyen.bankingapplication.dto.BranchDto;
import com.ercanbeyen.bankingapplication.entity.Branch;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.BranchMapper;
import com.ercanbeyen.bankingapplication.option.BranchFilteringOptions;
import com.ercanbeyen.bankingapplication.repository.BranchRepository;
import com.ercanbeyen.bankingapplication.service.BaseService;
import com.ercanbeyen.bankingapplication.util.LoggingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BranchService implements BaseService<BranchDto, BranchFilteringOptions> {
    private final BranchRepository branchRepository;
    private final BranchMapper branchMapper;

    @Override
    public List<BranchDto> getEntities(BranchFilteringOptions options) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        List<BranchDto> branchDtos = new ArrayList<>();

        branchRepository.findAll()
                .forEach(branch -> branchDtos.add(branchMapper.entityToDto(branch)));

        return branchDtos;
    }

    @Override
    public BranchDto getEntity(Integer id) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());
        return branchMapper.entityToDto(findById(id));
    }

    @Override
    public BranchDto createEntity(BranchDto request) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Branch branch = branchMapper.dtoToEntity(request);
        Branch savedBranch = branchRepository.save(branch);

        log.info(LogMessages.RESOURCE_CREATE_SUCCESS, Entity.BRANCH.getValue(), savedBranch.getId());

        return branchMapper.entityToDto(savedBranch);
    }

    @Override
    public BranchDto updateEntity(Integer id, BranchDto request) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Branch branch = findById(id);

        branch.setCity(request.getCity());
        branch.setDistrict(request.getDistrict());
        branch.setName(request.getName());

        return branchMapper.entityToDto(branchRepository.save(branch));
    }

    @Override
    public void deleteEntity(Integer id) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        String entity = Entity.BRANCH.getValue();

        branchRepository.findById(id)
                .ifPresentOrElse(branch -> branchRepository.deleteById(id), () -> {
                            log.error(LogMessages.RESOURCE_NOT_FOUND, entity);
                            throw new ResourceNotFoundException(String.format(ResponseMessages.NOT_FOUND, entity));
                });

        log.info(LogMessages.RESOURCE_DELETE_SUCCESS, entity, id);
    }

    public Branch findById(Integer id) {
        String entity = Entity.BRANCH.getValue();
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessages.NOT_FOUND, entity)));

        log.info(LogMessages.RESOURCE_FOUND, entity);

        return branch;
    }
}
