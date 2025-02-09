package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ContractUtil {
    public final String SPLITTER = "_";

    public String generateContractSubject(Entity entity) {
        return entity.getValue() + SPLITTER + Entity.CONTRACT.getValue();
    }

    public String generateContractSubject(String prefix, Entity entity) {
        return prefix + SPLITTER + generateContractSubject(entity);
    }
}
