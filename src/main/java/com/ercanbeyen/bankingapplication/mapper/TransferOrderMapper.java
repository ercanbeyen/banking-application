package com.ercanbeyen.bankingapplication.mapper;

import com.ercanbeyen.bankingapplication.dto.TransferOrderDto;
import com.ercanbeyen.bankingapplication.entity.TransferOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransferOrderMapper {
    @Mapping(target = "regularTransferDto.receiverAccountId", source = "transferOrder.regularTransfer.receiverAccount.id")
    @Mapping(target = "regularTransferDto", source = "transferOrder.regularTransfer")
    @Mapping(target = "senderAccountId", source = "transferOrder.senderAccount.id")
    TransferOrderDto entityToDto(TransferOrder transferOrder);
}
