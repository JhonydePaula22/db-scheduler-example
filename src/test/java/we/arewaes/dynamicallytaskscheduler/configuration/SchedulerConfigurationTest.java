package we.arewaes.dynamicallytaskscheduler.configuration;

import com.github.kagkarlsson.scheduler.event.ExecutionChain;
import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.schedule.CronSchedule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import we.arewaes.dynamicallytaskscheduler.entity.ScheduledTask;
import we.arewaes.dynamicallytaskscheduler.repository.ScheduledTaskRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchedulerConfigurationTest {

    @Mock
    private ScheduledTaskRepository scheduledTaskRepository;

    @Test
    void execute_shouldProceedWithTaskExecution_whenTaskIsNotOnHold() {
        TaskInstance<SchedulerConfiguration.ScheduleAndNoData> taskInstance = mock(TaskInstance.class);
        SchedulerConfiguration.ScheduleAndNoData data = new SchedulerConfiguration.ScheduleAndNoData(new CronSchedule("0 0/5 14 * * *"), false);
        when(taskInstance.getData()).thenReturn(data);
        when(taskInstance.getTaskName()).thenReturn(SchedulerConfiguration.DYNAMIC_RECURRING_TASK_NAME);

        SchedulerConfiguration.TaskExecutionCustomInterceptor interceptor = new SchedulerConfiguration.TaskExecutionCustomInterceptor(scheduledTaskRepository);
        ExecutionChain executionChain = mock(ExecutionChain.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);

        interceptor.execute(taskInstance, executionContext, executionChain);

        verify(executionChain, times(1)).proceed(taskInstance, executionContext);
    }

    @Test
    void execute_shouldRescheduleTask_whenTaskIsOnHoldInDb() {
        TaskInstance<SchedulerConfiguration.ScheduleAndNoData> taskInstance = mock(TaskInstance.class);
        SchedulerConfiguration.ScheduleAndNoData data = new SchedulerConfiguration.ScheduleAndNoData(new CronSchedule("0 0/5 14 * * *"), true);
        ScheduledTask scheduledTask = mock(ScheduledTask.class);
        when(taskInstance.getData()).thenReturn(data);
        when(taskInstance.getTaskName()).thenReturn(SchedulerConfiguration.DYNAMIC_RECURRING_TASK_NAME);
        when(scheduledTaskRepository.findByIdAndOnHoldTrue(taskInstance.getId())).thenReturn(Optional.of(scheduledTask));

        SchedulerConfiguration.TaskExecutionCustomInterceptor interceptor = new SchedulerConfiguration.TaskExecutionCustomInterceptor(scheduledTaskRepository);
        ExecutionChain executionChain = mock(ExecutionChain.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);

        CompletionHandler<?> result = interceptor.execute(taskInstance, executionContext, executionChain);

        assertTrue(result instanceof CompletionHandler.OnCompleteReschedule);
    }

    @Test
    void execute_shouldReplaceTask_whenTaskIsOnHoldButNotInDb() {
        TaskInstance<SchedulerConfiguration.ScheduleAndNoData> taskInstance = mock(TaskInstance.class);
        SchedulerConfiguration.ScheduleAndNoData data = new SchedulerConfiguration.ScheduleAndNoData(new CronSchedule("0 0/5 14 * * *"), true);
        when(taskInstance.getData()).thenReturn(data);
        when(taskInstance.getTaskName()).thenReturn(SchedulerConfiguration.DYNAMIC_RECURRING_TASK_NAME);
        when(scheduledTaskRepository.findByIdAndOnHoldTrue(taskInstance.getId())).thenReturn(Optional.empty());

        SchedulerConfiguration.TaskExecutionCustomInterceptor interceptor = new SchedulerConfiguration.TaskExecutionCustomInterceptor(scheduledTaskRepository);
        ExecutionChain executionChain = mock(ExecutionChain.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);

        CompletionHandler<?> result = interceptor.execute(taskInstance, executionContext, executionChain);

        assertTrue(result instanceof CompletionHandler.OnCompleteReplace);
    }

    @Test
    void execute_shouldReplaceTask_whenTaskIsNotOnHoldButInDb() {
        TaskInstance<SchedulerConfiguration.ScheduleAndNoData> taskInstance = mock(TaskInstance.class);
        SchedulerConfiguration.ScheduleAndNoData data = new SchedulerConfiguration.ScheduleAndNoData(new CronSchedule("0 0/5 14 * * *"), false);
        ScheduledTask scheduledTask = mock(ScheduledTask.class);
        when(taskInstance.getData()).thenReturn(data);
        when(taskInstance.getTaskName()).thenReturn(SchedulerConfiguration.DYNAMIC_RECURRING_TASK_NAME);
        when(scheduledTaskRepository.findByIdAndOnHoldTrue(taskInstance.getId())).thenReturn(Optional.of(scheduledTask));

        SchedulerConfiguration.TaskExecutionCustomInterceptor interceptor = new SchedulerConfiguration.TaskExecutionCustomInterceptor(scheduledTaskRepository);
        ExecutionChain executionChain = mock(ExecutionChain.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);

        CompletionHandler<?> result = interceptor.execute(taskInstance, executionContext, executionChain);

        assertTrue(result instanceof CompletionHandler.OnCompleteReplace);
    }
}