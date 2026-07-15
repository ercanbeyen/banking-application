package com.ercanbeyen.bankingapplication.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EmailUtil {
    private final String FOOTER = "<br><br><footer>Have a nice day,<br>Online Bank</footer>";

    public String constructContent(String fullName, String body) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<p>Hello");

        if (fullName != null) {
            stringBuilder.append(" ");
            stringBuilder.append(fullName);
        }

        stringBuilder.append(",<br><br>");
        stringBuilder.append(body);
        stringBuilder.append("</p>");
        stringBuilder.append(FOOTER);

        return stringBuilder.toString();
    }
}
