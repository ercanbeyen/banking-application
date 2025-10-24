package com.ercanbeyen.bankingapplication.listener;

import com.ercanbeyen.bankingapplication.constant.message.LogMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StepCompletionNotificationListener implements StepExecutionListener {
    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info(LogMessage.Batch.STEP_STATUS, stepExecution.getStepName(), stepExecution.getExitStatus().getExitCode(), stepExecution.getStartTime());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        ExitStatus exitStatus = stepExecution.getExitStatus();
        log.info(LogMessage.Batch.STEP_STATUS, stepExecution.getStepName(), exitStatus.getExitCode(), stepExecution.getEndTime());
        return exitStatus == ExitStatus.COMPLETED ? ExitStatus.COMPLETED : ExitStatus.FAILED;
    }
}
