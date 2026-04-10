package com.ercanbeyen.bankingapplication.mapper;

import com.ercanbeyen.bankingapplication.dto.MoneyTransferOrderDto;
import com.ercanbeyen.bankingapplication.entity.MoneyTransferOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MoneyTransferOrderMapper {
    @Mapping(target = "regularMoneyTransferDto.recipientAccountId", source = "regularMoneyTransfer.recipientAccount.id")
    @Mapping(target = "regularMoneyTransferDto", source = "regularMoneyTransfer")
    @Mapping(target = "regularMoneyTransferDto.paymentPeriod", source = "regularMoneyTransfer.paymentPeriod")
    @Mapping(target = "senderAccountId", source = "senderAccount.id")
    MoneyTransferOrderDto entityToDto(MoneyTransferOrder moneyTransferOrder);
}
