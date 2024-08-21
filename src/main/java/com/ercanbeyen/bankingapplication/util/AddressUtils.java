package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.constant.enums.AddressType;
import com.ercanbeyen.bankingapplication.exception.ResourceConflictException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AddressUtils {
    public void checkAddressType(AddressType addressType, String companyName) {
        boolean companyNameExists = companyName == null || companyName.isBlank();

        if (addressType == AddressType.WORK && companyNameExists) {
            throw new ResourceConflictException("Work address must have company name");
        }

        if (addressType == AddressType.HOUSE && !companyNameExists) {
            throw new ResourceConflictException("House address should not have company name");
        }
    }
}
