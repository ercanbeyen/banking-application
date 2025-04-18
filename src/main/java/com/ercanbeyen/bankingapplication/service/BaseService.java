package com.ercanbeyen.bankingapplication.service;

import com.ercanbeyen.bankingapplication.dto.BaseDto;
import com.ercanbeyen.bankingapplication.option.BaseFilteringOption;

import java.util.List;

public interface BaseService<T extends BaseDto, V extends BaseFilteringOption> {
    List<T> getEntities(V filteringOption);
    T getEntity(Integer id);
    T createEntity(T request);
    T updateEntity(Integer id, T request);
    void deleteEntity(Integer id);
}
