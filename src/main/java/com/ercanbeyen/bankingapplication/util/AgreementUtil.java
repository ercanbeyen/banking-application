package com.ercanbeyen.bankingapplication.util;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AgreementUtil {
    private final String SPLITTER = "_";

    public String generateSubject(String prefix, Entity entity) {
        return prefix + SPLITTER + generateSubject(entity);
    }

    public String generateSubject(Entity entity) {
        return entity.getValue();
    }
}
