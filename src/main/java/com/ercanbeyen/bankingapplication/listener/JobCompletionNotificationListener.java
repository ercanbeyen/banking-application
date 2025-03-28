package com.ercanbeyen.bankingapplication.listener;

import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JobCompletionNotificationListener extends JobExecutionListenerSupport {

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info(LogMessage.Batch.JOB_STATUS, "FINISHED");
        }
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.STARTING) {
            log.info(LogMessage.Batch.JOB_STATUS, "STARTED");
        }
    }
}
