package we.arewaes.dynamicallytaskscheduler.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TaskExecutorService {

    private static final String TASK_EXECUTION_STARTED_MESSAGE = "INSTANCE ID: %s -> Task %s started at %s \n";
    private final String instanceId;

    public void startTaskExecution(String taskId) {
        System.out.printf(TASK_EXECUTION_STARTED_MESSAGE, instanceId, taskId, LocalDateTime.now());
    }
}
