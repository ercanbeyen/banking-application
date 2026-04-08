package com.ercanbeyen.bankingapplication.scheduler;

import com.ercanbeyen.bankingapplication.constant.enums.AccountType;
import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.dto.AccountDto;
import com.ercanbeyen.bankingapplication.dto.response.MessageResponse;
import com.ercanbeyen.bankingapplication.security.service.JwtService;
import com.ercanbeyen.bankingapplication.security.util.JwtUtil;
import com.ercanbeyen.bankingapplication.util.UserCredentialUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@Async
@EnableScheduling
@Slf4j
@RequiredArgsConstructor
public class AccountScheduledTask {
    private static final String ID = "id";
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Scheduled(cron = "0 0 9 * * *") // 9:00 everyday
    public void payInterestOnDepositAccounts() {
        final String task = "periodic money deposit to deposit account";
        log.info(LogMessage.SCHEDULED_TASK_STARTED, task);

        UserDetails userDetails = userDetailsService.loadUserByUsername(UserCredentialUtil.getSystemAdminUsername());
        Map<String, String> tokens = jwtService.generateTokens(userDetails);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, JwtUtil.generateAuthorizationHeaderValue(tokens.get(JwtUtil.Header.ACCESS_TOKEN_HEADER)));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        UriComponents uriComponents = UriComponentsBuilder.fromUriString(Entity.ACCOUNT.getCollectionUrl())
                .queryParam("type", String.valueOf(AccountType.DEPOSIT))
                .build();

        List<AccountDto> accountDtos;

        try {
            accountDtos = getAccounts(uriComponents, headers);
        } catch (Exception exception) {
            log.error(LogMessage.EXCEPTION, exception.getMessage());
            return;
        }

        payInterests(accountDtos, headers);

        log.info(LogMessage.SCHEDULED_TASK_ENDED, task);
    }

    private void payInterests(List<AccountDto> accountDtos, HttpHeaders headers) {
        accountDtos.stream()
                .map(AccountDto::getId)
                .toList()
                .forEach(accountId -> {
                    log.info(LogMessage.BEFORE_REQUEST);

                    try {
                        Map<String, Integer> parameters = Map.of(ID, accountId);
                        String url = Entity.ACCOUNT.getCollectionUrl() + "/pay/interest/{" + ID + "}";
                        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

                        ResponseEntity<MessageResponse> responseEntity = restTemplate.exchange(
                                url,
                                HttpMethod.PUT,
                                httpEntity,
                                MessageResponse.class,
                                parameters
                        );

                        log.info(LogMessage.REST_TEMPLATE_SUCCESS, responseEntity.getBody());
                    } catch (Exception exception) {
                        log.error(LogMessage.EXCEPTION, exception.getMessage());
                    }

                    log.info(LogMessage.AFTER_REQUEST);
                });
    }

    private List<AccountDto> getAccounts(UriComponents uriComponents, HttpHeaders headers) {
        String url = uriComponents.toString();
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<List> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                httpEntity,
                List.class,
                uriComponents
        );

        log.info(LogMessage.CLASS_OF_RESPONSE, response.getClass());

        List<AccountDto> accountDtos = objectMapper.convertValue(response.getBody(), new TypeReference<>() {});
        accountDtos.forEach(accountDto -> log.info(LogMessage.CLASS_OF_OBJECT, "AccountDto", accountDto.getClass()));

        log.info(LogMessage.REST_TEMPLATE_SUCCESS, accountDtos);

        return accountDtos;
    }
}
