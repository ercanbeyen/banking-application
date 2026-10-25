package com.ercanbeyen.bankingapplication.scheduler;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import com.ercanbeyen.bankingapplication.dto.MoneyTransferOrderDto;
import com.ercanbeyen.bankingapplication.dto.request.MoneyTransferRequest;
import com.ercanbeyen.bankingapplication.dto.response.MessageResponse;
import com.ercanbeyen.bankingapplication.security.config.SystemAdminProperties;
import com.ercanbeyen.bankingapplication.security.service.JwtService;
import com.ercanbeyen.bankingapplication.security.util.JwtUtil;
import com.ercanbeyen.bankingapplication.util.MoneyTransferOrderUtil;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
@Async
@Slf4j
@RequiredArgsConstructor
public class MoneyTransferOrderScheduledTask {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SystemAdminProperties systemAdminProperties;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Scheduled(cron = "0 0 10 * * *") // 10:00 everyday
    public void applyMoneyTransferOrders() {
        final String task = "apply transfer orders";
        log.info(LogMessage.SCHEDULED_TASK_STARTED, task);

        UserDetails userDetails = userDetailsService.loadUserByUsername(systemAdminProperties.getUsername());
        Map<String, String> tokens = jwtService.generateTokens(userDetails);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.AUTHORIZATION, JwtUtil.generateAuthorizationHeaderValue(tokens.get(JwtUtil.Header.ACCESS_TOKEN_HEADER)));
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        List<MoneyTransferOrderDto> moneyTransferOrderDtos;

        try {
            moneyTransferOrderDtos = getMoneyTransferOrders(httpHeaders);
        } catch (Exception exception) {
            log.error(LogMessage.EXCEPTION, exception.getMessage());
            return;
        }

        applyMoneyTransferOrders(moneyTransferOrderDtos, httpHeaders);

        log.info(LogMessage.SCHEDULED_TASK_ENDED, task);
    }

    private void applyMoneyTransferOrders(List<MoneyTransferOrderDto> moneyTransferOrderDtos, HttpHeaders httpHeaders) {
        moneyTransferOrderDtos.forEach(transferOrderDto -> {
            if (MoneyTransferOrderUtil.getMoneyTransferOrderDtoPredicate().test(transferOrderDto)) {
                log.info("Time check is passed");
                transferMoneyConsumer(httpHeaders).accept(transferOrderDto);
                log.info("Transfer is successfully completed");
            }
        });
    }

    private Consumer<MoneyTransferOrderDto> transferMoneyConsumer(HttpHeaders httpHeaders) {
        return moneyTransferOrderDto -> {
            MoneyTransferRequest moneyTransferRequest = new MoneyTransferRequest(
                    moneyTransferOrderDto.getSenderAccountId(),
                    moneyTransferOrderDto.getRegularMoneyTransferDto().recipientAccountId(),
                    moneyTransferOrderDto.getRegularMoneyTransferDto().deducteeAccountId(),
                    moneyTransferOrderDto.getRegularMoneyTransferDto().amount(),
                    moneyTransferOrderDto.getRegularMoneyTransferDto().paymentType(),
                    moneyTransferOrderDto.getRegularMoneyTransferDto().explanation()
            );

            try {
                String url = Entity.ACCOUNT.getCollectionUrl() + "/transfer";
                HttpEntity<MoneyTransferRequest> httpEntity = new HttpEntity<>(moneyTransferRequest, httpHeaders);

                ResponseEntity<MessageResponse> response = restTemplate.exchange(
                        url,
                        HttpMethod.PUT,
                        httpEntity,
                        MessageResponse.class
                );

                log.info(LogMessage.REST_TEMPLATE_SUCCESS, response.getBody());
            } catch (Exception exception) {
                log.error(LogMessage.EXCEPTION, exception.getMessage());
            }

            log.info(LogMessage.AFTER_REQUEST);
        };
    }

    private List<MoneyTransferOrderDto> getMoneyTransferOrders(HttpHeaders httpHeaders) {
        String url = Entity.MONEY_TRANSFER_ORDER.getCollectionUrl();
        HttpEntity<Void> httpEntity = new HttpEntity<>(httpHeaders);

        ResponseEntity<List> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                httpEntity,
                List.class
        );

        log.info(LogMessage.CLASS_OF_RESPONSE, response.getClass());

        List<MoneyTransferOrderDto> moneyTransferOrderDtos = objectMapper.convertValue(response.getBody(), new TypeReference<>() {});
        moneyTransferOrderDtos.forEach(transferOrderDto -> log.info(LogMessage.CLASS_OF_OBJECT, "TransferDto", transferOrderDto.getClass()));

        log.info(LogMessage.REST_TEMPLATE_SUCCESS, moneyTransferOrderDtos);

        return moneyTransferOrderDtos;
    }
}
