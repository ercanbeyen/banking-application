package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.constant.enums.AddressType;
import com.ercanbeyen.bankingapplication.constant.enums.Ownership;
import com.ercanbeyen.bankingapplication.dto.AddressDto;
import com.ercanbeyen.bankingapplication.dto.request.CreateAddressRequest;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class AddressUtils {
    public void checkAddressCreateRequest(CreateAddressRequest request) {
        checkCompanyName(request.type(), request.companyName());
        checkOwnership(request.type(), request.ownership());
    }

    public void checkAddressUpdateRequest(AddressDto request) {
        checkCompanyName(request.type(), request.companyName());
        checkOwnership(request.type(), request.ownership());
    }

    private static void checkCompanyName(AddressType type, String companyName) {
        boolean companyNameExists = companyName != null && !companyName.isBlank();

        if (type == AddressType.WORK && !companyNameExists) {
            throw new ResourceConflictException(String.format("%s address must have a company name", type));
        }

        if (type == AddressType.HOUSE && companyNameExists) {
            throw new ResourceConflictException(String.format("%s address should not have a company name", type));
        }
    }

    private static void checkOwnership(AddressType addressType, Ownership ownership) {
        List<Ownership> validOwnerships = addressType == AddressType.WORK ? List.of(Ownership.OWNER, Ownership.EMPLOYEE)
                : List.of(Ownership.FAMILY_PROPERTY, Ownership.RENT, Ownership.PUBLIC_HOUSING);

        if (!validOwnerships.contains(ownership)) {
            throw new ResourceConflictException(String.format("%s address must have %s ownerships", addressType, validOwnerships));
        }
    }
}
