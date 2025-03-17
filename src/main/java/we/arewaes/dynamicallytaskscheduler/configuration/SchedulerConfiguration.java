package we.arewaes.dynamicallytaskscheduler.configuration;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.event.ExecutionChain;
import com.github.kagkarlsson.scheduler.event.ExecutionInterceptor;
import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTaskWithPersistentSchedule;
import com.github.kagkarlsson.scheduler.task.helper.ScheduleAndData;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.CronSchedule;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import we.arewaes.dynamicallytaskscheduler.repository.ScheduledTaskRepository;
import we.arewaes.dynamicallytaskscheduler.service.TaskExecutorService;

import javax.sql.DataSource;
import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;

@Configuration
public class SchedulerConfiguration {
    public static final String DYNAMIC_RECURRING_TASK_NAME = "dynamic-recurring-task";
    public static final TaskDescriptor<ScheduleAndNoData> DYNAMIC_RECURRING_TASK =
            TaskDescriptor.of(DYNAMIC_RECURRING_TASK_NAME, ScheduleAndNoData.class);

    @Bean
    public String instanceId(@Value("${instance.id}") String instanceId) {
        return instanceId;
    }

    @Bean
    public Scheduler scheduler(DataSource dataSource, TaskExecutorService taskExecutorService, ScheduledTaskRepository scheduledTaskRepository) {
        final RecurringTaskWithPersistentSchedule<ScheduleAndNoData> dynamicRecurringTask =
                Tasks.recurringWithPersistentSchedule(DYNAMIC_RECURRING_TASK)
                        .execute((taskInstance, executionContext) -> taskExecutorService.startTaskExecution(taskInstance.getId()));

        Scheduler scheduler = Scheduler.create(dataSource, dynamicRecurringTask)
                .pollingInterval(Duration.ofSeconds(1))
                .addExecutionInterceptor(new TaskExecutionCustomInterceptor(scheduledTaskRepository))
                .registerShutdownHook()
                .build();

        scheduler.start();
        return scheduler;
    }

    @RequiredArgsConstructor
    @Slf4j
    static class TaskExecutionCustomInterceptor implements ExecutionInterceptor {

        private final ScheduledTaskRepository scheduledTaskRepository;

        private static boolean isDynamicRecurringTask(TaskInstance<?> taskInstance) {
            return taskInstance.getTaskName().equals(DYNAMIC_RECURRING_TASK_NAME) &&
                    taskInstance.getData() instanceof ScheduleAndNoData;
        }

        @Override
        public CompletionHandler<?> execute(TaskInstance<?> taskInstance, ExecutionContext executionContext, ExecutionChain executionChain) {
            log.info("Custom interceptor is being executed");
            if (isDynamicRecurringTask(taskInstance)) {
                TaskInstance<ScheduleAndNoData> taskInstanceWithSchedule = (TaskInstance<ScheduleAndNoData>) taskInstance;
                if (isOnHold(taskInstanceWithSchedule)) {
                    return handleOnHold(taskInstanceWithSchedule);
                }

                if (!isOnHold(taskInstanceWithSchedule) && isOnHoldDb(taskInstanceWithSchedule)) {
                    return handleOnHoldNotInSyncWithDb("Task is on hold on the DB. Will be replaced \n", true, taskInstanceWithSchedule);
                }
            }
            log.info("Proceeding with the task execution");
            return executionChain.proceed(taskInstance, executionContext);
        }

        private CompletionHandler.OnCompleteReplace<ScheduleAndNoData> handleOnHoldNotInSyncWithDb(String log, boolean isOnHold, TaskInstance<ScheduleAndNoData> taskInstanceWithSchedule) {
            TaskExecutionCustomInterceptor.log.info(log);
            ScheduleAndNoData newData = new ScheduleAndNoData(taskInstanceWithSchedule.getData().getSchedule(), isOnHold);
            return new CompletionHandler.OnCompleteReplace<>(taskInstanceWithSchedule.getTaskName(), newData);
        }

        private CompletionHandler<?> handleOnHold(TaskInstance<ScheduleAndNoData> taskInstanceWithSchedule) {
            if (isOnHoldDb(taskInstanceWithSchedule)) {
                return rescheduleTaskExecutor(taskInstanceWithSchedule);
            }
            return handleOnHoldNotInSyncWithDb("Task was on hold. Will be replaced \n", false, taskInstanceWithSchedule);
        }

        private CompletionHandler.OnCompleteReschedule<Object> rescheduleTaskExecutor(TaskInstance<ScheduleAndNoData> taskInstanceWithSchedule) {
            log.info("Task is on hold. Will be rescheduled \n");
            return new CompletionHandler.OnCompleteReschedule<>(taskInstanceWithSchedule.getData().getSchedule());
        }

        private boolean isOnHold(TaskInstance<ScheduleAndNoData> taskInstanceWithSchedule) {
            return taskInstanceWithSchedule.getData().isOnHold();
        }

        private boolean isOnHoldDb(TaskInstance<ScheduleAndNoData> taskInstanceWithSchedule) {
            return scheduledTaskRepository.findByIdAndOnHoldTrue(taskInstanceWithSchedule.getId()).isPresent();
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class ScheduleAndNoData implements ScheduleAndData, Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private final CronSchedule schedule;
        private final boolean isOnHold;

        @Override
        public Object getData() {
            return null;
        }
    }

}
