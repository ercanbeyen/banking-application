package com.ercanbeyen.bankingapplication.scheduler;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.dto.CustomerDto;
import com.ercanbeyen.bankingapplication.dto.NotificationDto;
import com.ercanbeyen.bankingapplication.security.service.JwtService;
import com.ercanbeyen.bankingapplication.security.util.JwtUtil;
import com.ercanbeyen.bankingapplication.util.UserCredentialUtil;
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

import java.time.LocalDate;
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
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Scheduled(cron = "0 0 12 * * *") // 12:00 everyday
    public void celebrateCustomersBirthday() {
        final String task = "celebrate customers' birthday";
        log.info(LogMessage.SCHEDULED_TASK_STARTED, task);

        LocalDate birthday = LocalDate.now();
        UriComponents uriComponents = UriComponentsBuilder.fromUriString(Entity.CUSTOMER.getCollectionUrl())
                .queryParam("birthDate", birthday.toString())
                .build();

        String notificationMessage = "happy birthday";

        UserDetails userDetails = userDetailsService.loadUserByUsername(UserCredentialUtil.getSystemAdminUsername());
        Map<String, String> tokens = jwtService.generateTokens(userDetails);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, JwtUtil.generateAuthorizationHeaderValue(tokens.get(JwtUtil.Header.ACCESS_TOKEN_HEADER)));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        List<CustomerDto> customerDtos;

        try {
            customerDtos = getCustomers(uriComponents, headers);
        } catch (Exception exception) {
            log.error(LogMessage.EXCEPTION, exception.getMessage());
            return;
        }

        notifyCustomers(customerDtos, notificationMessage, headers);

        log.info(LogMessage.SCHEDULED_TASK_ENDED, task);
    }

    private void notifyCustomers(List<CustomerDto> customerDtos, String notificationMessage, HttpHeaders headers) {
        customerDtos.forEach(customerDto -> {
            NotificationDto request = new NotificationDto(customerDto.getNationalId(), notificationMessage);

            try {
                log.info(LogMessage.BEFORE_REQUEST);

                HttpEntity<NotificationDto> httpEntity = new HttpEntity<>(request, headers);
                ResponseEntity<NotificationDto> responseEntity = restTemplate.exchange(
                        Entity.NOTIFICATION.getCollectionUrl(),
                        HttpMethod.POST,
                        httpEntity,
                        NotificationDto.class
                );

                log.info(LogMessage.REST_TEMPLATE_SUCCESS, responseEntity.getBody());
            } catch (Exception exception) {
                log.error(LogMessage.EXCEPTION, exception.getMessage());
            }

            log.info(LogMessage.AFTER_REQUEST);
        });
    }

    private List<CustomerDto> getCustomers(UriComponents uriComponents, HttpHeaders headers) {
        List<CustomerDto> customerDtos;
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

        customerDtos = objectMapper.convertValue(response.getBody(), new TypeReference<>() {});
        customerDtos.forEach(customerDto -> log.info(LogMessage.CLASS_OF_OBJECT, "CustomerDto", customerDto.getClass()));

        log.info(LogMessage.REST_TEMPLATE_SUCCESS, customerDtos);

        return customerDtos;
    }
}
