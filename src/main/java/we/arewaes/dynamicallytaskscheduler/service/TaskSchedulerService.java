package we.arewaes.dynamicallytaskscheduler.service;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.exceptions.TaskInstanceCurrentlyExecutingException;
import com.github.kagkarlsson.scheduler.exceptions.TaskInstanceNotFoundException;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.schedule.CronSchedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import we.arewaes.dynamicallytaskscheduler.configuration.SchedulerConfiguration;

import static we.arewaes.dynamicallytaskscheduler.configuration.SchedulerConfiguration.DYNAMIC_RECURRING_TASK;
import static we.arewaes.dynamicallytaskscheduler.configuration.SchedulerConfiguration.DYNAMIC_RECURRING_TASK_NAME;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaskSchedulerService {
    private final Scheduler scheduler;

    public void scheduleTaskExecution(String taskId, String trigger) {
        log.info("Schedule task with taskId: {} \n", taskId);
        this.scheduler.scheduleIfNotExists(DYNAMIC_RECURRING_TASK
                .instance(taskId)
                .data(new SchedulerConfiguration.ScheduleAndNoData(new CronSchedule(trigger), false))
                .scheduledAccordingToData());
    }

    public void cancelTaskExecution(String taskId) {
        log.info("Cancel task with taskId: {} \n", taskId);
        try {
            this.scheduler.cancel(new TaskInstance<>(DYNAMIC_RECURRING_TASK_NAME, taskId));
        } catch (TaskInstanceNotFoundException e) {
            log.warn("Task not found with taskId: {}", taskId);
        } catch (TaskInstanceCurrentlyExecutingException e) {
            log.warn("Task with taskId {} is currently executing. Will be cancelled after execution.", taskId);
        }
        // in both exceptions, you can choose how you handle according to the project needs.
        // let's assume that after this succeeds, the task data is deleted also on the repository
    }

    public void updateTaskExecution(String taskId, String trigger, boolean onHold) {
        log.info("Update task with taskId: {} \n", taskId);
        this.cancelTaskExecution(taskId);
        this.scheduler.scheduleIfNotExists(DYNAMIC_RECURRING_TASK
                .instance(taskId)
                .data(new SchedulerConfiguration.ScheduleAndNoData(new CronSchedule(trigger), onHold))
                .scheduledAccordingToData());
    }
}
