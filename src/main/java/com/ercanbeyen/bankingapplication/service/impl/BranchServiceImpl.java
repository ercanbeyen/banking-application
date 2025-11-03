package com.ercanbeyen.bankingapplication.service.impl;

import com.ercanbeyen.bankingapplication.constant.enums.AddressType;
import com.ercanbeyen.bankingapplication.constant.enums.City;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.constant.message.ResponseMessage;
import com.ercanbeyen.bankingapplication.dto.BranchDto;
import com.ercanbeyen.bankingapplication.embeddable.Address;
import com.ercanbeyen.bankingapplication.entity.Branch;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import com.ercanbeyen.bankingapplication.exception.ResourceNotFoundException;
import com.ercanbeyen.bankingapplication.mapper.BranchMapper;
import com.ercanbeyen.bankingapplication.option.BranchFilteringOption;
import com.ercanbeyen.bankingapplication.repository.BranchRepository;
import com.ercanbeyen.bankingapplication.service.BranchService;
import com.ercanbeyen.bankingapplication.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Service
@Slf4j
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {
    private final BranchRepository branchRepository;
    private final BranchMapper branchMapper;

    @Override
    public List<BranchDto> getEntities(BranchFilteringOption filteringOption) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Predicate<Branch> branchPredicate = branch -> {
            City cityOption = filteringOption.getCity();
            String districtOption = filteringOption.getDistrict();
            LocalDate createdAtOption = filteringOption.getCreatedAt();
            Address address = branch.getAddress();

            boolean cityFilter = (Optional.ofNullable(cityOption).isEmpty() || address.getCity() == cityOption);
            boolean districtFilter = (Optional.ofNullable(districtOption).isEmpty() || address.getDistrict().equals(districtOption));
            boolean createdAtFilter = (Optional.ofNullable(createdAtOption).isEmpty() || branch.getCreatedAt().toLocalDate().isEqual(createdAtOption));

            return cityFilter && districtFilter && createdAtFilter;
        };

        return branchRepository.findAll()
                .stream()
                .filter(branchPredicate)
                .map(branchMapper::entityToDto)
                .toList();
    }

    @Override
    public BranchDto getEntity(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());
        return branchMapper.entityToDto(findById(id));
    }

    @Override
    public BranchDto createEntity(BranchDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        checkUniqueness(request, null);

        Branch branch = branchMapper.dtoToEntity(request);
        branch.getAddress().setType(AddressType.WORK);
        Branch savedBranch = branchRepository.save(branch);

        log.info(LogMessage.RESOURCE_CREATE_SUCCESS, Entity.BRANCH.getValue(), savedBranch.getId());

        return branchMapper.entityToDto(savedBranch);
    }

    @Override
    public BranchDto updateEntity(Integer id, BranchDto request) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        Branch branch = findById(id);
        checkUniqueness(request, branch.getName());

        branch.setName(request.getName());
        branch.setAddress(request.getAddress());
        branch.getAddress().setType(AddressType.WORK);

        return branchMapper.entityToDto(branchRepository.save(branch));
    }

    @Override
    public void deleteEntity(Integer id) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        String entity = Entity.BRANCH.getValue();
        Branch branch = findById(id);

        if (!branch.getAccounts().isEmpty()) {
            throw new ResourceConflictException("Some accounts are linked to this branch. To delete this branch, please first unlink the relevant accounts from this branch.");
        }

        branchRepository.delete(branch);

        log.info(LogMessage.RESOURCE_DELETE_SUCCESS, entity, id);
    }

    @Override
    public Branch findByName(String name) {
        log.info(LogMessage.ECHO, LoggingUtil.getCurrentClassName(), LoggingUtil.getCurrentMethodName());

        String entity = Entity.BRANCH.getValue();
        Branch branch = branchRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity)));

        log.info(LogMessage.RESOURCE_FOUND, entity);

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
            log.error(LogMessage.RESOURCE_NOT_UNIQUE, entity);
            isUnique = false;
        } catch (Exception exception) {
            log.info(LogMessage.RESOURCE_UNIQUE, entity);
            isUnique = true;
        }

        if (!isUnique) {
            throw new ResourceConflictException(String.format(ResponseMessage.ALREADY_EXISTS, entity));
        }
    }

    private Branch findById(Integer id) {
        String entity = Entity.BRANCH.getValue();
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ResponseMessage.NOT_FOUND, entity)));

        log.info(LogMessage.RESOURCE_FOUND, entity);

        return branch;
    }
}
