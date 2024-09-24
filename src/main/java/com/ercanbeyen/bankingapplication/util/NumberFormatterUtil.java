package com.ercanbeyen.bankingapplication.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class NumberFormatterUtil {
    public String convertNumberToFormalExpression(double number) {
        return String.format("%1$,.2f", number);
    }
}
