package com.ercanbeyen.bankingapplication.scheduler;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.dto.CustomerDto;
import com.ercanbeyen.bankingapplication.dto.NotificationDto;
import com.ercanbeyen.bankingapplication.security.config.SystemAdminProperties;
import com.ercanbeyen.bankingapplication.security.service.JwtService;
import com.ercanbeyen.bankingapplication.security.util.JwtUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@Async
@Slf4j
@RequiredArgsConstructor
public class CustomerScheduledTask {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SystemAdminProperties systemAdminProperties;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Scheduled(cron = "0 0 12 * * *") // 12:00 everyday
    public void celebrateCustomersBirthday() {
        final String task = "celebrate customers' birthday";
        log.info(LogMessage.SCHEDULED_TASK_STARTED, task);

        LocalDate birthday = LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault());
        UriComponents uriComponents = UriComponentsBuilder.fromUriString(Entity.CUSTOMER.getCollectionUrl())
                .queryParam("birthDate", birthday.toString())
                .build();

        String notificationMessage = "happy birthday";

        UserDetails userDetails = userDetailsService.loadUserByUsername(systemAdminProperties.getUsername());
        Map<String, String> tokens = jwtService.generateTokens(userDetails);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.AUTHORIZATION, JwtUtil.generateAuthorizationHeaderValue(tokens.get(JwtUtil.Header.ACCESS_TOKEN_HEADER)));
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        List<CustomerDto> customerDtos;

        try {
            customerDtos = getCustomers(uriComponents, httpHeaders);
        } catch (Exception exception) {
            log.error(LogMessage.EXCEPTION, exception.getMessage());
            return;
        }

        notifyCustomers(customerDtos, notificationMessage, httpHeaders);

        log.info(LogMessage.SCHEDULED_TASK_ENDED, task);
    }

    private void notifyCustomers(List<CustomerDto> customerDtos, String notificationMessage, HttpHeaders httpHeaders) {
        customerDtos.forEach(customerDto -> {
            NotificationDto request = new NotificationDto(customerDto.getNationalId(), notificationMessage);

            try {
                log.info(LogMessage.BEFORE_REQUEST);

                HttpEntity<NotificationDto> httpEntity = new HttpEntity<>(request, httpHeaders);
                NotificationDto response = restTemplate.postForObject(
                        Entity.NOTIFICATION.getCollectionUrl(),
                        httpEntity,
                        NotificationDto.class
                );

                log.info(LogMessage.REST_TEMPLATE_SUCCESS, response);
            } catch (Exception exception) {
                log.error(LogMessage.EXCEPTION, exception.getMessage());
            }

            log.info(LogMessage.AFTER_REQUEST);
        });
    }

    private List<CustomerDto> getCustomers(UriComponents uriComponents, HttpHeaders httpHeaders) {
        List<CustomerDto> customerDtos;

        String url = uriComponents.toString();
        HttpEntity<Void> httpEntity = new HttpEntity<>(httpHeaders);
        ResponseEntity<List> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                httpEntity,
                List.class,
                uriComponents
        );

        log.info(LogMessage.CLASS_OF_RESPONSE, response.getClass());

        customerDtos = objectMapper.convertValue(response.getBody(), new TypeReference<>() {});
        customerDtos.forEach(customerDto -> log.info(LogMessage.CLASS_OF_OBJECT, "CustomerDto", customerDto.getClass()));

        log.info(LogMessage.REST_TEMPLATE_SUCCESS, customerDtos);

        return customerDtos;
    }
}
