package com.ercanbeyen.bankingapplication.mapper;

import com.ercanbeyen.bankingapplication.dto.TransferOrderDto;
import com.ercanbeyen.bankingapplication.entity.TransferOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransferOrderMapper {
    @Mapping(target = "regularTransferDto.receiverAccountId", source = "regularTransfer.receiverAccount.id")
    @Mapping(target = "regularTransferDto", source = "regularTransfer")
    @Mapping(target = "regularTransferDto.paymentPeriod", source = "regularTransfer.paymentPeriod")
    @Mapping(target = "senderAccountId", source = "senderAccount.id")
    TransferOrderDto entityToDto(TransferOrder transferOrder);
}
