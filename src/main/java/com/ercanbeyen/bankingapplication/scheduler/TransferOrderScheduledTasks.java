package com.ercanbeyen.bankingapplication.scheduler;

import com.ercanbeyen.bankingapplication.constant.enums.Entity;
import com.ercanbeyen.bankingapplication.constant.message.LogMessages;
import com.ercanbeyen.bankingapplication.dto.RegularTransferDto;
import com.ercanbeyen.bankingapplication.dto.TransferOrderDto;
import com.ercanbeyen.bankingapplication.dto.request.MoneyTransferRequest;
import com.ercanbeyen.bankingapplication.util.TransferOrderUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.function.Consumer;

@Component
@Async
@Slf4j
@RequiredArgsConstructor
public class TransferOrderScheduledTasks {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "0 0 10 * * *") // 10:00 everyday
    public void applyTransferOrders() {
        final String task = "apply transfer orders";
        log.info(LogMessages.SCHEDULED_TASK_STARTED, task);

        List<TransferOrderDto> transferOrderDtos;

        try {
            String url = Entity.TRANSFER_ORDER.getCollectionUrl();
            log.info("Getting transfer url: {}", url);

            List<?> response = restTemplate.getForObject(url, List.class);
            assert response != null;
            log.info(LogMessages.CLASS_OF_RESPONSE, response.getClass());

            transferOrderDtos = objectMapper.convertValue(response, new TypeReference<>() {});
            transferOrderDtos.forEach(transferOrderDto -> log.info(LogMessages.CLASS_OF_OBJECT, "TransferDto", transferOrderDto.getClass()));

            log.info(LogMessages.REST_TEMPLATE_SUCCESS, transferOrderDtos);
        } catch (Exception exception) {
             log.error(LogMessages.EXCEPTION, exception.getMessage());
             return;
         }

        transferOrderDtos.forEach(transferOrderDto -> {
            if (TransferOrderUtils.getTransferOrderDtoPredicate().test(transferOrderDto)) {
                log.info("Time check is passed");
                getTransferOrderDtoConsumer().accept(transferOrderDto);
                log.info("Transfer is successfully completed");
            }
        });

        log.info(LogMessages.SCHEDULED_TASK_ENDED, task);
    }

    private Consumer<TransferOrderDto> getTransferOrderDtoConsumer() {
        return transferOrderDto -> {
            Integer senderAccountId = transferOrderDto.getSenderAccountId();
            RegularTransferDto regularTransferDto = transferOrderDto.getRegularTransferDto();
            Integer receiverAccountId = regularTransferDto.receiverAccountId();
            MoneyTransferRequest moneyTransferRequest = new MoneyTransferRequest(
                    senderAccountId,
                    receiverAccountId,
                    regularTransferDto.amount(),
                    regularTransferDto.paymentType(),
                    regularTransferDto.explanation()
            );
            try {
                String url = Entity.ACCOUNT.getCollectionUrl() + "/transfer";
                log.info("Transfer url: {}", url);
                restTemplate.put(url, moneyTransferRequest);

                String logMessage = "Transfer from " + senderAccountId + " to " + receiverAccountId + " is successfully completed";
                log.info(LogMessages.TRANSACTION_MESSAGE, logMessage);
            } catch (Exception exception) {
                log.error(LogMessages.EXCEPTION, exception.getMessage());
            }

            log.info(LogMessages.AFTER_REQUEST);
        };
    }
}
