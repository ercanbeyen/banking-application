package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.AddressType;
import com.ercanbeyen.bankingapplication.constant.enums.City;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessages;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessages;
import com.ercanbeyen.bankingapplication.dto.BranchDto;
import com.ercanbeyen.bankingapplication.embeddable.Address;
import com.ercanbeyen.bankingapplication.entity.Branch;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.BranchMapper;
import com.ercanbeyen.bankingapplication.option.BranchFilteringOptions;
import com.ercanbeyen.bankingapplication.repository.BranchRepository;
import com.ercanbeyen.bankingapplication.service.BaseService;
import com.ercanbeyen.bankingapplication.util.LoggingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Service
@Slf4j
@RequiredArgsConstructor
public class BranchService implements BaseService<BranchDto, BranchFilteringOptions> {
    private final BranchRepository branchRepository;
    private final BranchMapper branchMapper;

    @Override
    public List<BranchDto> getEntities(BranchFilteringOptions options) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Predicate<Branch> branchPredicate = branch -> {
            City optionsCity = options.getCity();
            String optionsDistrict = options.getDistrict();
            LocalDateTime optionsCreatedAt = options.getCreatedAt();
            Address address = branch.getAddress();

            boolean cityCheck = (Optional.ofNullable(optionsCity).isEmpty() || address.getCity() == optionsCity);
            boolean districtCheck = (Optional.ofNullable(optionsDistrict).isEmpty()|| address.getDistrict().equals(optionsDistrict));
            boolean createdAtCheck = (Optional.ofNullable(optionsCreatedAt).isEmpty() || branch.getCreatedAt().isEqual(optionsCreatedAt));

            return cityCheck && districtCheck && createdAtCheck;
        };

        return branchRepository.findAll()
                .stream()
                .filter(branchPredicate)
                .map(branchMapper::entityToDto)
                .toList();
    }

    @Override
    public BranchDto getEntity(Integer id) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());
        return branchMapper.entityToDto(findById(id));
    }

    @Override
    public BranchDto createEntity(BranchDto request) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        checkUniqueness(request, null);

        Branch branch = branchMapper.dtoToEntity(request);
        branch.getAddress().setType(AddressType.WORK);
        Branch savedBranch = branchRepository.save(branch);

        log.info(LogMessages.RESOURCE_CREATE_SUCCESS, Entity.BRANCH.getValue(), savedBranch.getId());

        return branchMapper.entityToDto(savedBranch);
    }

    @Override
    public BranchDto updateEntity(Integer id, BranchDto request) {
        log.info(LogMessages.ECHO, LoggingUtils.getCurrentClassName(), LoggingUtils.getCurrentMethodName());

        Branch branch = findById(id);
        checkUniqueness(request, branch.getName());

        branch.setName(request.getName());
        branch.setAddress(request.getAddress());
        branch.getAddress().setType(AddressType.WORK);

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

    public Branch findByName(String name) {
        String entity = Entity.BRANCH.getValue();
        Branch branch = branchRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessages.NOT_FOUND, entity)));

        log.info(LogMessages.RESOURCE_FOUND, entity);

        return branch;
    }

    private void checkUniqueness(BranchDto request, String previousName) {
        if (Optional.ofNullable(previousName).isPresent() && previousName.equals(request.getName())) {
            log.info("Same name is requested for branch update");
            return;
        }

        boolean isUnique;
        String entity = Entity.BRANCH.getValue();

        try {
            findByName(request.getName());
            log.error(LogMessages.RESOURCE_NOT_UNIQUE, entity);
            isUnique = false;
        } catch (Exception exception) {
            log.info(LogMessages.RESOURCE_UNIQUE, entity);
            isUnique = true;
        }

        if (!isUnique) {
            throw new ResourceConflictException(String.format(ResponseMessages.ALREADY_EXISTS, entity));
        }
    }

    private Branch findById(Integer id) {
        String entity = Entity.BRANCH.getValue();
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessages.NOT_FOUND, entity)));

        log.info(LogMessages.RESOURCE_FOUND, entity);

        return branch;
    }
}
