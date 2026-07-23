package com.ercanbeyen.bankingapplication.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class JobCompletionNotificationListener implements JobExecutionListener {
    @Override
    public void beforeJob(JobExecution jobExecution) {
        checkJobStatus(jobExecution.getStatus(), BatchStatus.STARTED, jobExecution.getJobInstanceId(), jobExecution.getStartTime());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        checkJobStatus(jobExecution.getStatus(), BatchStatus.COMPLETED, jobExecution.getJobInstanceId(), jobExecution.getEndTime());
    }

    private static void checkJobStatus(BatchStatus actualStatus, BatchStatus expectedStatus, Long jobId, LocalDateTime localDateTime) {
        if (actualStatus == expectedStatus) {
            log.info("!!! Job {} {} at {}", jobId, expectedStatus, localDateTime);
        } else {
            log.error("Actual status is different than the expected status. Expected & Actual: {} {}", expectedStatus, actualStatus);
        }
    }
}
