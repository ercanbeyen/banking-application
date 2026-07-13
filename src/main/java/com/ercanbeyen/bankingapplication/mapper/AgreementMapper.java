package com.ercanbeyen.bankingapplication.mapper;

import com.ercanbeyen.bankingapplication.dto.AgreementDto;
import com.ercanbeyen.bankingapplication.entity.Agreement;
import com.ercanbeyen.bankingapplication.entity.File;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface AgreementMapper {
    @Mapping(target = "fileNames", source = "files", qualifiedByName = "entityToName")
    AgreementDto entityToDto(Agreement agreement);

    @Named("entityToName")
    static String entityToName(File file) {
        return file.getName();
    }
}
