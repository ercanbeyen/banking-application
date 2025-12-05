package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.constant.enums.AddressType;
import com.ercanbeyen.bankingapplication.constant.enums.Ownership;
import com.ercanbeyen.bankingapplication.embeddable.Address;
import com.ercanbeyen.bankingapplication.exception.BadRequestException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@UtilityClass
@Slf4j
public class AddressUtil {
    public void checkAddressRequest(Address request) {
        checkCompanyName(request.getType(), request.getCompanyName());
        checkOwnership(request.getType(), request.getOwnership());
        log.info("Address request is valid");
    }

    private void checkCompanyName(AddressType type, String companyName) {
        boolean companyNameExists = companyName != null && !companyName.isBlank();

        if (type == AddressType.WORK && !companyNameExists) {
            throw new BadRequestException(String.format("%s address must have a company name", type));
        }

        if (type == AddressType.HOUSE && companyNameExists) {
            throw new BadRequestException(String.format("%s address should not have a company name", type));
        }
    }

    private void checkOwnership(AddressType addressType, Ownership ownership) {
        List<Ownership> validOwnerships = addressType == AddressType.WORK
                ? List.of(Ownership.OWNER, Ownership.EMPLOYEE)
                : List.of(Ownership.FAMILY_PROPERTY, Ownership.RENT, Ownership.PUBLIC_HOUSING);

        if (!validOwnerships.contains(ownership)) {
            throw new BadRequestException(String.format("%s address must have %s ownerships", addressType, validOwnerships));
        }
    }
}
