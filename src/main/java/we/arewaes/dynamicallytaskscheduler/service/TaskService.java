package we.arewaes.dynamicallytaskscheduler.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import we.arewaes.dynamicallytaskscheduler.domain.TaskRequest;
import we.arewaes.dynamicallytaskscheduler.entity.ScheduledTask;
import we.arewaes.dynamicallytaskscheduler.repository.ScheduledTaskRepository;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskSchedulerService taskSchedulerService;
    private final ScheduledTaskRepository scheduledTaskRepository;

    private static ScheduledTask generateScheduledTask(String taskId, String cron) {
        ScheduledTask scheduledTask = new ScheduledTask();
        scheduledTask.setId(taskId);
        scheduledTask.setCron(cron);
        scheduledTask.setOnHold(false);
        return scheduledTask;
    }

    public void scheduleTask(TaskRequest taskRequest) {
        ScheduledTask scheduledTask = generateScheduledTask(taskRequest.getTaskId(), taskRequest.getCron());
        scheduledTaskRepository.save(scheduledTask);
        taskSchedulerService.scheduleTaskExecution(taskRequest.getTaskId(), taskRequest.getCron());
    }

    public void cancelTask(String taskId) {
        taskSchedulerService.cancelTaskExecution(taskId);
        scheduledTaskRepository.deleteById(taskId);
    }

    public void setTaskOnHold(String taskId, boolean onHold) {
        ScheduledTask task = fetchScheduledTaskAndUpdateOnHOld(taskId, onHold);
        taskSchedulerService.updateTaskExecution(taskId, task.getCron(), onHold);
    }

    private ScheduledTask fetchScheduledTaskAndUpdateOnHOld(String taskId, boolean onHold) {
        ScheduledTask task = scheduledTaskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
        task.setOnHold(onHold);
        scheduledTaskRepository.save(task);
        return task;
    }

    public void setTasOnHoldOnlyDb(String taskId, boolean onHold) {
        fetchScheduledTaskAndUpdateOnHOld(taskId, onHold);
    }

}
