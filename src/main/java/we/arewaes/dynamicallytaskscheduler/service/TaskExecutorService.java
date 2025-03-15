package we.arewaes.dynamicallytaskscheduler.service;

import org.springframework.stereotype.Service;

@Service
public class TaskExecutorService {

    public void startTaskExecution(String taskId) {
        System.out.println("Task " + taskId + " started");
    }
}
