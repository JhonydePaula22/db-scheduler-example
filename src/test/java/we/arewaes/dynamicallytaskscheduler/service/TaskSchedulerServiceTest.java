package we.arewaes.dynamicallytaskscheduler.service;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.exceptions.TaskInstanceCurrentlyExecutingException;
import com.github.kagkarlsson.scheduler.exceptions.TaskInstanceNotFoundException;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TaskSchedulerServiceTest {

    @Mock
    private Scheduler scheduler;

    @InjectMocks
    private TaskSchedulerService taskSchedulerService;

    @Test
    void scheduleTaskExecution_shouldScheduleTaskSuccessfully() {
        String taskId = "exampleTaskId";
        String trigger = "0 0/5 14 * * *";

        taskSchedulerService.scheduleTaskExecution(taskId, trigger);

        verify(scheduler, times(1)).scheduleIfNotExists(any(SchedulableInstance.class));
    }

    @Test
    void cancelTaskExecution_shouldCancelTaskSuccessfully() {
        String taskId = "exampleTaskId";

        taskSchedulerService.cancelTaskExecution(taskId);

        verify(scheduler, times(1)).cancel(any(TaskInstance.class));
    }

    @Test
    void cancelTaskExecution_shouldHandleTaskNotFoundException() {
        String taskId = "nonExistentTaskId";
        doThrow(new TaskInstanceNotFoundException("Task not found", taskId)).when(scheduler).cancel(any(TaskInstance.class));

        taskSchedulerService.cancelTaskExecution(taskId);

        verify(scheduler, times(1)).cancel(any(TaskInstance.class));
    }

    @Test
    void cancelTaskExecution_shouldHandleTaskInstanceCurrentlyExecutingException() {
        String taskId = "currentlyExecutingTaskId";
        doThrow(new TaskInstanceCurrentlyExecutingException("Task is currently executing", taskId)).when(scheduler).cancel(any(TaskInstance.class));

        taskSchedulerService.cancelTaskExecution(taskId);

        verify(scheduler, times(1)).cancel(any(TaskInstance.class));
    }

    @Test
    void updateTaskExecution_shouldUpdateTaskSuccessfully() {
        String taskId = "exampleTaskId";
        String trigger = "0 0/5 14 * * *";
        boolean onHold = false;

        taskSchedulerService.updateTaskExecution(taskId, trigger, onHold);

        verify(scheduler, times(1)).scheduleIfNotExists(any(SchedulableInstance.class));
        verify(scheduler, times(1)).cancel(any(TaskInstance.class));
    }
}