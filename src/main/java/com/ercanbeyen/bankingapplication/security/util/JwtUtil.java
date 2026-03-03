package com.ercanbeyen.bankingapplication.security.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class JwtUtil {
    public final String ACCESS_TOKEN = "access_token";
    public final String REFRESH_TOKEN_TOKEN = "refresh_token";
    public final String AUTHORIZATION_HEADER = "Authorization";
    public final String AUTHORIZATION_HEADER_STARTS_WITH = "Bearer ";
    public final int TOKEN_BEGIN_INDEX = 7;
}
